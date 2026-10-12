/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError

/** The motion of a [CharSearch] in operator-pending mode, e.g. `dfa` */
internal class CleverFMotionAction(
  private val search: CharSearch,
  private val session: CleverFSession,
) : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = if (search.isInclusive) MotionType.INCLUSIVE else MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val target = search.findTarget(editor, caret.offset, operatorArguments.count1) ?: return Motion.Error
    if (caret.isPrimary) session.recordJump(search.state, editor, target)
    return target.toMotionOrError()
  }
}
