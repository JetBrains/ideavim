/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.leftright

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.ShiftedSpecialKeyHandler

/**
 * @author Alex Plate
 */
@CommandOrMotion(keys = ["<S-Home>"], modes = [Mode.INSERT, Mode.NORMAL, Mode.VISUAL, Mode.SELECT])
class MotionShiftHomeAction : ShiftedSpecialKeyHandler() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun motion(editor: VimEditor, context: ExecutionContext, cmd: Command, caret: VimCaret) {
    val newOffset = injector.motion.moveCaretToCurrentLineStart(editor, caret)
    caret.moveToOffset(newOffset)
  }
}
