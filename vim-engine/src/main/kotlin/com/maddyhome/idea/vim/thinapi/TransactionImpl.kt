/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Color
import com.intellij.vim.api.Highlighter
import com.intellij.vim.api.TextInfo
import com.intellij.vim.api.TextSelectionType
import com.intellij.vim.api.scopes.Transaction
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

class TransactionImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : Transaction {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

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

  override fun addCaret(offset: Int): CaretId {
    TODO("Not yet implemented")
  }

  override fun removeCaret(caretId: CaretId) {
    TODO("Not yet implemented")
  }

  override fun addHighlighter(
    startOffset: Int,
    endOffset: Int,
    backgroundColor: Color?,
    foregroundColor: Color?,
  ): Highlighter {
    return injector.highlightingService.addHighlighter(
      vimEditor,
      startOffset,
      endOffset,
      backgroundColor,
      foregroundColor
    )
  }

  override fun removeHighlighter(highlighter: Highlighter) {
    injector.highlightingService.removeHighlighter(vimEditor, highlighter)
  }

  override fun removeHighlighters(highlighters: List<Highlighter>) {
    highlighters.forEach { highlighter ->
      injector.highlightingService.removeHighlighter(vimEditor, highlighter)
    }
  }

  private fun putText(
    caretId: CaretId,
    startPosition: Int,
    text: String,
    caretAfterInsertedText: Boolean,
    preserveIndentation: Boolean,
  ): Boolean {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return false

    val copiedText = injector.clipboardManager.dumbCopiedText(text)
    val textData = PutData.TextData(null, copiedText, SelectionType.CHARACTER_WISE)

    val putData = PutData(
      textData = textData,
      visualSelection = null,
      count = 1,
      insertTextBeforeCaret = caretAfterInsertedText,
      rawIndent = preserveIndentation,
      caretAfterInsertedText = caretAfterInsertedText,
      putToLine = -1
    )

    var result = false
    injector.actionExecutor.executeCommand(vimEditor, {
      injector.application.runWriteAction {
        // Move caret to insertion position
        caret.moveToOffset(startPosition)

        result = injector.put.putTextForCaret(
          vimEditor,
          caret,
          executionContext,
          putData,
          updateVisualMarks = false,
          modifyRegister = false
        )
      }
    }, "Insert Text", null)

    return result
  }

  override fun insertText(
    caretId: CaretId,
    position: Int,
    text: String,
    caretAfterInsertedText: Boolean,
    preserveIndentation: Boolean,
  ): Boolean {
    return putText(caretId, position, text, caretAfterInsertedText, preserveIndentation)
  }

  override fun replaceText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    textInfo: TextInfo,
    selectionType: TextSelectionType,
    preserveIndentation: Boolean,
  ): Boolean {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return false

    val copiedText = injector.clipboardManager.dumbCopiedText(textInfo.text)
    val textData = PutData.TextData(null, copiedText, textInfo.type.toSelectionType())

    val emptySelection = startOffset == endOffset

    val visualSelection = PutData.VisualSelection(
      mapOf(
        caret to VimSelection.create(
          startOffset,
          if (emptySelection) endOffset else endOffset - 1,
          selectionType.toSelectionType(),
          vimEditor
        )
      ),
      selectionType.toSelectionType()
    )

    val putData = PutData(
      textData = textData,
      visualSelection = visualSelection,
      count = 1,
      insertTextBeforeCaret = true, // Always insert before caret for replace
      rawIndent = preserveIndentation,
      caretAfterInsertedText = false,
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
          updateVisualMarks = false,
          modifyRegister = false
        )
      }
    }, "Replace Text", null)

    return result
  }

  override fun deleteText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
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
          isChange = false,
          saveToRegister = false
        )
      }
    }, "Delete Text", null)

    return true
  }
}
