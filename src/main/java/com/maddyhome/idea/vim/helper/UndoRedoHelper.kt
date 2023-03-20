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
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.undo.UndoRedoBase

/**
 * @author oleg
 */
@Service
internal class UndoRedoHelper : UndoRedoBase() {
  init {
    injector.optionGroup.addListener(IjOptions.oldundo, {
      UndoManagerImpl.ourNeverAskUser = !injector.globalOptions().isSet(IjOptions.oldundo)
    }, true)
  }

  override fun undo(editor: VimEditor, context: ExecutionContext): Boolean {
    val ijContext = context.context as DataContext
    val project = PlatformDataKeys.PROJECT.getData(ijContext) ?: return false
    val fileEditor = TextEditorProvider.getInstance().getTextEditor(editor.ij)
    val undoManager = UndoManager.getInstance(project)
    if (undoManager.isUndoAvailable(fileEditor)) {
      val scrollingModel = editor.getScrollingModel()
      scrollingModel.accumulateViewportChanges()

      if (injector.globalOptions().isSet(IjOptions.oldundo)) {
        SelectionVimListenerSuppressor.lock().use { undoManager.undo(fileEditor) }
      } else {
        performUntilFileChanges(editor, { undoManager.isUndoAvailable(fileEditor) }, { undoManager.undo(fileEditor) })

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

  override fun redo(editor: VimEditor, context: ExecutionContext): Boolean {
    val ijContext = context.context as DataContext
    val project = PlatformDataKeys.PROJECT.getData(ijContext) ?: return false
    val fileEditor = TextEditorProvider.getInstance().getTextEditor(editor.ij)
    val undoManager = UndoManager.getInstance(project)
    if (undoManager.isRedoAvailable(fileEditor)) {
      if (injector.globalOptions().isSet(IjOptions.oldundo)) {
        SelectionVimListenerSuppressor.lock().use { undoManager.redo(fileEditor) }
      } else {
        performUntilFileChanges(editor, { undoManager.isRedoAvailable(fileEditor) }, { undoManager.redo(fileEditor) })
        CommandProcessor.getInstance().runUndoTransparentAction {
          editor.carets().forEach { it.ij.removeSelection() }
        }
      }
      return true
    }
    return false
  }

  private fun performUntilFileChanges(editor: VimEditor?, check: () -> Boolean, action: Runnable) {
    if (editor == null) return
    val vimDocument = editor.document

    val changeListener = object : ChangesListener {
      var hasChanged = false

      override fun documentChanged(change: ChangesListener.Change) {
        hasChanged = true
      }
    }

    val oldPath = editor.getPath()
    vimDocument.addChangeListener(changeListener)
    while (check() && !changeListener.hasChanged && !ifFilePathChanged(editor, oldPath)) {
      action.run()
    }
    vimDocument.removeChangeListener(changeListener)
  }

  private fun ifFilePathChanged(editor: VimEditor, oldPath: String?): Boolean {
    return editor.getPath() != oldPath
  }
}
