/*
 * Copyright 2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService

class DeleteJoinLinesSpacesAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.DELETE

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.isOneLineMode()) return false
    if (injector.optionService.isSet(OptionScope.LOCAL(editor), IjVimOptionService.ideajoinName)) {
      return injector.changeGroup.joinViaIdeaByCount(editor, context, operatorArguments.count1)
    }
    injector.editorGroup.notifyIdeaJoin(editor)
    val res = arrayOf(true)
    editor.forEachNativeCaret(
      { caret: VimCaret ->
        if (!injector.changeGroup.deleteJoinLines(editor, caret, operatorArguments.count1, true, operatorArguments)) res[0] = false
      },
      true
    )
    return res[0]
  }
}
