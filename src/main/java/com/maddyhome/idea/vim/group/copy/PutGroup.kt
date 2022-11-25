/*
 * Copyright 2003-2022 The IdeaVim authors
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
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.ide.CopyPasteManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.isBlock
import com.maddyhome.idea.vim.command.isChar
import com.maddyhome.idea.vim.command.isLine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.TestClipboardModel
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.helpers.ClipboardOptionHelper
import com.maddyhome.idea.vim.put.ProcessedTextData
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.put.VimPasteProvider
import com.maddyhome.idea.vim.put.VimPutBase
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.awt.datatransfer.DataFlavor

class PutGroup : VimPutBase() {

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

  override fun putTextViaIde(
    pasteProvider: VimPasteProvider,
    vimEditor: VimEditor,
    vimContext: ExecutionContext,
    text: ProcessedTextData,
    subMode: VimStateMachine.SubMode,
    data: PutData,
    additionalData: Map<String, Any>,
  ) {
    val editor = (vimEditor as IjVimEditor).editor
    val context = vimContext.context as DataContext
    val carets: MutableMap<Caret, RangeMarker> = mutableMapOf()
    EditorHelper.getOrderedCaretsList(editor).forEach { caret ->
      val startOffset =
        prepareDocumentAndGetStartOffsets(
          vimEditor,
          IjVimCaret(caret),
          text.typeInRegister,
          data,
          additionalData
        ).first()
      val pointMarker = editor.document.createRangeMarker(startOffset, startOffset)
      caret.moveToInlayAwareOffset(startOffset)
      carets[caret] = pointMarker
    }

    val allContentsBefore = CopyPasteManager.getInstance().allContents
    val sizeBeforeInsert = allContentsBefore.size
    val firstItemBefore = allContentsBefore.firstOrNull()
    val origTestContents = TestClipboardModel.contents
    logger.debug { "Transferable classes: ${text.transferableData.joinToString { it.javaClass.name }}" }
    val origContent: TextBlockTransferable = injector.clipboardManager.setClipboardText(
      text.text,
      transferableData = text.transferableData
    ) as TextBlockTransferable
    val allContentsAfter = CopyPasteManager.getInstance().allContents
    val sizeAfterInsert = allContentsAfter.size
    try {
      (pasteProvider as IjPasteProvider).pasteProvider.performPaste(context)
    } finally {
      val textOnTop =
        ((firstItemBefore as? TextBlockTransferable)?.getTransferData(DataFlavor.stringFlavor) as? String) != text.text
      TestClipboardModel.contents = origTestContents
      if (sizeBeforeInsert != sizeAfterInsert || textOnTop) {
        // Sometimes inserted text replaces existing one. E.g. on insert with + or * register
        (CopyPasteManager.getInstance() as? CopyPasteManagerEx)?.run { removeContent(origContent) }
      }
    }

    carets.forEach { (caret, point) ->
      val startOffset = point.startOffset
      point.dispose()
      if (!caret.isValid) return@forEach
      val endOffset = if (data.indent) doIndent(
        vimEditor,
        IjVimCaret(caret),
        vimContext,
        startOffset,
        startOffset + text.text.length
      ) else startOffset + text.text.length
      VimPlugin.getMark().setChangeMarks(editor.vim, TextRange(startOffset, endOffset))
      VimPlugin.getMark().setMark(editor.vim, MARK_CHANGE_POS, startOffset)
      moveCaretToEndPosition(
        vimEditor,
        IjVimCaret(caret),
        startOffset,
        endOffset,
        text.typeInRegister,
        subMode,
        data.caretAfterInsertedText
      )
    }
  }

  override fun doIndent(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    startOffset: Int,
    endOffset: Int,
  ): Int {
    val startLine = editor.offsetToBufferPosition(startOffset).line
    val endLine = editor.offsetToBufferPosition(endOffset - 1).line
    val startLineOffset = (editor as IjVimEditor).editor.document.getLineStartOffset(startLine)
    val endLineOffset = editor.editor.document.getLineEndOffset(endLine)

    VimPlugin.getChange().autoIndentRange(
      editor,
      caret,
      context,
      TextRange(startLineOffset, endLineOffset)
    )
    return editor.getLineEndOffset(endLine, true)
  }

  override fun notifyAboutIdeaPut(editor: VimEditor?) {
    val project = editor?.ij?.project
    if (VimPlugin.getVimState().isIdeaPutNotified ||
      OptionConstants.clipboard_ideaput in (
        VimPlugin.getOptionService()
          .getOptionValue(OptionScope.GLOBAL, OptionConstants.clipboardName) as VimString
        ).value ||
      ClipboardOptionHelper.ideaputDisabled
    ) return

    VimPlugin.getVimState().isIdeaPutNotified = true

    VimPlugin.getNotifications(project).notifyAboutIdeaPut()
  }
}

class IjPasteProvider(val pasteProvider: PasteProvider) : VimPasteProvider
