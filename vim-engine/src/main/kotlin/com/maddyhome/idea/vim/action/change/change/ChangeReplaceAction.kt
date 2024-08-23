/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.change

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.VimEditorReplaceMask
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler

@CommandOrMotion(keys = ["R"], modes = [Mode.NORMAL])
class ChangeReplaceAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.CHANGE

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    changeReplace(editor, context)
    return true
  }
}

/**
 * Begin Replace mode
 * @param editor  The editor to replace in
 * @param context The data context
 */
private fun changeReplace(editor: VimEditor, context: ExecutionContext) {
  injector.changeGroup.initInsert(editor, context, com.maddyhome.idea.vim.state.mode.Mode.REPLACE)
  editor.replaceMask = VimEditorReplaceMask()
}
