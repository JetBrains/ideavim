/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.ui.treeStructure.Tree
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities

/**
 * Captures the current tree selection and arranges to restore it if the
 * upcoming SpeedSearch session is dismissed via ESC, so the cursor returns
 * to the file the user was on before pressing `/`.
 *
 * No-op if no row is currently selected. Any non-ESC key (e.g. ENTER) cancels
 * the restoration so committing the search keeps the matched item selected.
 */
internal fun armSelectionRestoreOnEscape(tree: Tree) {
  val originalPath = tree.selectionPath ?: return

  lateinit var keyListener: KeyAdapter
  lateinit var focusListener: FocusAdapter
  val disarm = {
    tree.removeKeyListener(keyListener)
    tree.removeFocusListener(focusListener)
  }

  keyListener = object : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
      when (e.keyCode) {
        KeyEvent.VK_ESCAPE -> {
          disarm()
          // Defer until SpeedSearch finishes processing the ESC and clearing
          // its own state, so our restored selection is the one that sticks.
          SwingUtilities.invokeLater {
            tree.selectionPath = originalPath
            tree.scrollPathToVisible(originalPath)
          }
        }

        KeyEvent.VK_ENTER -> disarm()
      }
    }
  }

  // If focus leaves the tree before ESC/ENTER (mouse click elsewhere, popup
  // dismissed by tool window switch), drop both listeners so we don't leak
  // or surprise the user with a delayed jump on a later ESC.
  focusListener = object : FocusAdapter() {
    override fun focusLost(e: FocusEvent) = disarm()
  }

  tree.addKeyListener(keyListener)
  tree.addFocusListener(focusListener)
}
