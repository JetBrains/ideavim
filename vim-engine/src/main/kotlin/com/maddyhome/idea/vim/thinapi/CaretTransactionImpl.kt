/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Line
import com.intellij.vim.api.Range
import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.caret.CaretRead
import com.intellij.vim.api.scopes.caret.CaretTransaction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.mark.Jump as EngineJump

class CaretTransactionImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
  override val caretId: CaretId,
) : CaretTransaction, CaretRead by CaretReadImpl(caretId), Read by ReadImpl(listenerOwner, mappingOwner) {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  private val executionContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  private val vimCaret: VimCaret
    get() = vimEditor.carets().firstOrNull { it.id == caretId.id } ?: vimEditor.primaryCaret()

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

  private suspend fun putText(
    startPosition: Int,
    text: String,
    caretAfterInsertedText: Boolean,
  ): Boolean {
    val copiedText = injector.clipboardManager.dumbCopiedText(text)
    val textData = PutData.TextData(null, copiedText, SelectionType.CHARACTER_WISE)

    val putData = PutData(
      textData = textData,
      visualSelection = null,
      count = 1,
      insertTextBeforeCaret = caretAfterInsertedText,
      rawIndent = false,
      caretAfterInsertedText = caretAfterInsertedText,
      putToLine = -1
    )
    vimCaret.moveToOffset(startPosition)

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
    caretAfterInsertedText: Boolean,
  ): Boolean {
    return putText(position, text, caretAfterInsertedText)
  }

  override suspend fun replaceText(
    startOffset: Int,
    endOffset: Int,
    text: String,
  ): Boolean {
    val copiedText = injector.clipboardManager.dumbCopiedText(text)
    val textData = PutData.TextData(null, copiedText, SelectionType.CHARACTER_WISE)

    val emptySelection = startOffset == endOffset

    val selectionType = determineSelectionType(startOffset, endOffset)

    val visualSelection = PutData.VisualSelection(
      mapOf(
        vimCaret to VimSelection.create(
          startOffset,
          if (emptySelection) endOffset else endOffset - 1,
          selectionType,
          vimEditor
        )
      ),
      selectionType
    )

    val putData = PutData(
      textData = textData,
      visualSelection = visualSelection,
      count = 1,
      insertTextBeforeCaret = true, // Always insert before caret for replace
      rawIndent = false,
      caretAfterInsertedText = false,
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

  override suspend fun replaceTextForRange(startOffset: Int, endOffset: Int, text: String) {
    injector.changeGroup.replaceText(vimEditor, vimCaret, startOffset, endOffset, text)
  }

  override suspend fun deleteText(
    startOffset: Int,
    endOffset: Int,
  ): Boolean {
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
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return
    caret.moveToOffset(offset)
    selection?.let { (start, end) ->
      caret.setSelection(start, end)
    } ?: caret.removeSelection()
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