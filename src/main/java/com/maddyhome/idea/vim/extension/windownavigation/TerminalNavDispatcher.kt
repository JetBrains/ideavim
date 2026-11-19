/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.windownavigation

import com.intellij.ide.DataManager
import com.intellij.ide.IdeEventQueue
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.WriteIntentReadAction
import com.intellij.openapi.components.service
import java.awt.AWTEvent
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

/**
 * Passes the keys of a `<C-W>` sequence in the terminal tool window to [ToolWindowNavDispatcher].
 *
 * With "Override IDE shortcuts" on, the terminal sends every key that isn't a shortcut of an action on its own
 * allowlist to the shell, from an [IdeEventQueue] dispatcher, before the action system sees it. The keymap can't be
 * used either: for the keys on that allowlist, such as the arrows or Enter, the terminal's own action wins. So this
 * dispatcher runs the navigation action itself and keeps the keys from the terminal. It has to run before the
 * terminal's dispatcher: newer terminals add theirs to the same list each time they get the focus, so this one is added
 * when the extension starts.
 */
internal class TerminalNavDispatcher(
  private val isInTerminal: (Component) -> Boolean = ::isInTerminalToolWindow,
) : IdeEventQueue.NonLockedEventDispatcher {
  private var inSequence = false

  // A handled key press is followed by its key typed event, which must not reach the shell either
  private var swallowNextKeyTyped = false

  override fun dispatch(e: AWTEvent): Boolean {
    if (e !is KeyEvent) return false
    return when (e.id) {
      KeyEvent.KEY_PRESSED -> keyPressed(e)
      KeyEvent.KEY_TYPED -> keyTyped(e)
      else -> false
    }
  }

  private fun keyPressed(e: KeyEvent): Boolean {
    if (!isModifierKey(e.keyCode)) swallowNextKeyTyped = false
    if (!inSequence && !isCtrlW(e)) return false

    val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
    if (focusOwner == null || !isInTerminal(focusOwner)) {
      cancelSequence()
      return false
    }
    if (inSequence && isModifierKey(e.keyCode)) return true

    swallowNextKeyTyped = true
    when {
      !inSequence -> {
        // A sequence started in another tool window may have been left unfinished
        service<ToolWindowNavDispatcher>().cancelSequence()
        inSequence = true
        perform(e, focusOwner)
      }
      e.keyCode == KeyEvent.VK_ESCAPE -> cancelSequence()
      // Keys such as `k` are mapped by their character, which comes with the key typed event
      producesKeyTyped(e) -> swallowNextKeyTyped = false
      else -> perform(e, focusOwner)
    }
    return true
  }

  private fun keyTyped(e: KeyEvent): Boolean {
    if (swallowNextKeyTyped) {
      swallowNextKeyTyped = false
      return true
    }
    if (!inSequence) return false
    val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
    if (focusOwner == null || !isInTerminal(focusOwner)) {
      cancelSequence()
      return false
    }
    perform(e, focusOwner)
    return true
  }

  private fun perform(e: KeyEvent, component: Component) {
    val navDispatcher = service<ToolWindowNavDispatcher>()
    val dataContext = DataManager.getInstance().getDataContext(component)
    val event = AnActionEvent.createEvent(dataContext, null, ActionPlaces.KEYBOARD_SHORTCUT, ActionUiKind.NONE, e)
    try {
      WriteIntentReadAction.run { ActionUtil.performAction(navDispatcher, event) }
    } finally {
      inSequence = navDispatcher.isInSequence
    }
  }

  private fun cancelSequence() {
    if (!inSequence) return
    inSequence = false
    service<ToolWindowNavDispatcher>().cancelSequence()
  }

  private fun isCtrlW(e: KeyEvent) = e.keyCode == KeyEvent.VK_W && e.modifiersEx == InputEvent.CTRL_DOWN_MASK

  private fun isModifierKey(keyCode: Int) = keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL ||
    keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_META || keyCode == KeyEvent.VK_ALT_GRAPH

  private fun producesKeyTyped(e: KeyEvent) = !e.isActionKey &&
    e.modifiersEx and (InputEvent.CTRL_DOWN_MASK or InputEvent.ALT_DOWN_MASK or InputEvent.META_DOWN_MASK) == 0
}

private const val TERMINAL_TOOL_WINDOW_ID = "Terminal"

private fun isInTerminalToolWindow(component: Component): Boolean {
  val project = ProjectUtil.getProjectForComponent(component) ?: return false
  return findDockedToolWindow(component, project)?.id == TERMINAL_TOOL_WINDOW_ID
}
