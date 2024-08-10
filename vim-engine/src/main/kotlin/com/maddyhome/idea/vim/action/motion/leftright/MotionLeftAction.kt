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
import com.maddyhome.idea.vim.handler.MotionActionHandler

abstract class MotionLeftBaseAction(private val allowPastEnd: Boolean) : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val allowWrap = injector.options(editor).whichwrap.contains("h")
    return injector.motion.getHorizontalMotion(editor, caret, -operatorArguments.count1, allowPastEnd, allowWrap)
  }
}

@CommandOrMotion(keys = ["h"], modes = [Mode.NORMAL, Mode.VISUAL])
class MotionLeftAction : MotionLeftBaseAction(allowPastEnd = false)

// When the motion is used with an operator, the EOL character is counted.
// This allows e.g., `dh` to delete the end of line character on the previous line when wrap is active
// ('whichwrap' contains "h")
// See `:help whichwrap`. This says a delete or change operator, but it appears to apply to all operators
@CommandOrMotion(keys = ["h"], modes = [Mode.OP_PENDING])
class MotionLeftOpPendingModeAction : MotionLeftBaseAction(allowPastEnd = true)
