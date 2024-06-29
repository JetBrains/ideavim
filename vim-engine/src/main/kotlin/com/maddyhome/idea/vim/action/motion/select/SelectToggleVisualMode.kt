/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.select

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.pushVisualMode
import com.maddyhome.idea.vim.helper.setSelectMode
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * @author Alex Plate
 */

@CommandOrMotion(keys = ["<C-G>"], modes = [Mode.VISUAL, Mode.SELECT])
class SelectToggleVisualMode : VimActionHandler.SingleExecution() {

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

  companion object {
    fun toggleMode(editor: VimEditor) {
      val myMode = editor.mode
      if (myMode is com.maddyhome.idea.vim.state.mode.Mode.VISUAL) {
        editor.setSelectMode(myMode.selectionType)
        if (myMode.selectionType != SelectionType.LINE_WISE) {
          editor.nativeCarets().forEach {
            if (it.offset + injector.visualMotionGroup.selectionAdj == it.selectionEnd) {
              it.moveToInlayAwareOffset(it.offset + injector.visualMotionGroup.selectionAdj)
            }
          }
        }
      } else if (myMode is com.maddyhome.idea.vim.state.mode.Mode.SELECT) {
        editor.pushVisualMode(myMode.selectionType)
        if (myMode.selectionType != SelectionType.LINE_WISE) {
          editor.nativeCarets().forEach {
            if (it.offset == it.selectionEnd && it.visualLineStart <= it.offset - injector.visualMotionGroup.selectionAdj) {
              it.moveToInlayAwareOffset(it.offset - injector.visualMotionGroup.selectionAdj)
            }
          }
        }
      }
    }
  }
}
