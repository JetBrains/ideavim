/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.leftright

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.ShiftedSpecialKeyHandler
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

class MotionShiftEndAction : ShiftedSpecialKeyHandler() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun motion(editor: VimEditor, context: ExecutionContext, cmd: Command, caret: VimCaret) {
    var allow = false
    if (editor.inInsertMode) {
      allow = true
    } else if (editor.inVisualMode || editor.inSelectMode) {
      val opt = (
        injector.optionService.getOptionValue(
          OptionScope.LOCAL(editor),
          OptionConstants.selectionName
        ) as VimString
        ).value
      if (opt != "old") {
        allow = true
      }
    }

    val newOffset = injector.motion.moveCaretToLineEndOffset(editor, caret, cmd.count - 1, allow)
    caret.vimLastColumn = VimMotionGroupBase.LAST_COLUMN
    caret.moveToOffset(newOffset)
    caret.vimLastColumn = VimMotionGroupBase.LAST_COLUMN
  }
}
