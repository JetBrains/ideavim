/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.change

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.action.motion.updown.MotionDownLess1FirstNonSpaceAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandFlags.FLAG_NO_REPEAT_INSERT
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.EnumSet

@CommandOrMotion(keys = ["S"], modes = [Mode.NORMAL])
class ChangeLineAction : ChangeInInsertSequenceAction() {
  override val type: Command.Type = Command.Type.CHANGE

  override val flags: EnumSet<CommandFlags> = enumSetOf(FLAG_NO_REPEAT_INSERT)

  override fun executeInInsertSequence(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    // `S` command is a synonym of `cc`
    val motion = MotionDownLess1FirstNonSpaceAction()
    return injector.changeGroup.changeMotion(
      editor,
      caret,
      context,
      Argument.Motion(motion, null),
      operatorArguments,
    )
  }
}
