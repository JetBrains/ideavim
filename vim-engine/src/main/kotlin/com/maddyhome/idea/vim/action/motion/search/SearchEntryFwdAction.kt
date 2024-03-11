/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.search

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

@CommandOrMotion(keys = ["/"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
public class SearchEntryFwdAction : MotionActionHandler.ForEachCaret() {
  override val argumentType: Argument.Type = Argument.Type.EX_STRING

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    if (argument == null) return Motion.Error
    val offsetAndMotion = injector.searchGroup.processSearchCommand(
      editor,
      argument.string,
      caret.offset,
      Direction.FORWARDS
    )
    if (offsetAndMotion == null) return Motion.Error
    motionType = offsetAndMotion.second
    return offsetAndMotion.first.toMotionOrError()
  }

  // Default to EXCLUSIVE, but override in `execute`, based on the search offset
  override var motionType: MotionType = MotionType.EXCLUSIVE
}
