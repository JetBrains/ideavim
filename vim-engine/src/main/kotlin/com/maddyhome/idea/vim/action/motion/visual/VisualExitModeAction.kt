/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.visual

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * @author vlan
 */
class VisualExitModeAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    editor.exitVisualModeNative()

    editor.forEachCaret { caret ->
      val lineEnd = editor.getLineEndForOffset(caret.offset.point)
      if (lineEnd == caret.offset.point) {
        val position = injector.motion.getOffsetOfHorizontalMotion(editor, caret, -1, false)
        caret.moveToOffset(position)
      }
    }
    // Should it be in [exitVisualMode]?
    editor.forEachCaret { caret ->
      val lineEnd = editor.getLineEndForOffset(caret.offset.point)
      if (lineEnd == caret.offset.point) {
        val position = injector.motion.getOffsetOfHorizontalMotion(editor, caret, -1, false)
        caret.moveToOffset(position)
      }
    }

    return true
  }
}
