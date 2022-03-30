/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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

sealed class MotionUpBase : MotionActionHandler.ForEachCaret() {
  private var col: Int = 0

  override fun preOffsetComputation(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
  ): Boolean {
    col = injector.engineEditorHelper.prepareLastColumn(caret)
    return true
  }

  override fun postMove(editor: VimEditor, caret: VimCaret, context: ExecutionContext, cmd: Command) {
    injector.engineEditorHelper.updateLastColumn(caret, col)
  }
}

open class MotionUpAction : MotionUpBase() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.getVerticalMotionOffset(editor, caret, -operatorArguments.count1).toMotionOrError()
  }
}

class MotionUpCtrlPAction : MotionUpAction() {
  override fun getOffset(
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

  override fun getOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.getVerticalMotionOffset(editor, caret, -operatorArguments.count1).toMotionOrError()
  }
}
