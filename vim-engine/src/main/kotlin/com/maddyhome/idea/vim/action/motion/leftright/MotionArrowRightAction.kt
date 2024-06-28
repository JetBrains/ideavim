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
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.NonShiftedSpecialKeyHandler
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.usesVirtualSpace

@CommandOrMotion(keys = ["<Right>", "<kRight>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionArrowRightAction : NonShiftedSpecialKeyHandler() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun motion(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val allowPastEnd = editor.usesVirtualSpace || editor.isEndAllowed ||
      operatorArguments.isOperatorPending // because of `d<Right>` removing the last character
    val allowWrap = injector.options(editor).whichwrap.contains(">")
    return injector.motion.getHorizontalMotion(editor, caret, operatorArguments.count1, allowPastEnd, allowWrap)
  }
}
