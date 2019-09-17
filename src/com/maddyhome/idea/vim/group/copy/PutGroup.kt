/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.editor.*
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MarkGroup
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import com.maddyhome.idea.vim.option.OptionsManager
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * [putToLine] has affect only of [insertTextBeforeCaret] is false and [visualSelection] is null
 */
data class PutData(
  val textData: TextData?,
  val visualSelection: VisualSelection?,
  val count: Int,
  val insertTextBeforeCaret: Boolean,
  private val _indent: Boolean,
  val caretAfterInsertedText: Boolean,
  val putToLine: Int = -1
) {
  val indent: Boolean =
    if (_indent && textData?.typeInRegister != SelectionType.LINE_WISE && visualSelection?.typeInEditor != SelectionType.LINE_WISE) false else _indent

  data class VisualSelection(
    val caretsAndSelections: Map<Caret, VimSelection>,
    val typeInEditor: SelectionType
  )

  data class TextData(
    val rawText: String?,
    val typeInRegister: SelectionType,
    val transferableData: List<TextBlockTransferableData>
  )
}

private data class ProcessedTextData(
  val text: String,
  val typeInRegister: SelectionType,
  val transferableData: List<TextBlockTransferableData>
)

class PutGroup {
  fun putText(editor: Editor, context: DataContext, data: PutData): Boolean {
    val additionalData = collectPreModificationData(editor, data)
    deleteSelectedText(editor, data)
    val processedText = processText(editor, data) ?: return false
    putTextAndSetCaretPosition(editor, context, processedText, data, additionalData)
    return true
  }

  fun putTextForCaret(editor: Editor, caret: Caret, context: DataContext, data: PutData): Boolean {
    val additionalData = collectPreModificationData(editor, data)
    val processedText = processText(editor, data) ?: return false
    putForCaret(editor, caret, data, additionalData, context, processedText)
    return true
  }

  private fun collectPreModificationData(editor: Editor, data: PutData): Map<String, Any> {
    return if (data.visualSelection != null && data.visualSelection.typeInEditor == SelectionType.BLOCK_WISE) {
      val vimSelection = data.visualSelection.caretsAndSelections.getValue(editor.caretModel.primaryCaret)
      val selStart = editor.offsetToLogicalPosition(vimSelection.vimStart)
      val selEnd = editor.offsetToLogicalPosition(vimSelection.vimEnd)
      mapOf(
        "startColumnOfSelection" to min(selStart.column, selEnd.column),
        "selectedLines" to abs(selStart.line - selEnd.line),
        "firstSelectedLine" to min(selStart.line, selEnd.line)
      )
    } else mutableMapOf()
  }

  private fun deleteSelectedText(editor: Editor, data: PutData) {
    if (data.visualSelection == null) return

    data.visualSelection.caretsAndSelections.entries.sortedByDescending { it.key.logicalPosition }.forEach { (caret, selection) ->
      if (!caret.isValid) return@forEach
      val range = selection.toVimTextRange(false).normalize()

      ApplicationManager.getApplication().runWriteAction {
        VimPlugin.getChange().deleteRange(editor, caret, range, selection.type, false)
      }
      caret.moveToOffset(range.startOffset)
    }
  }

  private fun processText(editor: Editor, data: PutData): ProcessedTextData? {
    var text = data.textData?.rawText ?: run {
      if (data.visualSelection != null) {
        val offset = editor.caretModel.primaryCaret.offset
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, offset)
        VimPlugin.getMark().setChangeMarks(editor, TextRange(offset, offset))
      }
      return null
    }

    if (data.visualSelection?.typeInEditor == SelectionType.LINE_WISE && data.textData.typeInRegister == SelectionType.CHARACTER_WISE) text += "\n"

    if (data.textData.typeInRegister == SelectionType.LINE_WISE && text.isNotEmpty() && text.last() != '\n') text += '\n'

    return ProcessedTextData(text, data.textData.typeInRegister, data.textData.transferableData)
  }

  private fun putTextAndSetCaretPosition(editor: Editor, context: DataContext, text: ProcessedTextData, data: PutData, additionalData: Map<String, Any>) {
    val subMode = data.visualSelection?.typeInEditor?.toSubMode() ?: CommandState.SubMode.NONE
    if (ClipboardOptionsData.ideaput in OptionsManager.clipboard) {
      val idePasteProvider = getProviderForPasteViaIde(context, text.typeInRegister, data)
      if (idePasteProvider != null) {
        logger.debug("Perform put via idea paste")
        putTextViaIde(idePasteProvider, editor, context, text, subMode, data, additionalData)
        return
      }
    }

    notifyAboutIdeaPut(editor.project)
    logger.debug("Perform put via plugin")
    val myCarets = if (data.visualSelection != null) {
      data.visualSelection.caretsAndSelections.keys.sortedByDescending { it.logicalPosition }
    } else {
      EditorHelper.getOrderedCaretsList(editor)
    }
    ApplicationManager.getApplication().runWriteAction {
      myCarets.forEach { caret -> putForCaret(editor, caret, data, additionalData, context, text) }
    }
  }

  private fun putForCaret(editor: Editor, caret: Caret, data: PutData, additionalData: Map<String, Any>, context: DataContext, text: ProcessedTextData) {
    if (data.visualSelection?.typeInEditor == SelectionType.LINE_WISE && editor.isOneLineMode) return
    val startOffsets = prepareDocumentAndGetStartOffsets(editor, caret, text.typeInRegister, data, additionalData)

    startOffsets.forEach { startOffset ->
      val subMode = data.visualSelection?.typeInEditor?.toSubMode() ?: CommandState.SubMode.NONE
      val endOffset = putTextInternal(editor, caret, context, text.text, text.typeInRegister, subMode,
        startOffset, data.count, data.indent, data.caretAfterInsertedText)
      VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, endOffset))
      moveCaretToEndPosition(editor, caret, startOffset, endOffset, text.typeInRegister, subMode, data.caretAfterInsertedText)
    }
  }

  private fun prepareDocumentAndGetStartOffsets(editor: Editor, caret: Caret, typeInRegister: SelectionType, data: PutData, additionalData: Map<String, Any>): List<Int> {
    val application = ApplicationManager.getApplication()
    if (data.visualSelection != null) {
      return when {
        data.visualSelection.typeInEditor == SelectionType.CHARACTER_WISE && typeInRegister == SelectionType.LINE_WISE -> {
          application.runWriteAction { editor.document.insertString(caret.offset, "\n") }
          listOf(caret.offset + 1)
        }
        data.visualSelection.typeInEditor == SelectionType.BLOCK_WISE -> {
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
            SelectionType.BLOCK_WISE -> listOf(editor.logicalPositionToOffset(LogicalPosition(firstSelectedLine, startColumnOfSelection)))
          }
        }
        else -> listOf(caret.offset)
      }
    } else {
      if (data.insertTextBeforeCaret) {
        return when (typeInRegister) {
          SelectionType.LINE_WISE -> listOf(VimPlugin.getMotion().moveCaretToLineStart(editor, caret))
          else -> listOf(caret.offset)
        }
      }

      var startOffset: Int
      val line = if (data.putToLine < 0) caret.logicalPosition.line else data.putToLine
      when (typeInRegister) {
        SelectionType.LINE_WISE -> {
          startOffset = min(editor.document.textLength, VimPlugin.getMotion().moveCaretToLineEnd(editor, line, true) + 1)
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

  private fun getProviderForPasteViaIde(context: DataContext, typeInRegister: SelectionType, data: PutData): PasteProvider? {
    if (data.visualSelection != null && data.visualSelection.typeInEditor == SelectionType.BLOCK_WISE) return null
    if ((typeInRegister == SelectionType.LINE_WISE || typeInRegister == SelectionType.CHARACTER_WISE) && data.count == 1) {
      val provider = PlatformDataKeys.PASTE_PROVIDER.getData(context)
      if (provider != null && provider.isPasteEnabled(context)) return provider
    }
    return null
  }

  private fun putTextViaIde(pasteProvider: PasteProvider, editor: Editor, context: DataContext, text: ProcessedTextData, subMode: CommandState.SubMode, data: PutData, additionalData: Map<String, Any>) {
    val carets: MutableMap<Caret, RangeMarker> = mutableMapOf()
    EditorHelper.getOrderedCaretsList(editor).forEach { caret ->
      val startOffset = prepareDocumentAndGetStartOffsets(editor, caret, text.typeInRegister, data, additionalData).first()
      val pointMarker = editor.document.createRangeMarker(startOffset, startOffset)
      caret.moveToOffset(startOffset)
      carets[caret] = pointMarker
    }

    val sizeBeforeInsert = CopyPasteManager.getInstance().allContents.size
    val origContent: TextBlockTransferable = setClipboardText(text.text, text.transferableData)
    val sizeAfterInsert = CopyPasteManager.getInstance().allContents.size
    try {
      pasteProvider.performPaste(context)
    } finally {
      if (sizeBeforeInsert != sizeAfterInsert) {
        // Sometimes inserted text replaces existing one. E.g. on insert with + or * register
        (CopyPasteManager.getInstance() as? CopyPasteManagerEx)?.run { removeContent(origContent) }
      }
    }

    carets.forEach { (caret, point) ->
      val startOffset = point.startOffset
      point.dispose()
      if (!caret.isValid) return@forEach
      val endOffset = if (data.indent) doIndent(editor, caret, context, startOffset, startOffset + text.text.length) else startOffset + text.text.length
      VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, endOffset))
      VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset)
      moveCaretToEndPosition(editor, caret, startOffset, endOffset, text.typeInRegister, subMode, data.caretAfterInsertedText)
    }
  }

  private fun setClipboardText(text: String, transferableData: List<TextBlockTransferableData>): TextBlockTransferable {
    val mutableTransferableData = transferableData.toMutableList()
    val s = TextBlockTransferable.convertLineSeparators(text, "\n", transferableData)
    if (mutableTransferableData.none { it is CaretStateTransferableData }) {
      // Manually add CaretStateTransferableData to avoid adjustment of copied text to multicaret
      mutableTransferableData += CaretStateTransferableData(intArrayOf(0), intArrayOf(s.length))
    }
    logger.debug { "Paste text with transferable data: ${transferableData.joinToString { it.javaClass.name }}" }
    val content = TextBlockTransferable(s, mutableTransferableData, RawText(text))
    CopyPasteManager.getInstance().setContents(content)
    return content
  }

  private fun putTextInternal(editor: Editor, caret: Caret, context: DataContext,
                              text: String, type: SelectionType, mode: CommandState.SubMode,
                              startOffset: Int, count: Int, indent: Boolean, cursorAfter: Boolean): Int =
    when (type) {
      SelectionType.CHARACTER_WISE -> putTextCharacterwise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter)
      SelectionType.LINE_WISE -> putTextLinewise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter)
      else -> putTextBlockwise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter)
    }

  private fun putTextLinewise(editor: Editor, caret: Caret, context: DataContext,
                              text: String, type: SelectionType, mode: CommandState.SubMode,
                              startOffset: Int, count: Int, indent: Boolean, cursorAfter: Boolean): Int {
    val caretModel = editor.caretModel
    val overlappedCarets = ArrayList<Caret>(caretModel.caretCount)
    for (possiblyOverlappedCaret in caretModel.allCarets) {
      if (possiblyOverlappedCaret.offset != startOffset || possiblyOverlappedCaret === caret) continue

      MotionGroup.moveCaret(editor, possiblyOverlappedCaret,
        VimPlugin.getMotion().moveCaretHorizontal(editor, possiblyOverlappedCaret, 1, true))
      overlappedCarets.add(possiblyOverlappedCaret)
    }

    val endOffset = putTextCharacterwise(editor, caret, context, text, type, mode, startOffset, count, indent,
      cursorAfter)

    for (overlappedCaret in overlappedCarets) {
      MotionGroup.moveCaret(editor, overlappedCaret,
        VimPlugin.getMotion().moveCaretHorizontal(editor, overlappedCaret, -1, true))
    }

    return endOffset
  }

  private fun putTextBlockwise(editor: Editor, caret: Caret, context: DataContext,
                               text: String, type: SelectionType, mode: CommandState.SubMode,
                               startOffset: Int, count: Int, indent: Boolean, cursorAfter: Boolean): Int {
    val startPosition = editor.offsetToLogicalPosition(startOffset)
    val currentColumn = if (mode == CommandState.SubMode.VISUAL_LINE) 0 else startPosition.column
    var currentLine = startPosition.line

    val lineCount = StringUtil.getLineBreakCount(text) + 1
    if (currentLine + lineCount >= EditorHelper.getLineCount(editor)) {
      val limit = currentLine + lineCount - EditorHelper.getLineCount(editor)
      for (i in 0 until limit) {
        MotionGroup.moveCaret(editor, caret, EditorHelper.getFileSize(editor, true))
        VimPlugin.getChange().insertText(editor, caret, "\n")
      }
    }

    val maxLen = getMaxSegmentLength(text)
    val tokenizer = StringTokenizer(text, "\n")
    var endOffset = startOffset
    while (tokenizer.hasMoreTokens()) {
      var segment = tokenizer.nextToken()
      var origSegment = segment

      if (segment.length < maxLen) {
        segment += " ".repeat(maxLen - segment.length)

        if (currentColumn != 0 && currentColumn < EditorHelper.getLineLength(editor, currentLine)) {
          origSegment = segment
        }
      }

      val pad = EditorHelper.pad(editor, context, currentLine, currentColumn)

      val insertOffset = editor.logicalPositionToOffset(LogicalPosition(currentLine, currentColumn))
      MotionGroup.moveCaret(editor, caret, insertOffset)
      val insertedText = origSegment + segment.repeat(count - 1)
      VimPlugin.getChange().insertText(editor, caret, insertedText)
      endOffset += insertedText.length

      if (mode == CommandState.SubMode.VISUAL_LINE) {
        MotionGroup.moveCaret(editor, caret, endOffset)
        VimPlugin.getChange().insertText(editor, caret, "\n")
        ++endOffset
      } else {
        if (pad.isNotEmpty()) {
          MotionGroup.moveCaret(editor, caret, insertOffset)
          VimPlugin.getChange().insertText(editor, caret, pad)
          endOffset += pad.length
        }
      }

      ++currentLine
    }

    if (indent) endOffset = doIndent(editor, caret, context, startOffset, endOffset)
    moveCaretToEndPosition(editor, caret, startOffset, endOffset, type, mode, cursorAfter)

    return endOffset
  }

  private fun putTextCharacterwise(editor: Editor, caret: Caret, context: DataContext,
                                   text: String, type: SelectionType,
                                   mode: CommandState.SubMode, startOffset: Int, count: Int, indent: Boolean,
                                   cursorAfter: Boolean): Int {
    MotionGroup.moveCaret(editor, caret, startOffset)
    val insertedText = text.repeat(count)
    VimPlugin.getChange().insertText(editor, caret, insertedText)

    val endOffset = if (indent)
      doIndent(editor, caret, context, startOffset, startOffset + insertedText.length)
    else
      startOffset + insertedText.length
    moveCaretToEndPosition(editor, caret, startOffset, endOffset, type, mode, cursorAfter)

    return endOffset
  }

  private fun moveCaretToEndPosition(
    editor: Editor,
    caret: Caret,
    startOffset: Int,
    endOffset: Int,
    typeInRegister: SelectionType,
    modeInEditor: CommandState.SubMode,
    caretAfterInsertedText: Boolean
  ) {
    val cursorMode = when (typeInRegister) {
      SelectionType.BLOCK_WISE -> when (modeInEditor) {
        CommandState.SubMode.VISUAL_LINE -> if (caretAfterInsertedText) "postEndOffset" else "startOffset"
        else -> if (caretAfterInsertedText) "preLineEndOfEndOffset" else "startOffset"
      }
      SelectionType.LINE_WISE -> if (caretAfterInsertedText) "postEndOffset" else "startOffsetSkipLeading"
      SelectionType.CHARACTER_WISE -> when (modeInEditor) {
        CommandState.SubMode.VISUAL_LINE -> if (caretAfterInsertedText) "postEndOffset" else "startOffset"
        else -> if (caretAfterInsertedText) "preLineEndOfEndOffset" else "preEndOffset"
      }
    }

    when (cursorMode) {
      "startOffset" -> MotionGroup.moveCaret(editor, caret, startOffset)
      "preEndOffset" -> MotionGroup.moveCaret(editor, caret, endOffset - 1)
      "startOffsetSkipLeading" -> {
        MotionGroup.moveCaret(editor, caret, startOffset)
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret))
      }
      "postEndOffset" -> MotionGroup.moveCaret(editor, caret, endOffset + 1)
      "preLineEndOfEndOffset" -> {
        val pos = min(endOffset, EditorHelper.getLineEndForOffset(editor, endOffset - 1) - 1)
        MotionGroup.moveCaret(editor, caret, pos)
      }
    }
  }

  private fun doIndent(editor: Editor, caret: Caret, context: DataContext, startOffset: Int, endOffset: Int): Int {
    val startLine = editor.offsetToLogicalPosition(startOffset).line
    val endLine = editor.offsetToLogicalPosition(endOffset - 1).line
    val startLineOffset = editor.document.getLineStartOffset(startLine)
    val endLineOffset = editor.document.getLineEndOffset(endLine)

    VimPlugin.getChange().autoIndentRange(editor, caret, context, TextRange(startLineOffset, endLineOffset))
    return EditorHelper.getLineEndOffset(editor, endLine, true)
  }

  private fun getMaxSegmentLength(text: String): Int {
    val tokenizer = StringTokenizer(text, "\n")
    var maxLen = 0
    while (tokenizer.hasMoreTokens()) {
      val s = tokenizer.nextToken()
      maxLen = max(s.length, maxLen)
    }
    return maxLen
  }

  private fun notifyAboutIdeaPut(project: Project?) {
    if (VimPlugin.getVimState().isIdeaPutNotified
      || ClipboardOptionsData.ideaput in OptionsManager.clipboard
      || ClipboardOptionsData.ideaputDisabled) return

    VimPlugin.getVimState().isIdeaPutNotified = true

    VimPlugin.getNotifications(project).notifyAboutIdeaPut()
  }

  companion object {
    private val logger = Logger.getInstance(PutGroup::class.java.name)
  }
}
