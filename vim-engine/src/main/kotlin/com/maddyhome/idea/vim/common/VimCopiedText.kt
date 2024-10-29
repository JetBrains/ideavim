/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

interface VimCopiedText {
  val text: String

  // TODO Looks like sticky tape, I'm not sure that we need to modify already stored text
  fun updateText(newText: String): VimCopiedText
}
