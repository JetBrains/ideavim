/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.change

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.helper.CharacterHelper

class ChangeCaseUpperMotionAction : ChangeEditorActionHandler.ForEachCaret(), DuplicableOperatorAction {
  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = 'U'

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return argument != null &&
      injector.changeGroup
        .changeCaseMotion(
          editor,
          caret,
          context,
          CharacterHelper.CASE_UPPER,
          argument,
          operatorArguments
        )
  }
}
