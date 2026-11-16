/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.Disposer
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.group.visual.moveCaretOneCharLeftFromSelectionEnd
import com.maddyhome.idea.vim.helper.getTopLevelEditor
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.helper.isPossiblyDelegatingToBackend
import com.maddyhome.idea.vim.ide.isRider
import com.maddyhome.idea.vim.newapi.vim

internal class RiderActionListener : AnActionListener {

  private var editor: Editor? = null
  private val extendSelectionCaretFix = ExtendSelectionCaretFix()

  override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
    if (VimPlugin.isNotEnabled()) return

    event.dataContext.getData(CommonDataKeys.HOST_EDITOR)?.let { editor = it }
    if (action is EditorAction) {
      editor?.putUserData(ActionPlaces.EXECUTE_EDITOR_ACTION_ON_FRONTEND, shouldExecuteOnFrontend(action))
    }
  }

  override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
    if (VimPlugin.isNotEnabled()) return

    val editor = editor ?: return
    if (isExtendSelectionAction(action)) {
      extendSelectionCaretFix.moveCaretIntoSelectionAfterBackendUpdate(editor)
    }
  }

  /** Enter outside Insert mode is executed on the frontend (RIDER-123506) */
  private fun shouldExecuteOnFrontend(action: EditorAction): Boolean =
    action is EnterAction && editor?.vim?.insertMode == false

  private fun isExtendSelectionAction(action: AnAction): Boolean =
    ActionManager.getInstance().getId(action) in EXTEND_SELECTION_ACTIONS

  private companion object {
    val EXTEND_SELECTION_ACTIONS = setOf(
      IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET,
      IdeActions.ACTION_EDITOR_UNSELECT_WORD_AT_CARET,
    )
  }
}

/**
 * Rider's backend leaves the caret after the selection it extends or shrinks, while in Visual mode the caret has to be
 * on the last selected character. This waits for the backend's caret move, then moves the caret back.
 *
 * The move is postponed, as [IdeaSpecifics] does for caret normalization (VIM-4336). The backend's caret and selection
 * are applied together, and a caret move made while they're being applied isn't sent to the backend. The backend would
 * then extend a different selection than the editor shows, and the selection would stop growing.
 */
private class ExtendSelectionCaretFix {
  private var pendingFix: Disposable? = null

  fun moveCaretIntoSelectionAfterBackendUpdate(editor: Editor) {
    cancelPendingFix()
    val disposable = Disposer.newDisposable("IdeaVim caret fix after Rider's extend selection")
    EditorUtil.disposeWithEditor(editor, disposable)
    pendingFix = disposable
    editor.caretModel.addCaretListener(object : CaretListener {
      override fun caretPositionChanged(event: CaretEvent) {
        cancelPendingFix()
        moveCaretIntoSelectionLater(event.editor.getTopLevelEditor())
      }
    }, disposable)
  }

  private fun moveCaretIntoSelectionLater(editor: Editor) {
    ApplicationManager.getApplication().invokeLater({ moveCaretIntoSelection(editor) }, { editor.isDisposed })
  }

  private fun moveCaretIntoSelection(editor: Editor) {
    if (VimPlugin.isNotEnabled() || editor.isIdeaVimDisabledHere) return
    val predictedMode = injector.application.runReadAction {
      IdeaSelectionControl.predictMode(editor, VimListenerManager.SelectionSource.OTHER)
    }
    moveCaretOneCharLeftFromSelectionEnd(editor, predictedMode)
  }

  private fun cancelPendingFix() {
    pendingFix?.let { Disposer.dispose(it) }
    pendingFix = null
  }
}

/**
 * Tag jumps that ReSharper navigates by itself, so the tag stack entry waits for its navigation
 *
 * For a mouse click or a hotkey, Rider hides its backend actions behind one composite action, and the backend picks
 * which of them to run. Only the shortcut tells that Go to Declaration is among them. And when Go to Declaration does
 * show up, the backend may run it once more on the frontend for the same jump. Pushing before the action runs would
 * miss the first case and push twice in the second.
 *
 * Instead, the action only marks a tag jump as expected. ReSharper navigates in a command of its own, and when it
 * starts, the caret is still where the jump starts from. That's when the position goes onto the tag stack, once per
 * navigation, and only for a navigation that happens.
 */
internal object RiderTagJumps {
  /** The command Rider wraps ReSharper's navigation into */
  private const val NAVIGATION_COMMAND = "NavigationHost.SendNavigation"

  /** Long enough for the backend to answer, short enough not to mistake a later, unrelated navigation for the jump */
  private const val EXPECTED_JUMP_TIMEOUT_MS = 5_000L

  private var expectedJumpEditor: Editor? = null
  private var expectedJumpTimestamp = 0L

  /** True for an action whose navigation happens on the backend, see [RiderTagJumps] */
  fun isNavigatedByBackend(action: AnAction): Boolean = isRider() && action.isPossiblyDelegatingToBackend()

  /** Marks a tag jump from [editor] as expected, the push waits for the backend's navigation */
  fun expectJump(editor: Editor) {
    expectedJumpEditor = editor
    expectedJumpTimestamp = System.currentTimeMillis()
  }

  fun commandStarted(commandName: String?) {
    if (commandName != NAVIGATION_COMMAND) return
    val editor = expectedJumpEditor ?: return
    val isExpected = System.currentTimeMillis() - expectedJumpTimestamp < EXPECTED_JUMP_TIMEOUT_MS
    expectedJumpEditor = null
    if (isExpected) {
      pushTagLocation(editor)
    }
  }
}

internal class RiderNavigationCommandListener : CommandListener {
  override fun commandStarted(event: CommandEvent) {
    if (VimPlugin.isNotEnabled()) return
    RiderTagJumps.commandStarted(event.commandName)
  }
}
