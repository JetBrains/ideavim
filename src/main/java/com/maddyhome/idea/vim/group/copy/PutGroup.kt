/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.copy

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.ide.DataManager
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.util.PlatformUtils
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.VimPlugin.getNotifications
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.setChangeMarks
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.helper.VimLockLabel
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.ide.isClionNova
import com.maddyhome.idea.vim.ide.isRider
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimCopiedText
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.helpers.ClipboardOptionHelper
import com.maddyhome.idea.vim.put.ProcessedTextData
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.put.VimPasteProvider
import com.maddyhome.idea.vim.put.VimPutBase
import com.maddyhome.idea.vim.register.RegisterConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.isBlock
import com.maddyhome.idea.vim.state.mode.isChar
import com.maddyhome.idea.vim.state.mode.isLine
import com.maddyhome.idea.vim.undo.VimKeyBasedUndoService
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

internal class PutGroup : VimPutBase() {

  @VimLockLabel.SelfSynchronized
  override fun putText(
    editor: VimEditor,
    context: ExecutionContext,
    data: PutData,
    updateVisualMarks: Boolean,
    modifyRegister: Boolean,
  ): Boolean {
    val pasted = super.putText(editor, context, data, updateVisualMarks, modifyRegister)
    if (pasted) suggestControlCharsEditorIfNeeded(editor, data)
    return pasted
  }

  /** After a paste, suggest the control-chars editor if the pasted text contains control characters. */
  private fun suggestControlCharsEditorIfNeeded(editor: VimEditor, data: PutData) {
    if (editor.getVirtualBufferKind() != null) return // the user is already editing in a virtual buffer
    val text = data.textData?.rawText ?: return
    if (!EngineStringHelper.containsControlCharacters(text)) return
    val project = editor.ij.project ?: return
    getNotifications(project).notifyControlCharactersPasted()
  }

  override fun getProviderForPasteViaIde(
    editor: VimEditor,
    typeInRegister: SelectionType,
    data: PutData,
  ): VimPasteProvider? {
    val visualSelection = data.visualSelection
    if (visualSelection != null && visualSelection.typeInEditor.isBlock) return null
    if ((typeInRegister.isLine || typeInRegister.isChar) && data.count == 1) {
      val context = DataManager.getInstance().getDataContext(editor.ij.contentComponent)
      val provider = PlatformDataKeys.PASTE_PROVIDER.getData(context)
      if (provider != null && provider.isPasteEnabled(context)) return IjPasteProvider(provider)
    }
    return null
  }

  @VimLockLabel.SelfSynchronized
  override fun putTextViaIde(
    pasteProvider: VimPasteProvider,
    vimEditor: VimEditor,
    vimContext: ExecutionContext,
    text: ProcessedTextData,
    selectionType: SelectionType,
    data: PutData,
    additionalData: Map<String, Any>,
  ) {
    val editor = (vimEditor as IjVimEditor).editor
    val context = vimContext.context as DataContext
    val carets: MutableMap<Caret, RangeMarker> = mutableMapOf()
    if (injector.vimState.mode is Mode.INSERT) {
      val nanoTime = System.nanoTime()

      val undo = injector.undo
      when (undo) {
        is VimKeyBasedUndoService -> undo.setInsertNonMergeUndoKey()
        is VimTimestampBasedUndoService -> {
          vimEditor.forEachCaret { undo.startInsertSequence(it, it.offset, nanoTime) }
        }
      }
    }
    EditorHelper.getOrderedCaretsList(editor).forEach { caret ->
      val startOffset =
        prepareDocumentAndGetStartOffsets(
          vimEditor,
          IjVimCaret(caret),
          text.typeInRegister,
          data,
          additionalData,
        ).first()
      val pointMarker = editor.document.createRangeMarker(startOffset, startOffset)
      caret.moveToInlayAwareOffset(startOffset)
      carets[caret] = pointMarker
    }

    val registerChar = text.registerChar
    if (registerChar != null && registerChar == RegisterConstants.CLIPBOARD_REGISTER) {
      // On X11 the IDE clipboard can miss the text of another application even though the register was read (see
      // IjClipboardManager.getContents), and the IDE paste would then insert nothing. The register holds the clipboard
      // text, so it is put into the clipboard and kept there.
      if (CopyPasteManager.getInstance().contents?.getText() == null) {
        putIntoClipboard(text.copiedText as IjVimCopiedText)
      }
      (pasteProvider as IjPasteProvider).pasteProvider.performPaste(context)
    } else {
      pasteKeepingClipboard(text) {
        (pasteProvider as IjPasteProvider).pasteProvider.performPaste(context)
      }
    }

    val lastPastedRegion = if (carets.size == 1) editor.getUserData(EditorEx.LAST_PASTED_REGION) else null
    carets.forEach { (caret, point) ->
      val startOffset = point.startOffset
      point.dispose()
      if (!caret.isValid) return@forEach

      val caretPossibleEndOffset = lastPastedRegion?.endOffset ?: (startOffset + text.copiedText.text.length)
      val endOffset = if (data.indent) {
        doIndent(
          vimEditor,
          IjVimCaret(caret),
          vimContext,
          startOffset,
          caretPossibleEndOffset,
        )
      } else {
        caretPossibleEndOffset
      }
      val vimCaret = caret.vim
      injector.markService.setChangeMarks(vimCaret, TextRange(startOffset, endOffset))
      injector.markService.setMark(vimCaret, MARK_CHANGE_POS, startOffset)
      moveCaretToEndPosition(
        vimEditor,
        IjVimCaret(caret),
        startOffset,
        endOffset,
        text.typeInRegister,
        selectionType,
        data.caretAfterInsertedText,
      )
    }
  }

  /**
   * ideaput - option that enables "smartness" of the insert operation. For example, it automatically
   *   inserts import statements, or converts Java code to kotlin.
   * This "additional text processing" is bound to the IDE paste, and the IDE paste reads the clipboard. So the
   *   register text is put into the clipboard, the IDE paste is performed, and the clipboard is put back as it was:
   * - Usually the register text becomes a new item on top of the clipboard history. Removing it brings the previous
   *   content back to the top.
   * - Sometimes the register text replaces the previous item instead, e.g. on a put from the + or * register. The
   *   register text is removed then as well.
   * - If the clipboard already holds the same text, the register text is merged into that item. The clipboard didn't
   *   change, so nothing is removed: removing would lose the item of the user.
   * - A clipboard without history, like the one of the frontend in split mode, can't remove anything. It still holds
   *   the register text after the removal, so the previous content is set back.
   */
  private fun pasteKeepingClipboard(text: ProcessedTextData, doPaste: () -> Unit) {
    val clipboard = CopyPasteManager.getInstance()
    val previousContent = clipboard.allContents.firstOrNull()
    val historySizeBefore = clipboard.allContents.size

    val registerText = text.copiedText.text
    val registerContent = putIntoClipboard(text.copiedText as IjVimCopiedText)

    val registerAddedToHistory = clipboard.allContents.size != historySizeBefore
    val registerReplacedOtherText = previousContent?.getText().let { it != null && it != registerText }
    try {
      doPaste()
    } finally {
      if (registerAddedToHistory || registerReplacedOtherText) {
        (clipboard as? CopyPasteManagerEx)?.removeContent(registerContent)
      }
      if (previousContent != null && registerReplacedOtherText && clipboard.holdsText(registerText)) {
        clipboard.setContents(previousContent)
      }
    }
  }

  private fun putIntoClipboard(copiedText: IjVimCopiedText): TextBlockTransferable {
    logger.debug { "Copied text: $copiedText" }
    val (text, transferableData) = copiedText
    return injector.clipboardManager.setClipboardText(text, text, transferableData) as TextBlockTransferable
  }

  private fun CopyPasteManager.holdsText(text: String): Boolean = contents?.getText() == text

  private fun Transferable.getText(): String? {
    if (!isDataFlavorSupported(DataFlavor.stringFlavor)) return null
    // The content of another application may be gone by now
    return runCatching { getTransferData(DataFlavor.stringFlavor) as? String }.getOrNull()
  }

  override fun doIndent(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    startOffset: Int,
    endOffset: Int,
  ): Int {
    // Temp fix for VIM-2808 for Rider and Clion. Should be removed after rider will fix it's issues
    // Disable for client due to VIM-3857
    if (isRider() || isClionNova() || PlatformUtils.isJetBrainsClient()) return endOffset

    val startLine = editor.offsetToBufferPosition(startOffset).line
    val endLine = editor.offsetToBufferPosition(endOffset - 1).line
    val startLineOffset = (editor as IjVimEditor).editor.document.getLineStartOffset(startLine)
    val endLineOffset = editor.editor.document.getLineEndOffset(endLine)

    if (CodeInsightSettings.getInstance().REFORMAT_ON_PASTE == CodeInsightSettings.NO_REFORMAT) return editor.getLineEndOffset(
      endLine,
      true
    )

    VimPlugin.getChange().autoIndentRange(
      editor,
      context,
      ranges = listOf(TextRange(startLineOffset, endLineOffset)),
    )
    return editor.getLineEndOffset(endLine, true)
  }

  override fun notifyAboutIdeaPut(editor: VimEditor?) {
    val project = editor?.ij?.project
    if (VimPlugin.getVimState().isIdeaPutNotified || ClipboardOptionHelper.ideaputDisabled ||
      injector.globalOptions().clipboard.contains(OptionConstants.clipboard_ideaput)
    ) {
      return
    }

    VimPlugin.getVimState().isIdeaPutNotified = true

    getNotifications(project).notifyAboutIdeaPut()
  }
}

internal class IjPasteProvider(val pasteProvider: PasteProvider) : VimPasteProvider
