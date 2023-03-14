/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.leftright

import com.maddyhome.idea.vim.action.ComplicatedKeysAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.usesVirtualSpace
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

public class MotionRightAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val allowWrap = injector.options(editor).hasValue(Options.whichwrap, "l")
    val allowEnd = usesVirtualSpace || editor.isEndAllowed ||
      operatorArguments.isOperatorPending // because of `dl` removing the last character
    return injector.motion.getHorizontalMotion(editor, caret, operatorArguments.count1, allowPastEnd = allowEnd, allowWrap)
  }
}

public class MotionRightInsertAction : MotionActionHandler.ForEachCaret(), ComplicatedKeysAction {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0)),
  )

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val allowWrap = injector.options(editor).hasValue(Options.whichwrap, "]")
    return injector.motion.getHorizontalMotion(editor, caret, operatorArguments.count1, allowPastEnd = true, allowWrap)
  }
}
