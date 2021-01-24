/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.helper.EditorDataContext
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
class ExShortcutKeyAction(private val exEntryPanel: ExEntryPanel) : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val keyStroke = getKeyStroke(e)
    if (keyStroke != null) {
      val editor = exEntryPanel.entry.editor
      KeyHandler.getInstance().handleKey(editor, keyStroke, EditorDataContext.init(editor, e.dataContext), 0)
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

    val shortcuts = ExKeyBindings.bindings.map {
      KeyboardShortcut(it.key, null)
    }.toTypedArray()

    registerCustomShortcutSet({ shortcuts }, exEntryPanel)
  }
}

