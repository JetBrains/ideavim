/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.scopes.Transaction
import com.intellij.vim.api.scopes.caret.CaretTransaction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.state.mode.SelectionType

class TransactionImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : Transaction {
  private val vimEditor: VimEditor
    get() = injector.editorService.getFocusedEditor()!!

  private val executionContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  override fun forEachCaret(block: CaretTransaction.() -> Unit) {
    vimEditor.carets().forEach { caret -> CaretTransactionImpl(listenerOwner, mappingOwner, caret.caretId).block() }
  }

  override fun <T> mapEachCaret(block: CaretTransaction.() -> T): List<T> {
    return vimEditor.carets().map { caret -> CaretTransactionImpl(listenerOwner, mappingOwner, caret.caretId).block() }
  }

  override fun forEachCaretSorted(block: CaretTransaction.() -> Unit) {
    vimEditor.sortedCarets()
      .forEach { caret -> CaretTransactionImpl(listenerOwner, mappingOwner, caret.caretId).block() }
  }

  override fun withCaret(
    caretId: CaretId,
    block: CaretTransaction.() -> Unit,
  ) {
    vimEditor.carets().find { it.id == caretId.id }
      ?.let { caret -> block(CaretTransactionImpl(listenerOwner, mappingOwner, caret.caretId)) } ?: return
  }

  override fun insertText(caretId: CaretId, atPosition: Int, text: CharSequence) {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return
    (vimEditor as MutableVimEditor).insertText(caret, atPosition, text)
  }

  override fun deleteText(startOffset: Int, endOffset: Int) {
    vimEditor.deleteString(TextRange(startOffset, endOffset))
  }

  override fun replaceText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: String,
  ) {
    val replaceSequenceSize = endOffset - startOffset - 1
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return
    if (replaceSequenceSize == 0) {
      (vimEditor as MutableVimEditor).insertText(caret, startOffset, text)
    } else {
      val isLastCharInLine = endOffset == vimEditor.getLineEndOffset(caret.getLine(), true) + 1
      val preparedText = if (isLastCharInLine) text.plus("\n") else text
      (vimEditor as MutableVimEditor).replaceString(startOffset, endOffset, preparedText)
    }
  }

  override fun addCaret(offset: Int): CaretId {
    TODO("Not yet implemented")
  }

  override fun removeCaret(caretId: CaretId) {
    TODO("Not yet implemented")
  }

  private fun putText(
    caretId: CaretId,
    startPosition: Int,
    text: String,
    options: Transaction.TextOperationOptions,
    caretAfterText: Boolean,
    insertBeforeCaret: Boolean,
  ): Boolean {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return false

    // Create text data for PutData
    val copiedText = injector.clipboardManager.dumbCopiedText(text)
    val textData = PutData.TextData(null, copiedText, SelectionType.CHARACTER_WISE)

    // Create PutData
    val putData = PutData(
      textData = textData,
      visualSelection = null,
      count = 1,
      insertTextBeforeCaret = insertBeforeCaret,
      rawIndent = options.rawIndent,
      caretAfterInsertedText = caretAfterText,
      putToLine = -1
    )

    // Use executeCommand to wrap document modifications in a command
    var result = false
    injector.actionExecutor.executeCommand(vimEditor, {
      // Use runWriteAction to ensure document modifications are properly handled
      injector.application.runWriteAction {
        // Move caret to insertion position
        caret.moveToOffset(startPosition)

        result = injector.put.putTextForCaret(
          vimEditor,
          caret,
          executionContext,
          putData,
          false,
          options.modifyRegister
        )
      }
    }, "Insert Text", null)

    return result
  }

  override fun insertTextBeforeCaret(
    caretId: CaretId,
    position: Int,
    text: String,
    options: Transaction.TextOperationOptions,
  ): Boolean {
    return putText(caretId, position, text, options, caretAfterText = true, insertBeforeCaret = true)
  }

  override fun insertTextAfterCaret(
    caretId: CaretId,
    position: Int,
    text: String,
    options: Transaction.TextOperationOptions,
  ): Boolean {
    return putText(caretId, position, text, options, caretAfterText = false, insertBeforeCaret = false)
  }

  override fun replaceText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: String,
    options: Transaction.TextOperationOptions,
  ): Boolean {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return false

    val copiedText = injector.clipboardManager.dumbCopiedText(text)
    val textData = PutData.TextData(null, copiedText, SelectionType.CHARACTER_WISE)

    val visualSelection = PutData.VisualSelection(
      mapOf(caret to VimSelection.create(startOffset, endOffset, SelectionType.CHARACTER_WISE, vimEditor)),
      SelectionType.CHARACTER_WISE
    )

    val putData = PutData(
      textData = textData,
      visualSelection = visualSelection,
      count = 1,
      insertTextBeforeCaret = true, // Always insert before caret for replace
      rawIndent = options.rawIndent,
      caretAfterInsertedText = true,
      putToLine = -1
    )

    var result = false
    injector.actionExecutor.executeCommand(vimEditor, {
      injector.application.runWriteAction {
        result = injector.put.putTextForCaret(
          vimEditor,
          caret,
          executionContext,
          putData,
          false,
          options.modifyRegister
        )
      }
    }, "Replace Text", null)

    return result
  }

  override fun deleteText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    options: Transaction.DeleteOptions,
  ): Boolean {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return false

    // Create a range for the text to delete
    val range = TextRange(startOffset, endOffset)

    injector.actionExecutor.executeCommand(vimEditor, {
      injector.application.runWriteAction {
        injector.changeGroup.deleteRange(
          vimEditor,
          executionContext,
          caret,
          range,
          SelectionType.CHARACTER_WISE,
          isChange = options.isChange,
          saveToRegister = options.saveToRegister
        )
      }
    }, "Delete Text", null)

    return true
  }

  override fun insertTextAtLine(
    caretId: CaretId,
    line: Int,
    text: String,
    options: Transaction.TextOperationOptions,
  ) {
    vimEditor.carets().find { it.id == caretId.id } ?: return
    val lineStartOffset = vimEditor.getLineStartOffset(line)

    putText(caretId, lineStartOffset, text, options, caretAfterText = true, insertBeforeCaret = true)
  }

  override fun replaceTextBlockwise(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: List<String>,
  ) {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return
    val firstLine = vimEditor.offsetToBufferPosition(startOffset).line
    val lastLine = text.size + firstLine - 1

    val startDiff = startOffset - vimEditor.getLineStartOffset(firstLine)
    val endDiff = vimEditor.getLineEndOffset(firstLine, true) - endOffset

    text.zip(firstLine..lastLine).forEach { (lineText, line) ->
      val lineStartOffset = vimEditor.getLineStartOffset(line)
      val lineEndOffset = vimEditor.getLineEndOffset(line, true)

      if (line == firstLine) {
        (vimEditor as MutableVimEditor).replaceString(lineStartOffset + startDiff, lineEndOffset - endDiff, lineText)
      } else {
        (vimEditor as MutableVimEditor).insertText(caret, lineStartOffset + startDiff, lineText)
      }
    }
  }
}
