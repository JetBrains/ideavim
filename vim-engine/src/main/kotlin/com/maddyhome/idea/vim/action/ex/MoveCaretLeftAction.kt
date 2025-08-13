/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.common.Graphemes

@CommandOrMotion(keys = ["<Left>"], modes = [Mode.CMD_LINE])
class MoveCaretLeftAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val caret = commandLine.caret
    val prevOffset = Graphemes.prev(commandLine.text, caret.offset) ?: return true
    caret.offset = prevOffset
    return true
  }
}
