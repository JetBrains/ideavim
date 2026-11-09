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
import com.maddyhome.idea.vim.group.onEdt

/**
 * RPC handler for [ChangeRemoteApi].
 *
 * Registers StartMarkAction/FinishMarkAction on the backend's UndoManager
 * to group multiple document changes into a single undo step, splits the frontend's changes into undo steps,
 * and runs undo/redo.
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

  override suspend fun closeUndoStep(editorId: EditorId) = onEdt {
    editorId.findEditorOrNull()?.let { FrontendUndoSteps.close(it) }
    Unit
  }

  override suspend fun undo(editorId: EditorId): Boolean? = onEdt {
    val target = findUndoTarget(editorId) ?: return@onEdt null
    if (!target.undoManager.isUndoAvailable(target.textEditor)) return@onEdt false
    withTransparentCaretMovement { target.undoManager.undo(target.textEditor) }
    true
  }

  override suspend fun redo(editorId: EditorId): Boolean? = onEdt {
    val target = findUndoTarget(editorId) ?: return@onEdt null
    if (!target.undoManager.isRedoAvailable(target.textEditor)) return@onEdt false
    withTransparentCaretMovement { target.undoManager.redo(target.textEditor) }
    true
  }

  private class UndoTarget(val undoManager: UndoManager, val textEditor: TextEditor)

  private fun findUndoTarget(editorId: EditorId): UndoTarget? {
    val editor = editorId.findEditorOrNull() ?: return null
    val project = editor.project ?: return null
    return UndoTarget(UndoManager.getInstance(project), textEditorOf(editor))
  }
}

/** The TextEditor with a preview, if the editor is hosted in one, see VIM-3671 */
internal fun textEditorOf(editor: Editor): TextEditor {
  val currentTextEditor = TextEditorProvider.getInstance().getTextEditor(editor)
  return TextEditorWithPreview.getParentSplitEditor(currentTextEditor) as? TextEditor ?: currentTextEditor
}
