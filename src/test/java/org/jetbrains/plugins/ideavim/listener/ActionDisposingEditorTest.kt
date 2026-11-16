/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.listener

import com.intellij.idea.TestFor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.rd.ui.actions.PossiblyDelegatingToBackendAction
import com.maddyhome.idea.vim.helper.vimInitialised
import com.maddyhome.idea.vim.listener.IdeaSpecifics
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * An IDE action can dispose the editor it runs in, e.g. by closing its tab. IdeaVim normalizes the caret after every
 * IDE action, and must not touch the editor once it is disposed (VIM-4378).
 *
 * The editor is disposed synchronously, while IdeaVim deinitializes it later, so [vimInitialised] is still true when the
 * action ends.
 */
class ActionDisposingEditorTest : VimTestCase() {
  @Test
  @TestFor(issues = ["VIM-4378"])
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `action that disposes the editor`() {
    configureByText("Lorem ipsum$c\ndolor sit amet")

    performActionsDisposingEditor(object : AnAction() {
      override fun actionPerformed(e: AnActionEvent) {}
    })
  }

  @Test
  @TestFor(issues = ["VIM-4378"])
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `backend delegating action that disposes the editor`() {
    // A disposed editor reports the caret at offset 0, so the action looks as if it is still waiting for the backend
    configureByText("${c}Lorem ipsum\ndolor sit amet")

    performActionsDisposingEditor(FakeBackendDelegatingAction())
  }

  /**
   * Runs [action] in a second editor, which is disposed while the action runs, and then an action without an editor,
   * e.g. one from a tool window
   */
  private fun performActionsDisposingEditor(action: AnAction) {
    ApplicationManager.getApplication().invokeAndWait {
      val editorFactory = EditorFactory.getInstance()
      val editor = editorFactory.createEditor(fixture.editor.document, fixture.project)
      editor.caretModel.moveToOffset(fixture.editor.caretModel.offset)
      assertTrue(editor.vimInitialised)

      val listener = IdeaSpecifics.VimActionListener()
      listener.beforeActionPerformed(action, event(action, editorContext(editor)))
      editorFactory.releaseEditor(editor)
      assertTrue(editor.isDisposed)
      assertTrue(editor.vimInitialised)
      listener.afterActionPerformed(action, event(action, editorContext(editor)), AnActionResult.PERFORMED)

      val actionWithoutEditor = object : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {}
      }
      val projectContext = SimpleDataContext.getProjectContext(fixture.project)
      listener.beforeActionPerformed(actionWithoutEditor, event(actionWithoutEditor, projectContext))
      listener.afterActionPerformed(
        actionWithoutEditor,
        event(actionWithoutEditor, projectContext),
        AnActionResult.PERFORMED,
      )
    }
  }

  private fun editorContext(editor: Editor): DataContext = SimpleDataContext.builder()
    .add(CommonDataKeys.PROJECT, fixture.project)
    .add(CommonDataKeys.HOST_EDITOR, editor)
    .add(CommonDataKeys.EDITOR, editor)
    .build()

  private fun event(action: AnAction, context: DataContext): AnActionEvent =
    TestActionEvent.createTestEvent(action, context)

  private class FakeBackendDelegatingAction : AnAction(), PossiblyDelegatingToBackendAction {
    override fun beforeDelegatingToBackend() {}
    override fun actionPerformed(e: AnActionEvent) {}
  }
}
