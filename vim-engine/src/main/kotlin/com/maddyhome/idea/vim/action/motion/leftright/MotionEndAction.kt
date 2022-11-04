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
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.handler.NonShiftedSpecialKeyHandler
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

class MotionEndAction : NonShiftedSpecialKeyHandler() {
  override val motionType: MotionType = MotionType.INCLUSIVE

  override fun offset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): Int {
    var allow = false
    if (editor.inInsertMode) {
      allow = true
    } else if (editor.inVisualMode || editor.inSelectMode) {
      val opt = (
        injector.optionService
          .getOptionValue(OptionScope.LOCAL(editor), OptionConstants.selectionName) as VimString
        ).value
      if (opt != "old") {
        allow = true
      }
    }

    return injector.motion.moveCaretToLineEndOffset(editor, caret, count - 1, allow)
  }

  override fun preMove(editor: VimEditor, caret: VimCaret, context: ExecutionContext, cmd: Command) {
    caret.vimLastColumn = VimMotionGroupBase.LAST_COLUMN
  }

  override fun postMove(editor: VimEditor, caret: VimCaret, context: ExecutionContext, cmd: Command) {
    caret.vimLastColumn = VimMotionGroupBase.LAST_COLUMN
  }
}
