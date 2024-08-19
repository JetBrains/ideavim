/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent.KeyBinding

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal object ExKeyBindings {

  // TODO - add the following keys:
  // Ctrl-\ Ctrl-N - abort

  val bindings: Array<KeyBinding> by lazy {
    arrayOf(
      // Escape will cancel a pending insert digraph/register before cancelling
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ExEditorKit.EscapeChar),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.EscapeChar),

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

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.nextWordAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.nextWordAction),

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.StartDigraph),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.StartLiteral),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.StartLiteral),

      // These appear to be non-Vim shortcuts
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.pasteAction),
    )
  }
}
