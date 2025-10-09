/*
 * Copyright 2003-2025 The IdeaVim authors
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

@CommandOrMotion(keys = ["<C-W>"], modes = [Mode.CMD_LINE])
class DeletePreviousWordAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val caretOffset = commandLine.caret.offset
    if (caretOffset == 0) return true

    val oldText = commandLine.text
    val motion = injector.motion.findOffsetOfNextWord(oldText, commandLine.caret.offset, count = -1, bigWord = false, commandLine.editor)
    if (motion is Motion.AbsoluteOffset) {
      commandLine.deleteText(motion.offset, caretOffset - motion.offset)
    }

    return true
  }
}
