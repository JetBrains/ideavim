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
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.util.registry.Registry
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ChangesListener
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

      performUndo(editor, undoManager, fileEditor)

      scrollingModel.flushViewportChanges()

      return true
    }
    return false
  }

  private fun performUndo(
    editor: VimEditor,
    undoManager: UndoManager,
    fileEditor: TextEditor,
  ) {
    if (injector.globalIjOptions().oldundo) {
      // TODO refactor me after VIM-308 when restoring selection and caret movement will be ignored by undo
      editor.runWithChangeTracking {
        undoManager.undo(fileEditor)

        // We execute undo one more time if the previous one just restored selection
        if (!hasChanges && hasSelection(editor) && undoManager.isUndoAvailable(fileEditor)) {
          undoManager.undo(fileEditor)
        }
      }

      CommandProcessor.getInstance().runUndoTransparentAction {
        removeSelections(editor)
      }
    } else {
      runWithBooleanRegistryOption("ide.undo.transparent.caret.movement", true) {
        undoManager.undo(fileEditor)
      }

      CommandProcessor.getInstance().runUndoTransparentAction {
        removeSelections(editor)
      }
    }
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
      performRedo(undoManager, fileEditor, editor)

      return true
    }
    return false
  }

  override fun startInsertSequence(caret: VimCaret, startOffset: Int, startNanoTime: Long) {
    (caret as IjVimCaret).startInsertSequence(startOffset, startNanoTime)
  }

  override fun endInsertSequence(caret: VimCaret, endOffset: Int, endNanoTime: Long) {
    (caret as IjVimCaret).endInsertSequence(endOffset, endNanoTime)
  }

  override fun getInsertSequence(caret: VimCaret, nanoTime: Long) {
    (caret as IjVimCaret).getInsertSequenceForTime(nanoTime)
  }

  private fun performRedo(
    undoManager: UndoManager,
    fileEditor: TextEditor,
    editor: VimEditor,
  ) {
    if (injector.globalIjOptions().oldundo) {
      undoManager.redo(fileEditor)
      CommandProcessor.getInstance().runUndoTransparentAction {
        editor.carets().forEach { it.ij.removeSelection() }
      }
      // TODO refactor me after VIM-308 when restoring selection and caret movement will be ignored by undo
      editor.runWithChangeTracking {
        undoManager.redo(fileEditor)

        // We execute undo one more time if the previous one just restored selection
        if (!hasChanges && hasSelection(editor) && undoManager.isRedoAvailable(fileEditor)) {
          undoManager.redo(fileEditor)
        }
      }

      CommandProcessor.getInstance().runUndoTransparentAction {
        removeSelections(editor)
      }
    } else {
      runWithBooleanRegistryOption("ide.undo.transparent.caret.movement", true) {
        undoManager.redo(fileEditor)
      }

      CommandProcessor.getInstance().runUndoTransparentAction {
        removeSelections(editor)
      }
    }
  }

  private fun removeSelections(editor: VimEditor) {
    editor.carets().forEach {
      val ijCaret = it.ij
      if (!ijCaret.hasSelection()) return@forEach

      val selectionStart = ijCaret.selectionStart
      ijCaret.removeSelection()
      ijCaret.moveToOffset(selectionStart)
    }
  }

  private fun runWithBooleanRegistryOption(option: String, value: Boolean, block: () -> Unit) {
    val registry = Registry.get(option)
    val oldValue = registry.asBoolean()
    registry.setValue(value)
    try {
      block()
    } finally {
      registry.setValue(oldValue)
    }
  }

  private fun VimEditor.runWithChangeTracking(block: ChangeTracker.() -> Unit) {
    val tracker = ChangeTracker(this)
    tracker.block()
  }

  private class ChangeTracker(private val editor: VimEditor) {
    private val initialPath = editor.getPath()
    private val changeListener = object : ChangesListener {
      var hasChanged = false

      override fun documentChanged(change: ChangesListener.Change) {
        hasChanged = true
      }
    }

    init {
      editor.document.addChangeListener(changeListener)
    }

    val hasChanges: Boolean
      get() = changeListener.hasChanged || initialPath != editor.getPath()
  }
}
