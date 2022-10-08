/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.updown

import com.maddyhome.idea.vim.action.ComplicatedKeysAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.NonShiftedSpecialKeyHandler
import com.maddyhome.idea.vim.handler.toAdjustedMotionOrError
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class MotionArrowDownAction : NonShiftedSpecialKeyHandler(), ComplicatedKeysAction {
  override val motionType: MotionType = MotionType.LINE_WISE

  override val keyStrokesSet: Set<List<KeyStroke>> =
    setOf(injector.parser.parseKeys("<Down>"), listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0)))

  private var col: Int = 0

  override fun motion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): Motion {
    val offset = injector.motion.getVerticalMotionOffset(editor, caret, count)
    return offset.toAdjustedMotionOrError(col)
  }

  override fun preOffsetComputation(editor: VimEditor, caret: VimCaret, context: ExecutionContext, cmd: Command): Boolean {
    col = injector.engineEditorHelper.prepareLastColumn(caret)
    return true
  }
}
