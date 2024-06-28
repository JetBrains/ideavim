/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.window

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["<C-W>j", "<C-W><C-J>", "<C-W><Down>"], modes = [Mode.NORMAL])
class WindowDownAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.selectWindowInRow(editor.primaryCaret(), context, cmd.count, true)
    return true
  }
}

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["<C-W>h", "<C-W><C-H>", "<C-W><Left>"], modes = [Mode.NORMAL])
class WindowLeftAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.selectWindowInRow(editor.primaryCaret(), context, cmd.count * -1, false)
    return true
  }
}

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["<C-W>l", "<C-W><C-L>", "<C-W><Right>"], modes = [Mode.NORMAL])
class WindowRightAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.selectWindowInRow(editor.primaryCaret(), context, cmd.count, false)
    return true
  }
}

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["<C-W>k", "<C-W><C-K>", "<C-W><Up>"], modes = [Mode.NORMAL])
class WindowUpAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.selectWindowInRow(editor.primaryCaret(), context, cmd.count * -1, true)
    return true
  }
}
