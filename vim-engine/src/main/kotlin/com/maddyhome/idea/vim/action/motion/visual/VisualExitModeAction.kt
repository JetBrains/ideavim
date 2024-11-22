/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.visual

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.moveToMotion
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.exitVisualMode

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["<Esc>", "<C-[>", "<C-C>"], modes = [Mode.VISUAL])
class VisualExitModeAction : VimActionHandler.ConditionalMulticaret() {
  override val type: Command.Type = Command.Type.MODE_CHANGE
  override fun runAsMulticaret(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    // We don't need to choose here actually, but need a single call and multiple calls.
    // I don't really like this approach
    editor.exitVisualMode()

    return true
  }

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val lineEnd = editor.getLineEndForOffset(caret.offset)
    if (lineEnd == caret.offset) {
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
    throw NotImplementedError()
  }
}
