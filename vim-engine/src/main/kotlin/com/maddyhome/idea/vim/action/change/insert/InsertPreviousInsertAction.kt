/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.insert

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler

@CommandOrMotion(keys = ["<C-A>"], modes = [Mode.INSERT])
class InsertPreviousInsertAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.changeGroup.insertPreviousInsert(editor, context, false, operatorArguments)
    return true
  }
}

@CommandOrMotion(keys = ["<C-@>", "<C-S-2>", "<C-2>"], modes = [Mode.INSERT])
class InsertPreviousInsertExitAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.changeGroup.insertPreviousInsert(editor, context, true, operatorArguments)
    return false
  }
}
