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

      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK), ExEditorKit.DeleteToCursor),

      // These appear to be non-Vim shortcuts
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction),
      KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.pasteAction),
    )
  }
}
