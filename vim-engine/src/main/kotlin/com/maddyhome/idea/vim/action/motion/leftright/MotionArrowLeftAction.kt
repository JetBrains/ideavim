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
import com.maddyhome.idea.vim.handler.NonShiftedSpecialKeyHandler

private fun doMotion(
  editor: VimEditor,
  caret: ImmutableVimCaret,
  count1: Int,
  whichwrapKey: String,
  allowPastEnd: Boolean,
): Motion {
  val allowWrap = injector.options(editor).whichwrap.contains(whichwrapKey)
  return injector.motion.getHorizontalMotion(editor, caret, count1, allowPastEnd, allowWrap)
}

abstract class MotionNonShiftedArrowLeftBaseAction() : NonShiftedSpecialKeyHandler() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun motion(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return doMotion(editor, caret, -operatorArguments.count1, "<", allowPastEnd)
  }

  protected open val allowPastEnd: Boolean = false
}

// Note that Select mode is handled in [SelectMotionArrowLeftAction]
@CommandOrMotion(keys = ["<Left>", "<kLeft>"], modes = [Mode.NORMAL, Mode.VISUAL])
class MotionArrowLeftAction : MotionNonShiftedArrowLeftBaseAction()

@CommandOrMotion(keys = ["<Left>", "<kLeft>"], modes = [Mode.OP_PENDING])
class MotionArrowLeftOpPendingAction : MotionNonShiftedArrowLeftBaseAction() {
  // When the motion is used with an operator, the EOL character is counted.
  // This allows e.g., `d<Left>` to delete the end of line character on the previous line when wrap is active
  // ('whichwrap' contains "<")
  // See `:help whichwrap`. This says a delete or change operator, but it appears to apply to all operators
  override val allowPastEnd = true
}

// Just needs to be a plain motion handler - it's not shifted, and the non-shifted actions don't apply in Insert mode
@CommandOrMotion(keys = ["<Left>", "<kLeft>"], modes = [Mode.INSERT])
class MotionArrowLeftInsertModeAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    // Insert mode is always allowed past the end of the line
    return doMotion(editor, caret, -operatorArguments.count1, "[", allowPastEnd = true)
  }
}
