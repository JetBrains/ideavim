/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group.copy

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.ide.DataManager
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.ide.CopyPasteManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.isBlock
import com.maddyhome.idea.vim.command.isChar
import com.maddyhome.idea.vim.command.isLine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.TestClipboardModel
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.subMode
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
import com.maddyhome.idea.vim.put.VimPutBase
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.awt.datatransfer.DataFlavor
import kotlin.math.min

class PutGroup : VimPutBase() {
  override fun putTextForCaret(editor: VimEditor, caret: VimCaret, context: ExecutionContext, data: PutData, updateVisualMarks: Boolean): Boolean {
    val additionalData = collectPreModificationData(editor, data)
    data.visualSelection?.let {
      deleteSelectedText(
        editor,
        data,
        OperatorArguments(false, 0, editor.mode, editor.subMode)
      )
    }
    val processedText = processText(editor, data) ?: return false
    putForCaret(editor, caret, data, additionalData, context, processedText)
    if (editor.primaryCaret() == caret && updateVisualMarks) {
      wrapInsertedTextWithVisualMarks(editor, data, processedText)
    }
    return true
  }

  override fun putTextAndSetCaretPosition(
    editor: VimEditor,
    context: ExecutionContext,
    text: ProcessedTextData,
    data: PutData,
    additionalData: Map<String, Any>,
  ) {
    val visualSelection = data.visualSelection
    val subMode = visualSelection?.typeInEditor?.toSubMode() ?: VimStateMachine.SubMode.NONE
    if (OptionConstants.clipboard_ideaput in (
      injector.optionService
        .getOptionValue(OptionScope.GLOBAL, OptionConstants.clipboardName) as VimString
      ).value
    ) {
      val idePasteProvider = getProviderForPasteViaIde(editor, text.typeInRegister, data)
      if (idePasteProvider != null) {
        logger.debug("Perform put via idea paste")
        putTextViaIde(idePasteProvider, editor, context, text, subMode, data, additionalData)
        return
      }
    }

    logger.debug("Perform put via plugin")
    val myCarets = if (visualSelection != null) {
      visualSelection.caretsAndSelections.keys.sortedByDescending { it.getLogicalPosition() }
    } else {
      EditorHelper.getOrderedCaretsList(editor.ij).map { IjVimCaret(it) }
    }
    injector.application.runWriteAction {
      myCarets.forEach { caret -> putForCaret(editor, caret, data, additionalData, context, text) }
    }
  }

  private fun putForCaret(
    editor: VimEditor,
    caret: VimCaret,
    data: PutData,
    additionalData: Map<String, Any>,
    context: ExecutionContext,
    text: ProcessedTextData,
  ) {
    notifyAboutIdeaPut(editor)
    if (data.visualSelection?.typeInEditor?.isLine == true && editor.isOneLineMode()) return
    val startOffsets = prepareDocumentAndGetStartOffsets(editor, caret, text.typeInRegister, data, additionalData)

    startOffsets.forEach { startOffset ->
      val subMode = data.visualSelection?.typeInEditor?.toSubMode() ?: VimStateMachine.SubMode.NONE
      val endOffset = putTextInternal(
        editor, caret, context, text.text, text.typeInRegister, subMode,
        startOffset, data.count, data.indent, data.caretAfterInsertedText
      )
      if (caret == editor.primaryCaret()) {
        VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, endOffset))
      }
      moveCaretToEndPosition(
        editor,
        caret,
        startOffset,
        endOffset,
        text.typeInRegister,
        subMode,
        data.caretAfterInsertedText
      )
    }
  }

  private fun prepareDocumentAndGetStartOffsets(
    vimEditor: VimEditor,
    vimCaret: VimCaret,
    typeInRegister: SelectionType,
    data: PutData,
    additionalData: Map<String, Any>,
  ): List<Int> {
    val editor = (vimEditor as IjVimEditor).editor
    val caret = (vimCaret as IjVimCaret).caret
    val application = injector.application
    val visualSelection = data.visualSelection
    if (visualSelection != null) {
      return when {
        visualSelection.typeInEditor.isChar && typeInRegister.isLine -> {
          application.runWriteAction { editor.document.insertString(caret.offset, "\n") }
          listOf(caret.offset + 1)
        }
        visualSelection.typeInEditor.isBlock -> {
          val firstSelectedLine = additionalData["firstSelectedLine"] as Int
          val selectedLines = additionalData["selectedLines"] as Int
          val startColumnOfSelection = additionalData["startColumnOfSelection"] as Int
          val line = if (data.insertTextBeforeCaret) firstSelectedLine else firstSelectedLine + selectedLines
          when (typeInRegister) {
            SelectionType.LINE_WISE -> when {
              data.insertTextBeforeCaret -> listOf(EditorHelper.getLineStartOffset(editor, line))
              else -> {
                val pos = EditorHelper.getLineEndOffset(editor, line, true)
                application.runWriteAction { editor.document.insertString(pos, "\n") }
                listOf(pos + 1)
              }
            }
            SelectionType.CHARACTER_WISE -> (firstSelectedLine + selectedLines downTo firstSelectedLine)
              .map { editor.logicalPositionToOffset(LogicalPosition(it, startColumnOfSelection)) }
            SelectionType.BLOCK_WISE -> listOf(
              editor.logicalPositionToOffset(
                LogicalPosition(
                  firstSelectedLine,
                  startColumnOfSelection
                )
              )
            )
          }
        }
        visualSelection.typeInEditor.isLine -> {
          val lastChar = if (editor.fileSize > 0) {
            editor.document.getText(com.intellij.openapi.util.TextRange(editor.fileSize - 1, editor.fileSize))[0]
          } else {
            null
          }
          if (caret.offset == editor.fileSize && editor.fileSize != 0 && lastChar != '\n') {
            application.runWriteAction { editor.document.insertString(caret.offset, "\n") }
            listOf(caret.offset + 1)
          } else listOf(caret.offset)
        }
        else -> listOf(caret.offset)
      }
    } else {
      if (data.insertTextBeforeCaret) {
        return when (typeInRegister) {
          SelectionType.LINE_WISE -> listOf(VimPlugin.getMotion().moveCaretToLineStart(editor.vim, caret.vim))
          else -> listOf(caret.offset)
        }
      }

      var startOffset: Int
      val line = if (data.putToLine < 0) caret.logicalPosition.line else data.putToLine
      when (typeInRegister) {
        SelectionType.LINE_WISE -> {
          startOffset =
            min(editor.document.textLength, VimPlugin.getMotion().moveCaretToLineEnd(editor.vim, line, true) + 1)
          if (startOffset > 0 && startOffset == editor.document.textLength && editor.document.charsSequence[startOffset - 1] != '\n') {
            application.runWriteAction { editor.document.insertString(startOffset, "\n") }
            startOffset++
          }
        }
        else -> {
          startOffset = caret.offset
          if (!EditorHelper.isLineEmpty(editor, line, false)) {
            startOffset++
          }
        }
      }

      return if (startOffset > editor.document.textLength) listOf(editor.document.textLength) else listOf(startOffset)
    }
  }

  private fun getProviderForPasteViaIde(
    editor: VimEditor,
    typeInRegister: SelectionType,
    data: PutData,
  ): PasteProvider? {
    val visualSelection = data.visualSelection
    if (visualSelection != null && visualSelection.typeInEditor.isBlock) return null
    if ((typeInRegister.isLine || typeInRegister.isChar) && data.count == 1) {
      val context = DataManager.getInstance().getDataContext(editor.ij.contentComponent)
      val provider = PlatformDataKeys.PASTE_PROVIDER.getData(context)
      if (provider != null && provider.isPasteEnabled(context)) return provider
    }
    return null
  }

  private fun putTextViaIde(
    pasteProvider: PasteProvider,
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
    val origContent: TextBlockTransferable = injector.clipboardManager.setClipboardText(
      text.text,
      transferableData = text.transferableData
    ) as TextBlockTransferable
    val allContentsAfter = CopyPasteManager.getInstance().allContents
    val sizeAfterInsert = allContentsAfter.size
    try {
      pasteProvider.performPaste(context)
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
    val startLine = editor.offsetToLogicalPosition(startOffset).line
    val endLine = editor.offsetToLogicalPosition(endOffset - 1).line
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

  companion object {
    private val logger = Logger.getInstance(PutGroup::class.java.name)
  }
}
