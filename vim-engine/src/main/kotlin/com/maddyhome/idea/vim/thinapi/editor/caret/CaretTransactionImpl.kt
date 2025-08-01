/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.editor.caret

import com.intellij.vim.api.models.CaretId
import com.intellij.vim.api.models.Line
import com.intellij.vim.api.models.Range
import com.intellij.vim.api.scopes.editor.Read
import com.intellij.vim.api.scopes.editor.caret.CaretRead
import com.intellij.vim.api.scopes.editor.caret.CaretTransaction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.thinapi.editor.ReadImpl
import com.maddyhome.idea.vim.mark.Jump as EngineJump

class CaretTransactionImpl(
  override val caretId: CaretId,
) : CaretTransaction, CaretRead by CaretReadImpl(caretId), Read by ReadImpl() {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  private val executionContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  private val vimCaret: VimCaret
    get() = vimEditor.carets().firstOrNull { it.id == caretId.id } ?: vimEditor.primaryCaret()

  private fun assertOffsetInRange(offset: Int, range: IntRange) {
    require(offset in range) {
      "Offset $offset is not within the valid range ${range.start}..${range.endInclusive}."
    }
  }

  private fun determineSelectionType(startOffset: Int, endOffset: Int): SelectionType {
    val endOffsetNormalized = endOffset.coerceAtMost(vimEditor.fileSize().toInt())
    if (endOffsetNormalized == startOffset) return SelectionType.CHARACTER_WISE

    val selectedText = vimEditor.text().subSequence(startOffset, endOffsetNormalized)

    val startLine = vimEditor.offsetToBufferPosition(startOffset).line
    val endLine = vimEditor.offsetToBufferPosition(endOffset).line

    val lineStartOffset = vimEditor.getLineStartOffset(startLine)
    val lineEndOffset = vimEditor.getLineEndOffset(endLine)

    val isLine: Boolean = (startOffset == lineStartOffset && endOffset == lineEndOffset) &&
      selectedText.endsWith("\n")

    return if (startLine == endLine) {
      if (isLine) {
        SelectionType.LINE_WISE
      } else {
        SelectionType.CHARACTER_WISE
      }
    } else {
      SelectionType.LINE_WISE
    }
  }

  private fun putTextInternal(
    text: String,
    visualSelection: PutData.VisualSelection?,
    caretAfterInsertedText: Boolean,
    beforeCaret: Boolean,
  ): Boolean {
    val copiedText = injector.clipboardManager.dumbCopiedText(text)
    val textData = PutData.TextData(null, copiedText, SelectionType.CHARACTER_WISE)

    val putData = PutData(
      textData = textData,
      visualSelection = visualSelection,
      count = 1,
      insertTextBeforeCaret = beforeCaret,
      rawIndent = false,
      caretAfterInsertedText = caretAfterInsertedText,
      putToLine = -1
    )

    val result: Boolean = injector.put.putTextForCaret(
      vimEditor,
      vimCaret,
      executionContext,
      putData,
      updateVisualMarks = false,
      modifyRegister = false
    )
    return result
  }

  override suspend fun insertText(
    position: Int,
    text: String,
    caretAtEnd: Boolean,
    insertBeforeCaret: Boolean,
  ): Boolean {
    val fileLength = vimEditor.text().length
    val validRange = 0..fileLength
    assertOffsetInRange(position, validRange)

    vimCaret.moveToOffset(position)

    val returnValue = putTextInternal(text, null, caretAtEnd, insertBeforeCaret)

    val newFileLength = vimEditor.text().length
    val endBoundary = if (newFileLength > 0) newFileLength - 1 else 0

    val newOffset = if (caretAtEnd) {
      if (insertBeforeCaret) {
        position + text.length - 1
      } else {
        position + text.length
      }
    } else {
      if (insertBeforeCaret) {
        position
      } else {
        position + 1
      }
    }.coerceIn(0, endBoundary)

    vimCaret.moveToOffset(newOffset)

    return returnValue
  }

  override suspend fun replaceText(
    startOffset: Int,
    endOffset: Int,
    text: String,
  ): Boolean {
    if (vimEditor.text().isEmpty()) {
      val fileSize = vimEditor.fileSize().toInt()
      if (startOffset != 0 || endOffset != 0) {
        throw IllegalArgumentException(
          "Invalid offsets for an empty editor: startOffset=$startOffset, endOffset=$endOffset, fileSize=$fileSize."
        )
      }

      insertText(startOffset, text, caretAtEnd = true)
      return true
    }

    val startOffsetValidRange = 0..<vimEditor.fileSize().toInt()
    val endOffsetValidRange = 0..vimEditor.fileSize().toInt()

    assertOffsetInRange(startOffset, startOffsetValidRange)
    assertOffsetInRange(endOffset, endOffsetValidRange)

    if (startOffset > endOffset) {
      throw IllegalArgumentException("Start offset must be less than or equal to end offset!")
    }

    if (startOffset == endOffset) {
      insertText(startOffset, text, caretAtEnd = false, insertBeforeCaret = true)
      return true
    }

    val selectionType = determineSelectionType(startOffset, endOffset)

    val visualSelection = PutData.VisualSelection(
      mapOf(
        vimCaret to VimSelection.create(
          startOffset,
          endOffset - 1,
          selectionType,
          vimEditor
        )
      ),
      selectionType
    )

    return putTextInternal(text, visualSelection, caretAfterInsertedText = false, beforeCaret = true)
  }

  override suspend fun replaceTextBlockwise(range: Range.Block, text: List<String>) {
    val selections: Array<Range.Simple> = range.ranges.sortedByDescending { it.start }.toTypedArray()
    val listOfText = text.reversed()

    if (listOfText.size != selections.size) {
      throw IllegalArgumentException("Text block size must match number of selections!")
    }

    val startOffsetValidRange = 0..<vimEditor.fileSize().toInt()
    val endOffsetValidRange = 0..vimEditor.fileSize().toInt()

    selections.forEach { selection ->
      assertOffsetInRange(selection.start, startOffsetValidRange)
      assertOffsetInRange(selection.end, endOffsetValidRange)
    }

    for (selection in selections.withIndex()) {
      injector.changeGroup.replaceText(
        vimEditor,
        vimCaret,
        selection.value.start,
        selection.value.end,
        listOfText[selection.index]
      )
    }
  }

  override suspend fun deleteText(
    startOffset: Int,
    endOffset: Int,
  ): Boolean {
    val startOffsetValidRange = 0..<vimEditor.fileSize().toInt()
    val endOffsetValidRange = 0..vimEditor.fileSize().toInt()

    assertOffsetInRange(startOffset, startOffsetValidRange)
    assertOffsetInRange(endOffset, endOffsetValidRange)

    val range = TextRange(startOffset, endOffset)

    injector.changeGroup.deleteRange(
      vimEditor,
      executionContext,
      vimCaret,
      range,
      SelectionType.CHARACTER_WISE,
      isChange = false,
      saveToRegister = false
    )

    return true
  }

  override suspend fun updateCaret(offset: Int, selection: Range.Simple?) {
    val textLength = vimEditor.text().length
    val startOffsetValidRange = 0..<textLength
    val endOffsetValidRange = 0..textLength

    assertOffsetInRange(offset, startOffsetValidRange)

    if (selection != null) {
      assertOffsetInRange(selection.start, startOffsetValidRange)
      assertOffsetInRange(selection.end, endOffsetValidRange)
    }

    vimCaret.moveToOffset(offset)

    selection?.let { (start, end) ->
      if (start != end) {
        vimCaret.setSelection(start, end)
      }
    } ?: vimCaret.removeSelection()
  }

  override suspend fun getLineStartOffset(line: Int): Int {
    return vimEditor.getLineStartOffset(line)
  }

  override suspend fun getLineEndOffset(line: Int, allowEnd: Boolean): Int {
    return vimEditor.getLineEndOffset(line)
  }

  override suspend fun getLine(offset: Int): Line {
    val lineNumber = vimEditor.offsetToBufferPosition(offset).line
    val lineText = vimEditor.getLineText(lineNumber)
    val lineStartOffset = vimEditor.getLineStartOffset(lineNumber)
    val lineEndOffset = vimEditor.getLineEndOffset(lineNumber)
    return Line(lineNumber, lineText, lineStartOffset, lineEndOffset)
  }

  override suspend fun addJump(reset: Boolean) {
    val virtualFile = vimEditor.getVirtualFile() ?: return
    val path = virtualFile.path
    val protocol = virtualFile.protocol
    val position = vimEditor.offsetToBufferPosition(vimCaret.offset)
    val jump = EngineJump(position.line, position.column, path, protocol)
    injector.jumpService.addJump(vimEditor.projectId, jump, reset)
  }

  override suspend fun saveJumpLocation() {
    addJump(true)
    injector.markService.setMark(vimEditor, '\'')
    injector.jumpService.includeCurrentCommandAsNavigation(vimEditor)
  }
}
