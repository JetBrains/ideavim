/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.updown

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion
import com.maddyhome.idea.vim.handler.toMotionOrError

sealed class MotionDownBase : MotionActionHandler.ForEachCaret() {
  private var col: Int = 0

  final override fun getOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments
  ): Motion {
    val motion = getMotion(editor, caret, context, argument, operatorArguments)
    return when (motion) {
      is Motion.AbsoluteOffset -> Motion.AdjustedOffset(motion.offset, col)
      else -> motion
    }
  }

  abstract fun getMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments
  ): Motion

  override fun preOffsetComputation(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
  ): Boolean {
    col = injector.engineEditorHelper.prepareLastColumn(caret)
    return true
  }
}

open class MotionDownAction : MotionDownBase() {

  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.getVerticalMotionOffset(editor, caret, operatorArguments.count1).toMotionOrError()
  }
}

class MotionDownCtrlNAction : MotionDownAction() {
  override fun getMotion(
    editor: VimEditor,
    caret: VimCaret,
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
      caret.offset.point.toMotion()
    } else {
      super.getOffset(editor, caret, context, argument, operatorArguments)
    }
  }
}

class MotionDownNotLineWiseAction : MotionDownBase() {

  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.getVerticalMotionOffset(editor, caret, operatorArguments.count1).toMotionOrError()
  }
}
