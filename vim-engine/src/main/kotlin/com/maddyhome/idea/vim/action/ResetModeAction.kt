/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.moveToMotion
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(
  keys = ["<C-\\><C-N>"],
  modes = [Mode.NORMAL, Mode.VISUAL, Mode.SELECT, Mode.OP_PENDING, Mode.INSERT, Mode.CMD_LINE]
)
class ResetModeAction : VimActionHandler.ConditionalMulticaret() {
  private lateinit var modeBeforeReset: com.maddyhome.idea.vim.state.mode.Mode
  override val type: Command.Type = Command.Type.OTHER_WRITABLE
  override fun runAsMulticaret(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    modeBeforeReset = editor.mode
    KeyHandler.getInstance().fullReset(editor)
    return true
  }

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (modeBeforeReset == com.maddyhome.idea.vim.state.mode.Mode.INSERT) {
      val position = injector.motion.getHorizontalMotion(editor, caret, -1, false)
      caret.moveToMotion(position)
    }
    return true
  }

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    error("This method should not be used")
  }
}
