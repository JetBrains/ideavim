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
import com.maddyhome.idea.vim.helper.CharacterHelper

@CommandOrMotion(keys = ["<C-Right>", "<S-Right>"], modes = [Mode.CMD_LINE])
class MoveCaretToNextBigWordAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    // The docs say to move one WORD to the right, but in practice, we just move to the next whitespace character.
    val text = commandLine.text
    var pos = commandLine.caret.offset + 1
    while (pos < text.length && !CharacterHelper.isWhitespace(commandLine.editor, text[pos], true)) {
      pos++
    }
    commandLine.caret.offset = pos.coerceAtMost(text.length)
    return true
  }
}
