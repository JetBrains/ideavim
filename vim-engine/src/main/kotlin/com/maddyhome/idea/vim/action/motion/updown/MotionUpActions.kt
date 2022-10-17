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
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion

sealed class MotionUpBase : MotionActionHandler.ForEachCaret() {
  final override fun getOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments
  ): Motion {
    return getMotion(editor, caret, context, argument, operatorArguments)
  }

  abstract fun getMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments
  ): Motion
}

open class MotionUpAction : MotionUpBase() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.getVerticalMotionOffset(editor, caret, -operatorArguments.count1)
  }
}

class MotionUpCtrlPAction : MotionUpAction() {
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
        activeLookup.up(caret, context)
      }
      caret.offset.point.toMotion()
    } else {
      super.getOffset(editor, caret, context, argument, operatorArguments)
    }
  }
}

class MotionUpNotLineWiseAction : MotionUpBase() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.getVerticalMotionOffset(editor, caret, -operatorArguments.count1)
  }
}
