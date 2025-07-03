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
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimChangeGroup
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler

@CommandOrMotion(keys = ["g~"], modes = [Mode.NORMAL])
class ChangeCaseToggleMotionAction : ChangeEditorActionHandler.ForEachCaret(), DuplicableOperatorAction {
  private val logger = vimLogger<ChangeCaseToggleMotionAction>()

  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = '~'

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (argument == null || argument !is Argument.Motion) {
      logger.error("Argument is null or not Argument.Motion. argument=$argument")
      return false
    }

    return injector.changeGroup.changeCaseMotion(
        editor,
        caret,
        context,
        VimChangeGroup.ChangeCaseType.TOGGLE,
        argument,
        operatorArguments,
      )
  }
}
