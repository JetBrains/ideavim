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

package com.maddyhome.idea.vim.ui

import com.maddyhome.idea.vim.helper.StringHelper
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
object ModalEntry {
  inline fun activate(crossinline processor: (KeyStroke) -> Boolean) {
    val systemQueue = Toolkit.getDefaultToolkit().systemEventQueue
    val loop = systemQueue.createSecondaryLoop()

    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(object : KeyEventDispatcher {
      override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val stroke: KeyStroke
        if (e.id == KeyEvent.KEY_RELEASED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e)
          if (!StringHelper.isCloseKeyStroke(stroke) && stroke.keyCode != KeyEvent.VK_ENTER) {
            return true
          }
        } else if (e.id == KeyEvent.KEY_TYPED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e)
        } else {
          return true
        }
        if (!processor(stroke)) {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this)
          loop.exit()
        }
        return true
      }
    })

    loop.enter()
  }
}
