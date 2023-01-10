/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.delete

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope

class DeleteMotionAction : ChangeEditorActionHandler.ForEachCaret(), DuplicableOperatorAction {
  override val type: Command.Type = Command.Type.DELETE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = 'd'

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (argument == null) return false
    if (injector.optionService.isSet(OptionScope.GLOBAL, OptionConstants.experimentalapiName)) {
      val (first, second) = injector.changeGroup
        .getDeleteRangeAndType2(editor, caret, context, argument, false, operatorArguments)
        ?: return false
      return injector.changeGroup.deleteRange2(editor, caret, first, second)
    } else {
      val (first, second) = injector.changeGroup
        .getDeleteRangeAndType(editor, caret, context, argument, false, operatorArguments)
        ?: return false
      return injector.changeGroup.deleteRange(editor, caret, first, second, false, operatorArguments)
    }
  }
}
