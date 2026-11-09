/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.change

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

/**
 * Splits the changes made on the frontend into undo steps, one per IdeaVim command (VIM-4370).
 *
 * When the undo stack is on the backend (e.g. Rider), the backend applies the frontend's changes as undo-transparent
 * document patches. The platform merges such changes into one undo step, and undoes all transparent steps on top of
 * the stack at once. So without this, one `u` undoes every change made on the frontend since the backend's own last
 * command.
 *
 * It's a workaround that relies on how the platform merges undo steps. Remove it once RIDER-145069 is fixed.
 */
internal object FrontendUndoSteps {
  /** Ends the current undo step: the changes made so far are undone together, and the next ones separately */
  fun close(editor: Editor) {
    val project = editor.project ?: return
    val document = editor.document
    FrontendChanges.startTracking(project)
    if (FrontendChanges.areTheOnlyChangesIn(document)) {
      makeStepNonTransparent(project, document)
    }
    FrontendChanges.forget(document)
    endStep(project, editor)
  }

  /** A non-transparent action merged into the step makes the whole step a normal one, undone on its own */
  private fun makeStepNonTransparent(project: Project, document: Document) {
    CommandProcessor.getInstance().executeCommand(project, {
      UndoManager.getInstance(project).undoableActionPerformed(NoOpUndoableAction(document))
    }, null, null, document)
  }

  /** Getting the next undo step ends the current one first. The platform has no other public API for that */
  private fun endStep(project: Project, editor: Editor) {
    UndoManager.getInstance(project).getNextUndoNanoTime(textEditorOf(editor))
  }
}

private class NoOpUndoableAction(document: Document) : BasicUndoableAction(document) {
  override fun undo() {}
  override fun redo() {}
}

/**
 * Remembers whether a document was changed only by the frontend since the last [FrontendUndoSteps.close].
 *
 * The backend applies the frontend's changes in undo-transparent actions, and makes its own changes, e.g. typing or
 * undo, in normal commands. A step with a normal change in it is already non-transparent.
 */
private object FrontendChanges : DocumentListener {
  private val ONLY_FRONTEND_CHANGES = Key.create<Boolean>("ideavim.undo.only.frontend.changes")
  private val TRACKING = Key.create<Boolean>("ideavim.undo.tracking.frontend.changes")

  fun startTracking(project: Project) {
    if (project.getUserData(TRACKING) == true) return
    project.putUserData(TRACKING, true)
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(this, project)
  }

  fun areTheOnlyChangesIn(document: Document): Boolean = document.getUserData(ONLY_FRONTEND_CHANGES) == true

  fun forget(document: Document) = document.putUserData(ONLY_FRONTEND_CHANGES, null)

  override fun documentChanged(event: DocumentEvent) {
    val document = event.document
    val isFrontendChange = CommandProcessor.getInstance().isUndoTransparentActionInProgress
    if (!isFrontendChange) {
      document.putUserData(ONLY_FRONTEND_CHANGES, false)
    } else if (document.getUserData(ONLY_FRONTEND_CHANGES) == null) {
      document.putUserData(ONLY_FRONTEND_CHANGES, true)
    }
  }
}
