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

import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent.KeyBinding

object ExKeyBindings {

  // TODO - add the following keys:
  // Ctrl-\ Ctrl-N - abort

  val bindings: Array<KeyBinding> by lazy {
    arrayOf(
      // Escape will cancel a pending insert digraph/register before cancelling
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ExEditorKit.EscapeChar),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.EscapeChar),

      // Cancel entry, ignoring any pending actions such as digraph/registry entry
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.CancelEntry),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ExEditorKit.CompleteEntry),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.CompleteEntry),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.CompleteEntry),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.beginLineAction),
      KeyBinding(KeyStroke.getKeyStroke(0x02.toChar().toInt(), KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.beginLineAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), DefaultEditorKit.beginLineAction),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.endLineAction),
      KeyBinding(KeyStroke.getKeyStroke(0x05.toChar().toInt(), KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.endLineAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), DefaultEditorKit.endLineAction),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), DefaultEditorKit.deletePrevCharAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.deletePrevCharAction),
      KeyBinding(KeyStroke.getKeyStroke(0x08.toChar().toInt(), KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.deletePrevCharAction),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DefaultEditorKit.deleteNextCharAction),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.deletePrevWordAction),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.DeleteToCursor),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), ExEditorKit.HistoryUpFilter),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK), ExEditorKit.HistoryUp),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), ExEditorKit.HistoryUp),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.HistoryUp),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), ExEditorKit.HistoryDownFilter),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK), ExEditorKit.HistoryDown),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), ExEditorKit.HistoryDown),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.HistoryDown),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), ExEditorKit.ToggleInsertReplace),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), DefaultEditorKit.backwardAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.previousWordAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.previousWordAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), DefaultEditorKit.forwardAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.nextWordAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.nextWordAction),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.StartDigraph),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.StartLiteral),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.StartLiteral),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.InsertRegister),

      // These appear to be non-Vim shortcuts
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.pasteAction)
    )
  }
}
