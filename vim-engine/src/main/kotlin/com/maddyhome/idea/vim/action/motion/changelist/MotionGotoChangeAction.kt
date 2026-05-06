/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.changelist

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler

/** `g;` -- go to [count] older position in the change list. */
@CommandOrMotion(keys = ["g;"], modes = [Mode.NORMAL])
class MotionGotoChangeOlderAction : MotionActionHandler.ForEachCaret() {
  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion = injector.motion.moveCaretToChange(editor, caret, -operatorArguments.count1)

  override val motionType: MotionType = MotionType.EXCLUSIVE
}

/** `g,` -- go to [count] newer position in the change list. */
@CommandOrMotion(keys = ["g,"], modes = [Mode.NORMAL])
class MotionGotoChangeNewerAction : MotionActionHandler.ForEachCaret() {
  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion = injector.motion.moveCaretToChange(editor, caret, operatorArguments.count1)

  override val motionType: MotionType = MotionType.EXCLUSIVE
}
