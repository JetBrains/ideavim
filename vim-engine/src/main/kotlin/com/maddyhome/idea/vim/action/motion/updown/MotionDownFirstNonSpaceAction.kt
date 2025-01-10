/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.updown

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
import com.maddyhome.idea.vim.handler.toMotion

@CommandOrMotion(keys = ["+", "<C-M>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionDownFirstNonSpaceAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.moveCaretToRelativeLineStartSkipLeading(editor, caret, operatorArguments.count1).toMotion()
  }
}

// FIXME I should not exist (see class above)
@CommandOrMotion(keys = ["<CR>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class EnterNormalAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val templateState = injector.templateManager.getTemplateState(editor)
    return if (templateState != null) {
      injector.actionExecutor.executeAction(
        editor,
        name = injector.actionExecutor.ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE,
        context = context
      )
      Motion.NoMotion
    } else {
      injector.motion.moveCaretToRelativeLineStartSkipLeading(editor, caret, operatorArguments.count1).toMotion()
    }
  }
}
