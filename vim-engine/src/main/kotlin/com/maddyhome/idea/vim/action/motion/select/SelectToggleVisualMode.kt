/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.select

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.pushSelectMode
import com.maddyhome.idea.vim.helper.pushVisualMode
import com.maddyhome.idea.vim.helper.vimStateMachine

/**
 * @author Alex Plate
 */

public class SelectToggleVisualMode : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    toggleMode(editor)
    return true
  }

  public companion object {
    public fun toggleMode(editor: VimEditor) {
      val commandState = editor.vimStateMachine
      val subMode = commandState.subMode
      val mode = commandState.mode
      commandState.popModes()
      if (mode.inVisualMode) {
        commandState.pushSelectMode(subMode, mode)
        if (subMode != VimStateMachine.SubMode.VISUAL_LINE) {
          editor.nativeCarets().forEach {
            if (it.offset.point + injector.visualMotionGroup.selectionAdj == it.selectionEnd) {
              it.moveToInlayAwareOffset(it.offset.point + injector.visualMotionGroup.selectionAdj)
            }
          }
        }
      } else {
        commandState.pushVisualMode(subMode, mode)
        if (subMode != VimStateMachine.SubMode.VISUAL_LINE) {
          editor.nativeCarets().forEach {
            if (it.offset.point == it.selectionEnd && it.visualLineStart <= it.offset.point - injector.visualMotionGroup.selectionAdj) {
              it.moveToInlayAwareOffset(it.offset.point - injector.visualMotionGroup.selectionAdj)
            }
          }
        }
      }
    }
  }
}
