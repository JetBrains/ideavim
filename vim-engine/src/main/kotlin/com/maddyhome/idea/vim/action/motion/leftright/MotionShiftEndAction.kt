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
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.ShiftedSpecialKeyHandler
import com.maddyhome.idea.vim.state.mode.inSelectMode
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.isInsertionAllowed

@CommandOrMotion(keys = ["<S-End>"], modes = [Mode.INSERT, Mode.NORMAL, Mode.VISUAL, Mode.SELECT])
class MotionShiftEndAction : ShiftedSpecialKeyHandler() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun motion(editor: VimEditor, context: ExecutionContext, cmd: Command, caret: VimCaret) {
    var allow = false
    if (editor.isInsertionAllowed) {
      allow = true
    } else if (editor.inVisualMode || editor.inSelectMode) {
      allow = injector.options(editor).selection != "old"
    }

    val newOffset = injector.motion.moveCaretToRelativeLineEnd(editor, caret, cmd.count - 1, allow)

    caret.moveToOffset(newOffset)
    caret.vimLastColumn = VimMotionGroupBase.LAST_COLUMN
  }
}
