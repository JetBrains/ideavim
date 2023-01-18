/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.leftright

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

class MotionLastScreenColumnAction : MotionActionHandler.ForEachCaret() {
  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    var allow = false
    if (editor.inInsertMode) {
      allow = true
    } else if (editor.inVisualMode) {
      val opt = (
        injector.optionService.getOptionValue(
          OptionScope.LOCAL(editor),
          OptionConstants.selection
        ) as VimString
        ).value
      if (opt != "old") {
        allow = true
      }
    }
    val motion = injector.motion.moveCaretToCurrentDisplayLineEnd(editor, caret, allow)
    return when (motion) {
      is Motion.AbsoluteOffset -> Motion.AdjustedOffset(motion.offset, VimMotionGroupBase.LAST_COLUMN)
      else -> motion
    }
  }

  override val motionType: MotionType = MotionType.INCLUSIVE
}
