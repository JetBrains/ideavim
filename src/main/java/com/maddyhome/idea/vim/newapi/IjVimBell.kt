/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.VimBell
import java.awt.Toolkit

internal class IjVimBell : VimBell {

  private var lastBeepTimeMillis = 0L

  override fun ring() {
    // Vim only allows a beep once every half second - :help 'visualbell'
    val currentTimeMillis = System.currentTimeMillis()
    if (currentTimeMillis - lastBeepTimeMillis > 500) {
      Toolkit.getDefaultToolkit().beep()
      lastBeepTimeMillis = currentTimeMillis
    }
  }
}
