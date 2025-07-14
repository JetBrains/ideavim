/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.CTRL_DOWN_MASK
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_C
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_ESCAPE

fun VimKeyStroke.isCloseKeyStroke(): Boolean {
  return keyCode == VK_ESCAPE ||
    keyChar.code == VK_ESCAPE ||
    keyCode == VK_C && modifiers and CTRL_DOWN_MASK != 0 ||
    keyCode == '['.code && modifiers and CTRL_DOWN_MASK != 0
}
