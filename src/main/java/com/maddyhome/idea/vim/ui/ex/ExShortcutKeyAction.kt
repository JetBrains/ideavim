/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.project.DumbAwareAction
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * An IntelliJ action to forward shortcuts to the ex entry component
 * <p>
 * Key events are processed by the IDE action system before they are dispatched to the actual component, which means
 * they take precedence over the keyboard shortcuts registered with the ex component as Swing actions. This can cause
 * clashes such as <C-R> invoking the Run action instead of the Paste Register ex action, or <BS> being handled by the
 * editor rather than allowing us to cancel the ex entry.
 * <p>
 * This class is an IDE action that is registered with the ex entry component, so is available only when the ex entry
 * component has focus. It registers all shortcuts used by the Swing actions and forwards them directly to the key
 * handler.
 */
@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal class ExShortcutKeyAction(private val exEntryPanel: ExEntryPanel) : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val keyStroke = getKeyStroke(e)
    if (keyStroke != null) {
      val editor = exEntryPanel.entry.editor
      val keyHandler = KeyHandler.getInstance()

      // About the context: we use the context of the main editor to execute actions on it.
      //   e.dataContext will refer to the ex-entry editor and commands will be executed on it,
      //   thus it should not be used. For example, `:action EditorSelectWord` will not work with this context
      val mainEditorContext = exEntryPanel.entry.context.vim
      keyHandler.handleKey(editor!!.vim, keyStroke, mainEditorContext, keyHandler.keyHandlerState)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = exEntryPanel.isActive
  }

  private fun getKeyStroke(e: AnActionEvent): KeyStroke? {
    val inputEvent = e.inputEvent
    if (inputEvent is KeyEvent) {
      return KeyStroke.getKeyStrokeForEvent(inputEvent)
    }
    return null
  }

  fun registerCustomShortcutSet() {
    val shortcuts = listOf(
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.SHIFT_DOWN_MASK),
    )
      .map { KeyboardShortcut(it, null) }
      .toTypedArray()

    registerCustomShortcutSet({ shortcuts }, exEntryPanel)
  }

  // / Or EDT? We access ExEntryPanel actually. But seems to work with BGT
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
