/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.CHAR_UNDEFINED
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_ENTER
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_ESCAPE

abstract class VimOutputPanelBase : VimOutputPanel {
  protected abstract val atEnd: Boolean

  override fun handleKey(key: VimKeyStroke) {
    if (atEnd) {
      close(key)
      return
    }

    when (key.keyChar) {
      ' ' -> scrollPage()
      'd' -> scrollHalfPage()
      'q', '\u001b' -> close()
      '\n' -> scrollLine()
      CHAR_UNDEFINED -> {
        when (key.keyCode) {
          VK_ENTER -> scrollLine()
          VK_ESCAPE -> close()
          else -> onBadKey()
        }
      }

      else -> onBadKey()
    }
  }

  protected abstract fun onBadKey()
  protected abstract fun close(key: VimKeyStroke?)
}