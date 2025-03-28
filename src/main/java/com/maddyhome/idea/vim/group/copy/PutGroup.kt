/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.copy

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.ide.DataManager
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.util.PlatformUtils
import com.maddyhome.idea.vim.VimPlugin
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
import com.maddyhome.idea.vim.helper.RWLockLabel
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

@Service
internal class PutGroup : VimPutBase() {

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

  @RWLockLabel.SelfSynchronized
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
   * Unfortunately, at the moment, this functionality of "additional text processing" is bound to
   *   paste operation. So here we do the trick, in order to insert text from the register with all the
   *   brains from IJ, we put this text into the clipboard and perform a regular IJ paste.
   * In order to do this properly, after the paste, we should remove the clipboard text from
   *   the kill ring (stack of clipboard items)
   * So, generally this function should look like this:
   * ```
   * setClipboardText(text)
   * try {
   *   performPaste()
   * } finally {
   *   removeTextFromClipboard()
   * }
   * ```
   * And it was like this till some moment. However, if our text to paste matches the text that is already placed
   *   in the clipboard, instead of putting new text on top of stack, it merges the text into the last stack item.
   * So, all the other code in this function is created to detect such case and do not remove last clipboard item.
   */
  private fun pasteKeepingClipboard(text: ProcessedTextData, doPaste: () -> Unit) {
    val allContentsBefore = CopyPasteManager.getInstance().allContents
    val sizeBeforeInsert = allContentsBefore.size
    val firstItemBefore = allContentsBefore.firstOrNull()
    logger.debug { "Copied text: ${text.copiedText}" }
    val (textContent, transferableData) = text.copiedText as IjVimCopiedText
    val origContent: TextBlockTransferable =
      injector.clipboardManager.setClipboardText(textContent, textContent, transferableData) as TextBlockTransferable
    val allContentsAfter = CopyPasteManager.getInstance().allContents
    val sizeAfterInsert = allContentsAfter.size
    try {
      doPaste()
    } finally {
      val textInClipboard = (firstItemBefore as? TextBlockTransferable)
        ?.getTransferData(DataFlavor.stringFlavor) as? String
      val textOnTop = textInClipboard != null && textInClipboard != text.copiedText.text
      if (sizeBeforeInsert != sizeAfterInsert || textOnTop) {
        // Sometimes an inserted text replaces an existing one. E.g. on insert with + or * register
        (CopyPasteManager.getInstance() as? CopyPasteManagerEx)?.run { removeContent(origContent) }
      }
    }
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

    VimPlugin.getChange().autoIndentRange(
      editor,
      caret,
      context,
      TextRange(startLineOffset, endLineOffset),
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

    VimPlugin.getNotifications(project).notifyAboutIdeaPut()
  }
}

internal class IjPasteProvider(val pasteProvider: PasteProvider) : VimPasteProvider
