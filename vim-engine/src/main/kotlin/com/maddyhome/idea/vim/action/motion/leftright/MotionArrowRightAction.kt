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
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.usesVirtualSpace

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

abstract class MotionNonShiftedArrowRightBaseAction() : NonShiftedSpecialKeyHandler() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun motion(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return doMotion(editor, caret, operatorArguments.count1, ">", allowPastEnd(editor))
  }

  protected open fun allowPastEnd(editor: VimEditor) = editor.usesVirtualSpace || editor.isEndAllowed
}

// Note that Select mode is handled with [SelectMotionArrowRightAction]
@CommandOrMotion(keys = ["<Right>", "<kRight>"], modes = [Mode.NORMAL, Mode.VISUAL])
class MotionArrowRightAction : MotionNonShiftedArrowRightBaseAction()

@CommandOrMotion(keys = ["<Right>", "<kRight>"], modes = [Mode.OP_PENDING])
class MotionArrowRightOpPendingAction : MotionNonShiftedArrowRightBaseAction() {
  // When the motion is used with an operator, the EOL character is counted.
  // This allows e.g., `d<Right>` to delete the last character in a line. Note that we can't use editor.isEndAllowed to
  // give us this because the current mode when we execute the operator/motion is no longer OP_PENDING.
  // See `:help whichwrap`. This says a delete or change operator, but it appears to apply to all operators
  override fun allowPastEnd(editor: VimEditor) = true
}

// Just needs to be a plain motion handler - it's not shifted, and the non-shifted actions don't apply in Insert mode
@CommandOrMotion(keys = ["<Right>", "<kRight>"], modes = [Mode.INSERT])
class MotionArrowRightInsertModeAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    // Insert mode is always allowed past the end of the line
    return doMotion(editor, caret, operatorArguments.count1, "]", allowPastEnd = true)
  }
}
