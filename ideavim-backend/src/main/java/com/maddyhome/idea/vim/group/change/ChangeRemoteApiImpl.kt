/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.change

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.impl.FinishMarkAction
import com.intellij.openapi.command.impl.StartMarkAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.openapi.editor.impl.findEditorOrNull
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.util.registry.Registry
import com.maddyhome.idea.vim.group.onEdt

/**
 * RPC handler for [ChangeRemoteApi].
 *
 * Registers StartMarkAction/FinishMarkAction on the backend's UndoManager
 * to group multiple document changes into a single undo step, and runs undo/redo on it.
 */
internal class ChangeRemoteApiImpl : ChangeRemoteApi {
  private var currentStartMark: StartMarkAction? = null

  override suspend fun startUndoMark(editorId: EditorId, commandName: String) = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt
    val project = editor.project ?: return@onEdt
    CommandProcessor.getInstance().executeCommand(project, {
      currentStartMark = try {
        StartMarkAction.start(editor, project, commandName)
      } catch (_: StartMarkAction.AlreadyStartedException) {
        null
      }
    }, commandName, null)
  }

  override suspend fun finishUndoMark(editorId: EditorId) = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt
    val project = editor.project ?: return@onEdt
    val mark = currentStartMark
    currentStartMark = null
    CommandProcessor.getInstance().executeCommand(project, {
      if (mark != null) {
        FinishMarkAction.finish(project, editor, mark)
      }
    }, "Finish Undo Mark", null)
  }

  override suspend fun undo(editorId: EditorId): Boolean = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt false
    val project = editor.project ?: return@onEdt false
    val textEditor = getTextEditor(editor)
    val undoManager = UndoManager.getInstance(project)
    if (!undoManager.isUndoAvailable(textEditor)) return@onEdt false
    withTransparentCaretMovement { undoManager.undo(textEditor) }
    true
  }

  override suspend fun redo(editorId: EditorId): Boolean = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt false
    val project = editor.project ?: return@onEdt false
    val textEditor = getTextEditor(editor)
    val undoManager = UndoManager.getInstance(project)
    if (!undoManager.isRedoAvailable(textEditor)) return@onEdt false
    withTransparentCaretMovement { undoManager.redo(textEditor) }
    true
  }

  /** The TextEditor with a preview, if the editor is hosted in one, see VIM-3671 */
  private fun getTextEditor(editor: Editor): TextEditor {
    val currentTextEditor = TextEditorProvider.getInstance().getTextEditor(editor)
    return TextEditorWithPreview.getParentSplitEditor(currentTextEditor) as? TextEditor ?: currentTextEditor
  }

  /**
   * VIM-4370. When the caret moved after a change, e.g. back by one on leaving Insert mode, the platform spends the
   * first undo only on restoring the caret, so `u` seems to do nothing
   */
  private inline fun withTransparentCaretMovement(block: () -> Unit) {
    val registry = Registry.get("ide.undo.transparent.caret.movement")
    val oldValue = registry.asBoolean()
    registry.setValue(true)
    try {
      block()
    } finally {
      registry.setValue(oldValue)
    }
  }
}
