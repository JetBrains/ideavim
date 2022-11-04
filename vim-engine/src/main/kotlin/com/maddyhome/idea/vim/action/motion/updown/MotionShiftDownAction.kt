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
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.ShiftedArrowKeyHandler

/**
 * @author Alex Plate
 */

class MotionShiftDownAction : ShiftedArrowKeyHandler(false) {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun motionWithKeyModel(editor: VimEditor, caret: VimCaret, context: ExecutionContext, cmd: Command) {
    val vertical = injector.motion.getVerticalMotionOffset(editor, caret, cmd.count)
    val col = injector.engineEditorHelper.prepareLastColumn(caret)
    caret.moveToOffset(vertical)

    injector.engineEditorHelper.updateLastColumn(caret, col)
  }

  override fun motionWithoutKeyModel(editor: VimEditor, context: ExecutionContext, cmd: Command) {
    injector.motion.scrollFullPage(editor, editor.primaryCaret(), cmd.count)
  }
}
