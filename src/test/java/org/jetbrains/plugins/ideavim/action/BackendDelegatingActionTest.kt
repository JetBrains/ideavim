/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.idea.TestFor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.rd.ui.actions.PossiblyDelegatingToBackendAction
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * In Rider, some actions (e.g. `CommentByLineComment` for C#) are executed by the backend: the frontend action only
 * submits a request and returns, and the backend's changes are applied later.
 */
class BackendDelegatingActionTest : VimTestCase() {
  @Test
  @TestFor(issues = ["VIM-4336"])
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `caret is not moved while backend action is pending`() {
    // The previous comment action left the caret on the line break
    configureByText(
      """
      // public sealed class ExternalPaymentResult
      {$c
      }
      """.trimIndent(),
    )
    val offsetBeforeAction = ApplicationManager.getApplication().runReadAction<Int> { fixture.editor.caretModel.offset }
    var offsetWhenBackendApplied = -1

    performActionAppliedLaterByBackend { editor ->
      offsetWhenBackendApplied = editor.caretModel.offset
      commentLineAndMoveToEndOfNextLine(editor, line = 1)
    }

    assertEquals(offsetBeforeAction, offsetWhenBackendApplied)
    assertState(
      """
      // public sealed class ExternalPaymentResult
      // {
      $c}
      """.trimIndent(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-4336"])
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `caret is normalized after backend action is applied`() {
    configureByText(
      """
      public sealed class ${c}ExternalPaymentResult
      {
      }
      """.trimIndent(),
    )

    performActionAppliedLaterByBackend { editor -> commentLineAndMoveToEndOfNextLine(editor, line = 0) }

    assertState(
      """
      // public sealed class ExternalPaymentResult
      $c{
      }
      """.trimIndent(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-4336"])
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `caret is normalized after backend delegating action is applied immediately`() {
    configureByText(
      """
      public sealed class ${c}ExternalPaymentResult
      {
      }
      """.trimIndent(),
    )

    performActionAppliedImmediately { editor -> commentLineAndMoveToEndOfNextLine(editor, line = 0) }

    assertState(
      """
      // public sealed class ExternalPaymentResult
      $c{
      }
      """.trimIndent(),
    )
  }

  /** Like `CommentByLineComment`, which keeps the caret column, clamped to the end of the shorter next line */
  private fun commentLineAndMoveToEndOfNextLine(editor: Editor, line: Int) {
    editor.document.insertString(editor.document.getLineStartOffset(line), "// ")
    editor.caretModel.moveToOffset(editor.document.getLineEndOffset(line + 1))
  }

  /** Like a backend response */
  private fun performActionAppliedLaterByBackend(change: (Editor) -> Unit) =
    performBackendDelegatingAction(change) { applyChange -> ApplicationManager.getApplication().invokeLater(applyChange) }

  /** Like Rider's lightweight backend or a speculative frontend execution */
  private fun performActionAppliedImmediately(change: (Editor) -> Unit) =
    performBackendDelegatingAction(change) { applyChange -> applyChange.run() }

  private fun performBackendDelegatingAction(change: (Editor) -> Unit, schedule: (Runnable) -> Unit) {
    val actionManager = ActionManager.getInstance()
    actionManager.registerAction(ACTION_ID, FakeBackendDelegatingAction(change, schedule))
    try {
      ApplicationManager.getApplication().invokeAndWait {
        fixture.performEditorAction(ACTION_ID)
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
      }
    } finally {
      actionManager.unregisterAction(ACTION_ID)
    }
  }

  private class FakeBackendDelegatingAction(
    private val change: (Editor) -> Unit,
    private val schedule: (Runnable) -> Unit,
  ) : AnAction(), PossiblyDelegatingToBackendAction {
    override fun beforeDelegatingToBackend() {}

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: return
      schedule { WriteCommandAction.runWriteCommandAction(e.project) { change(editor) } }
    }
  }

  private companion object {
    const val ACTION_ID = "IdeaVimTestFakeBackendDelegatingAction"
  }
}
