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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.handler.Motion

@CommandOrMotion(keys = ["<C-Left>", "<S-Left>"], modes = [Mode.CMD_LINE])
class MoveToPreviousWordAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val text = commandLine.text
    // TODO: Should this be WORD or word?
    val motion = injector.motion.findOffsetOfNextWord(text, commandLine.caret.offset, -1, true, commandLine.editor)
    when (motion) {
      is Motion.AbsoluteOffset -> {
        commandLine.caret.offset = motion.offset
      }

      else -> {}
    }
    return true
  }
}
