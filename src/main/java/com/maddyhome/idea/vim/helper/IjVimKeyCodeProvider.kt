/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.jetbrains.Service
import com.maddyhome.idea.vim.key.VimKeyCodeProvider
import com.maddyhome.idea.vim.key.VimKeyStroke
import javax.swing.KeyStroke

@Service
class IjVimKeyCodeProvider : VimKeyCodeProvider {
}

val KeyStroke.vim : VimKeyStroke
  get() = TODO()

val VimKeyStroke.awt : KeyStroke
  get() = TODO()