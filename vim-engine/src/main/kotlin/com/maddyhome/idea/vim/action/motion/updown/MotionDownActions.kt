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

@CommandOrMotion(keys = ["j", "<C-J>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
open class MotionDownAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE
  override val keepFold: Boolean = true

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.getVerticalMotionOffset(editor, caret, operatorArguments.count1)
  }
}

@CommandOrMotion(keys = ["<C-N>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionDownCtrlNAction : MotionDownAction() {
  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val activeLookup = injector.lookupManager.getActiveLookup(editor)
    return if (activeLookup != null) {
      val primaryCaret = editor.primaryCaret()
      if (caret == primaryCaret) {
        activeLookup.down(primaryCaret, context)
      }
      caret.offset.toMotion()
    } else {
      super.getOffset(editor, caret, context, argument, operatorArguments)
    }
  }
}

@CommandOrMotion(keys = ["gj", "g<Down>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionDownNotLineWiseAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.getVerticalMotionOffset(editor, caret, operatorArguments.count1)
  }
}
