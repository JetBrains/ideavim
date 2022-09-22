package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimLogicalPosition
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.isBlock
import com.maddyhome.idea.vim.command.isChar
import com.maddyhome.idea.vim.command.isLine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.firstOrNull
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS
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

  protected fun collectPreModificationData(editor: VimEditor, data: PutData): Map<String, Any> {
    return if (data.visualSelection != null && data.visualSelection.typeInEditor.isBlock) {
      val vimSelection = data.visualSelection.caretsAndSelections.getValue(editor.primaryCaret())
      val selStart = editor.offsetToLogicalPosition(vimSelection.vimStart)
      val selEnd = editor.offsetToLogicalPosition(vimSelection.vimEnd)
      mapOf(
        "startColumnOfSelection" to min(selStart.column, selEnd.column),
        "selectedLines" to abs(selStart.line - selEnd.line),
        "firstSelectedLine" to min(selStart.line, selEnd.line)
      )
    } else mutableMapOf()
  }

  protected fun wasTextInsertedLineWise(text: ProcessedTextData): Boolean {
    return text.typeInRegister == SelectionType.LINE_WISE
  }

  /**
   * see ":h gv":
   * After using "p" or "P" in Visual mode the text that was put will be selected
   */
  protected fun wrapInsertedTextWithVisualMarks(editor: VimEditor, data: PutData, text: ProcessedTextData) {
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

  protected fun deleteSelectedText(editor: VimEditor, data: PutData, operatorArguments: OperatorArguments) {
    if (data.visualSelection == null) return

    data.visualSelection.caretsAndSelections.entries.sortedByDescending { it.key.getLogicalPosition() }
      .forEach { (caret, selection) ->
        if (!caret.isValid) return@forEach
        val range = selection.toVimTextRange(false).normalize()

        injector.application.runWriteAction {
          injector.changeGroup.deleteRange(editor, caret, range, selection.type, false, operatorArguments)
        }
        caret.moveToInlayAwareOffset(range.startOffset)
      }
  }

  protected fun processText(editor: VimEditor, data: PutData): ProcessedTextData? {
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
        caret.moveToOffset(injector.motion.moveCaretToLineStartSkipLeading(editor, caret))
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

  protected fun putTextCharacterwise(
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

  protected fun putTextLinewise(
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

  protected fun getMaxSegmentLength(text: String): Int {
    val tokenizer = StringTokenizer(text, "\n")
    var maxLen = 0
    while (tokenizer.hasMoreTokens()) {
      val s = tokenizer.nextToken()
      maxLen = max(s.length, maxLen)
    }
    return maxLen
  }

  protected fun putTextBlockwise(
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
    val startPosition = editor.offsetToLogicalPosition(startOffset)
    val currentColumn = if (mode == VimStateMachine.SubMode.VISUAL_LINE) 0 else startPosition.column
    var currentLine = startPosition.line

    val lineCount = injector.engineEditorHelper.getLineBreakCount(text) + 1
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

        if (currentColumn != 0 && currentColumn < injector.engineEditorHelper.getLineLength(editor, currentLine)) {
          origSegment = segment
        }
      }

      val pad = injector.engineEditorHelper.pad(editor, context, currentLine, currentColumn)

      val insertOffset = editor.logicalPositionToOffset(VimLogicalPosition(currentLine, currentColumn))
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

  protected fun putTextInternal(
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
}
