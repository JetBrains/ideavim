/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.isLineEmpty
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.isBlock
import com.maddyhome.idea.vim.command.isChar
import com.maddyhome.idea.vim.command.isLine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.firstOrNull
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class VimPutBase : VimPut {
  override fun putText(
    editor: VimEditor,
    context: ExecutionContext,
    data: PutData,
    operatorArguments: OperatorArguments,
    updateVisualMarks: Boolean,
  ): Boolean {
    val additionalData = collectPreModificationData(editor, data)
    deleteSelectedText(editor, data, operatorArguments)
    val processedText = processText(editor, data) ?: return false
    putTextAndSetCaretPosition(editor, context, processedText, data, additionalData)

    if (updateVisualMarks) {
      wrapInsertedTextWithVisualMarks(editor, data, processedText)
    }

    return true
  }

  private fun collectPreModificationData(editor: VimEditor, data: PutData): Map<String, Any> {
    return if (data.visualSelection != null && data.visualSelection.typeInEditor.isBlock) {
      val vimSelection = data.visualSelection.caretsAndSelections.getValue(editor.primaryCaret())
      val selStart = editor.offsetToBufferPosition(vimSelection.vimStart)
      val selEnd = editor.offsetToBufferPosition(vimSelection.vimEnd)
      mapOf(
        "startColumnOfSelection" to min(selStart.column, selEnd.column),
        "selectedLines" to abs(selStart.line - selEnd.line),
        "firstSelectedLine" to min(selStart.line, selEnd.line)
      )
    } else mutableMapOf()
  }

  private fun wasTextInsertedLineWise(text: ProcessedTextData): Boolean {
    return text.typeInRegister == SelectionType.LINE_WISE
  }

  /**
   * see ":h gv":
   * After using "p" or "P" in Visual mode the text that was put will be selected
   */
  private fun wrapInsertedTextWithVisualMarks(editor: VimEditor, data: PutData, text: ProcessedTextData) {
    val textLength: Int = data.textData?.rawText?.length ?: return
    val currentCaret = editor.currentCaret()
    val caretsAndSelections = data.visualSelection?.caretsAndSelections ?: return
    val selection = caretsAndSelections[currentCaret] ?: caretsAndSelections.firstOrNull()?.value ?: return

    val leftIndex = min(selection.vimStart, selection.vimEnd)
    val rightIndex = leftIndex + textLength - 1

    val rangeForMarks = if (wasTextInsertedLineWise(text)) {
      // here we skip the \n char after the inserted text
      TextRange(leftIndex, rightIndex - 1)
    } else {
      TextRange(leftIndex, rightIndex)
    }

    editor.vimLastSelectionType = SelectionType.CHARACTER_WISE
    injector.markGroup.setVisualSelectionMarks(editor, rangeForMarks)
  }

  private fun deleteSelectedText(editor: VimEditor, data: PutData, operatorArguments: OperatorArguments) {
    if (data.visualSelection == null) return

    data.visualSelection.caretsAndSelections.entries.sortedByDescending { it.key.getBufferPosition() }
      .forEach { (caret, selection) ->
        if (!caret.isValid) return@forEach
        val range = selection.toVimTextRange(false).normalize()

        injector.application.runWriteAction {
          injector.changeGroup.deleteRange(editor, caret, range, selection.type, false, operatorArguments)
        }
        caret.moveToInlayAwareOffset(range.startOffset)
      }
  }

  private fun processText(editor: VimEditor, data: PutData): ProcessedTextData? {
    var text = data.textData?.rawText ?: run {
      if (data.visualSelection != null) {
        val offset = editor.primaryCaret().offset.point
        injector.markGroup.setMark(editor, MARK_CHANGE_POS, offset)
        injector.markGroup.setChangeMarks(editor, TextRange(offset, offset + 1))
      }
      return null
    }

    if (data.visualSelection?.typeInEditor?.isLine == true && data.textData.typeInRegister.isChar) text += "\n"

    if (data.textData.typeInRegister.isLine && text.isNotEmpty() && text.last() != '\n') text += '\n'

    if (data.textData.typeInRegister.isChar && text.lastOrNull() == '\n' && data.visualSelection?.typeInEditor?.isLine == false) text =
      text.dropLast(1)

    return ProcessedTextData(text, data.textData.typeInRegister, data.textData.transferableData)
  }

  protected fun moveCaretToEndPosition(
    editor: VimEditor,
    caret: VimCaret,
    startOffset: Int,
    endOffset: Int,
    typeInRegister: SelectionType,
    modeInEditor: VimStateMachine.SubMode,
    caretAfterInsertedText: Boolean,
  ) {
    val cursorMode = when (typeInRegister) {
      SelectionType.BLOCK_WISE -> when (modeInEditor) {
        VimStateMachine.SubMode.VISUAL_LINE -> if (caretAfterInsertedText) "postEndOffset" else "startOffset"
        else -> if (caretAfterInsertedText) "preLineEndOfEndOffset" else "startOffset"
      }
      SelectionType.LINE_WISE -> if (caretAfterInsertedText) "postEndOffset" else "startOffsetSkipLeading"
      SelectionType.CHARACTER_WISE -> when (modeInEditor) {
        VimStateMachine.SubMode.VISUAL_LINE -> if (caretAfterInsertedText) "postEndOffset" else "startOffset"
        else -> if (caretAfterInsertedText) "preLineEndOfEndOffset" else "preEndOffset"
      }
    }

    when (cursorMode) {
      "startOffset" -> caret.moveToOffset(startOffset)
      "preEndOffset" -> caret.moveToOffset(endOffset - 1)
      "startOffsetSkipLeading" -> {
        caret.moveToOffset(startOffset)
        caret.moveToOffset(injector.motion.moveCaretToCurrentLineStartSkipLeading(editor, caret))
      }
      "postEndOffset" -> caret.moveToOffset(endOffset + 1)
      "preLineEndOfEndOffset" -> {
        var rightestPosition = editor.getLineEndForOffset(endOffset - 1)
        if (editor.mode != VimStateMachine.Mode.INSERT) --rightestPosition // it's not possible to place a caret at the end of the line in any mode except insert
        val pos = min(endOffset, rightestPosition)
        caret.moveToOffset(pos)
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
    TODO("Not yet implemented")
  }

  private fun putTextCharacterwise(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    text: String,
    type: SelectionType,
    mode: VimStateMachine.SubMode,
    startOffset: Int,
    count: Int,
    indent: Boolean,
    cursorAfter: Boolean,
  ): Int {
    caret.moveToOffset(startOffset)
    val insertedText = text.repeat(count)
    injector.changeGroup.insertText(editor, caret, insertedText)

    val endOffset = if (indent)
      doIndent(editor, caret, context, startOffset, startOffset + insertedText.length)
    else
      startOffset + insertedText.length
    moveCaretToEndPosition(editor, caret, startOffset, endOffset, type, mode, cursorAfter)

    return endOffset
  }

  private fun putTextLinewise(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    text: String,
    type: SelectionType,
    mode: VimStateMachine.SubMode,
    startOffset: Int,
    count: Int,
    indent: Boolean,
    cursorAfter: Boolean,
  ): Int {
    val overlappedCarets = ArrayList<VimCaret>(editor.carets().size)
    for (possiblyOverlappedCaret in editor.carets()) {
      if (possiblyOverlappedCaret.offset.point != startOffset || possiblyOverlappedCaret == caret) continue

      possiblyOverlappedCaret.moveToOffset(
        injector.motion.getOffsetOfHorizontalMotion(editor, possiblyOverlappedCaret, 1, true)
      )
      overlappedCarets.add(possiblyOverlappedCaret)
    }

    val endOffset = putTextCharacterwise(
      editor, caret, context, text, type, mode, startOffset, count, indent,
      cursorAfter
    )

    for (overlappedCaret in overlappedCarets) {
      overlappedCaret.moveToOffset(
        injector.motion.getOffsetOfHorizontalMotion(editor, overlappedCaret, -1, true)
      )
    }

    return endOffset
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

  private fun putTextBlockwise(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    text: String,
    type: SelectionType,
    mode: VimStateMachine.SubMode,
    startOffset: Int,
    count: Int,
    indent: Boolean,
    cursorAfter: Boolean,
  ): Int {
    val startPosition = editor.offsetToBufferPosition(startOffset)
    val currentColumn = if (mode == VimStateMachine.SubMode.VISUAL_LINE) 0 else startPosition.column
    var currentLine = startPosition.line

    val lineCount = text.getLineBreakCount() + 1
    if (currentLine + lineCount >= editor.nativeLineCount()) {
      val limit = currentLine + lineCount - editor.nativeLineCount()
      for (i in 0 until limit) {
        caret.moveToOffset(editor.fileSize().toInt())
        injector.changeGroup.insertText(editor, caret, "\n")
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

        if (currentColumn != 0 && currentColumn < editor.lineLength(currentLine)) {
          origSegment = segment
        }
      }

      val pad = injector.engineEditorHelper.pad(editor, context, currentLine, currentColumn)

      val insertOffset = editor.bufferPositionToOffset(BufferPosition(currentLine, currentColumn))
      caret.moveToOffset(insertOffset)
      val insertedText = origSegment + segment.repeat(count - 1)
      injector.changeGroup.insertText(editor, caret, insertedText)
      endOffset += insertedText.length

      if (mode == VimStateMachine.SubMode.VISUAL_LINE) {
        caret.moveToOffset(endOffset)
        injector.changeGroup.insertText(editor, caret, "\n")
        ++endOffset
      } else {
        if (pad.isNotEmpty()) {
          caret.moveToOffset(insertOffset)
          injector.changeGroup.insertText(editor, caret, pad)
          endOffset += pad.length
        }
      }

      ++currentLine
    }

    if (indent) endOffset = doIndent(editor, caret, context, startOffset, endOffset)
    moveCaretToEndPosition(editor, caret, startOffset, endOffset, type, mode, cursorAfter)

    return endOffset
  }

  private fun putTextInternal(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    text: String,
    type: SelectionType,
    mode: VimStateMachine.SubMode,
    startOffset: Int,
    count: Int,
    indent: Boolean,
    cursorAfter: Boolean,
  ): Int {
    return when (type) {
      SelectionType.CHARACTER_WISE -> putTextCharacterwise(
        editor,
        caret,
        context,
        text,
        type,
        mode,
        startOffset,
        count,
        indent,
        cursorAfter
      )
      SelectionType.LINE_WISE -> putTextLinewise(
        editor,
        caret,
        context,
        text,
        type,
        mode,
        startOffset,
        count,
        indent,
        cursorAfter
      )
      else -> putTextBlockwise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter)
    }
  }

  protected fun prepareDocumentAndGetStartOffsets(
    vimEditor: VimEditor,
    vimCaret: VimCaret,
    typeInRegister: SelectionType,
    data: PutData,
    additionalData: Map<String, Any>,
  ): List<Int> {
    val application = injector.application
    val visualSelection = data.visualSelection
    if (visualSelection != null) {
      return when {
        visualSelection.typeInEditor.isChar && typeInRegister.isLine -> {
          application.runWriteAction { (vimEditor as MutableVimEditor).insertText(vimCaret.offset, "\n") }
          listOf(vimCaret.offset.point + 1)
        }
        visualSelection.typeInEditor.isBlock -> {
          val firstSelectedLine = additionalData["firstSelectedLine"] as Int
          val selectedLines = additionalData["selectedLines"] as Int
          val startColumnOfSelection = additionalData["startColumnOfSelection"] as Int
          val line = if (data.insertTextBeforeCaret) firstSelectedLine else firstSelectedLine + selectedLines
          when (typeInRegister) {
            SelectionType.LINE_WISE -> when {
              data.insertTextBeforeCaret -> listOf(vimEditor.getLineStartOffset(line))
              else -> {
                val pos = vimEditor.getLineEndOffset(line, true)
                application.runWriteAction { (vimEditor as MutableVimEditor).insertText(pos.offset, "\n") }
                listOf(pos + 1)
              }
            }
            SelectionType.CHARACTER_WISE -> (firstSelectedLine + selectedLines downTo firstSelectedLine)
              .map { vimEditor.bufferPositionToOffset(BufferPosition(it, startColumnOfSelection)) }
            SelectionType.BLOCK_WISE -> listOf(
              vimEditor.bufferPositionToOffset(
                BufferPosition(
                  firstSelectedLine,
                  startColumnOfSelection
                )
              )
            )
          }
        }
        visualSelection.typeInEditor.isLine -> {
          val lastChar = if (vimEditor.fileSize() > 0) {
            vimEditor.getText(TextRange(vimEditor.fileSize().toInt() - 1, vimEditor.fileSize().toInt()))[0]
          } else {
            null
          }
          if (vimCaret.offset.point == vimEditor.fileSize().toInt() && vimEditor.fileSize().toInt() != 0 && lastChar != '\n') {
            application.runWriteAction { (vimEditor as MutableVimEditor).insertText(vimCaret.offset, "\n") }
            listOf(vimCaret.offset.point + 1)
          } else listOf(vimCaret.offset.point)
        }
        else -> listOf(vimCaret.offset.point)
      }
    } else {
      if (data.insertTextBeforeCaret) {
        return when (typeInRegister) {
          SelectionType.LINE_WISE -> listOf(injector.motion.moveCaretToCurrentLineStart(vimEditor, vimCaret))
          else -> listOf(vimCaret.offset.point)
        }
      }

      var startOffset: Int
      val line = if (data.putToLine < 0) vimCaret.getBufferPosition().line else data.putToLine
      when (typeInRegister) {
        SelectionType.LINE_WISE -> {
          startOffset =
            min(vimEditor.text().length, injector.motion.moveCaretToLineEnd(vimEditor, line, true) + 1)
          if (startOffset > 0 && startOffset == vimEditor.text().length && vimEditor.text()[startOffset - 1] != '\n') {
            application.runWriteAction { (vimEditor as MutableVimEditor).insertText(startOffset.offset, "\n") }
            startOffset++
          }
        }
        else -> {
          startOffset = vimCaret.offset.point
          if (!vimEditor.isLineEmpty(line, false)) {
            startOffset++
          }
        }
      }

      return if (startOffset > vimEditor.text().length) listOf(vimEditor.text().length) else listOf(startOffset)
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
        injector.markGroup.setChangeMarks(editor, TextRange(startOffset, endOffset))
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
    val myCarets = visualSelection?.caretsAndSelections?.keys?.sortedByDescending { it.getBufferPosition() }
      ?: editor.sortedNativeCarets().reversed()
    injector.application.runWriteAction {
      myCarets.forEach { caret -> putForCaret(editor, caret, data, additionalData, context, text) }
    }
  }

  companion object {
    val logger by lazy { vimLogger<VimPutBase>() }
  }
}

// This is taken from StringUtil of IntelliJ IDEA
private fun CharSequence.getLineBreakCount(): Int {
  var count = 0
  var i = 0
  while (i < length) {
    val c = this[i]
    if (c == '\n') {
      count++
    } else if (c == '\r') {
      if (i + 1 < length && this[i + 1] == '\n') {
        i++
      }
      count++
    }
    i++
  }
  return count
}
