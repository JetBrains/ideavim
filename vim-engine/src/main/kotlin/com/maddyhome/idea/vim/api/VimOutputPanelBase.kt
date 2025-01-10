/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import java.awt.event.KeyEvent
import javax.swing.KeyStroke

abstract class VimOutputPanelBase : VimOutputPanel {
  protected abstract val atEnd: Boolean

  override fun handleKey(key: KeyStroke) {
    if (atEnd) {
      close(key)
      return
    }

    when (key.keyChar) {
      ' ' -> scrollPage()
      'd' -> scrollHalfPage()
      'q', '\u001b' -> close()
      '\n' -> scrollLine()
      KeyEvent.CHAR_UNDEFINED -> {
        when (key.keyCode) {
          KeyEvent.VK_ENTER -> scrollLine()
          KeyEvent.VK_ESCAPE -> close()
          else -> onBadKey()
        }
      }

      else -> onBadKey()
    }
  }

  protected abstract fun onBadKey()
  protected abstract fun close(key: KeyStroke?)
}