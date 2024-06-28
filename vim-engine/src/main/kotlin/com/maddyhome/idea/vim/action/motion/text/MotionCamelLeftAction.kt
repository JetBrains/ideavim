/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.text

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
import com.maddyhome.idea.vim.handler.toMotionOrError

@CommandOrMotion(keys = ["[b"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionCamelLeftAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.searchHelper.findPreviousCamelStart(editor.text(), caret.offset, operatorArguments.count1)
      ?.toMotionOrError() ?: Motion.Error
  }
}

@CommandOrMotion(keys = ["[w"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionCamelRightAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.searchHelper.findNextCamelStart(editor.text(), caret.offset + 1, operatorArguments.count1)
      ?.toMotionOrError() ?: Motion.Error
  }
}
