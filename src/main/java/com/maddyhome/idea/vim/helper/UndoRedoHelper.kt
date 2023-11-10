/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.undo.UndoRedoBase

/**
 * @author oleg
 */
@Service
internal class UndoRedoHelper : UndoRedoBase() {
  override fun undo(editor: VimEditor, context: ExecutionContext): Boolean {
    val ijContext = context.context as DataContext
    val project = PlatformDataKeys.PROJECT.getData(ijContext) ?: return false
    val fileEditor = TextEditorProvider.getInstance().getTextEditor(editor.ij)
    val undoManager = UndoManager.getInstance(project)
    if (undoManager.isUndoAvailable(fileEditor)) {
      val scrollingModel = editor.getScrollingModel()
      scrollingModel.accumulateViewportChanges()

      if (injector.globalIjOptions().oldundo) {
        SelectionVimListenerSuppressor.lock().use { undoManager.undo(fileEditor) }
      } else {
        // TODO refactor me after VIM-308 when restoring selection and caret movement will be ignored by undo
        undoManager.undo(fileEditor)
        if (hasSelection(editor) && undoManager.isUndoAvailable(fileEditor)) {
          undoManager.undo(fileEditor) // execute one more time if the previous undo just restored selection
        }
        
        // remove selection
        editor.carets().forEach {
          val ijCaret = it.ij
          val hasSelection = ijCaret.hasSelection()
          if (hasSelection) {
            val selectionStart = ijCaret.selectionStart
            CommandProcessor.getInstance().runUndoTransparentAction {
              it.ij.removeSelection()
              it.ij.moveToOffset(selectionStart)
            }
          }
        }
      }

      scrollingModel.flushViewportChanges()

      return true
    }
    return false
  }

  private fun hasSelection(editor: VimEditor): Boolean {
    return editor.primaryCaret().ij.hasSelection()
  }
  
  override fun redo(editor: VimEditor, context: ExecutionContext): Boolean {
    val ijContext = context.context as DataContext
    val project = PlatformDataKeys.PROJECT.getData(ijContext) ?: return false
    val fileEditor = TextEditorProvider.getInstance().getTextEditor(editor.ij)
    val undoManager = UndoManager.getInstance(project)
    if (undoManager.isRedoAvailable(fileEditor)) {
      if (injector.globalIjOptions().oldundo) {
        SelectionVimListenerSuppressor.lock().use { undoManager.redo(fileEditor) }
      } else {
        undoManager.redo(fileEditor)
        CommandProcessor.getInstance().runUndoTransparentAction {
          editor.carets().forEach { it.ij.removeSelection() }
        }
      }
      return true
    }
    return false
  }
}
