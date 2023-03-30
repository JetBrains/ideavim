/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.delete

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.newapi.ijOptions

public class DeleteJoinLinesSpacesAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.DELETE

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.isOneLineMode()) return false
    if (injector.ijOptions(editor).ideajoin) {
      return injector.changeGroup.joinViaIdeaByCount(editor, context, operatorArguments.count1)
    }
    injector.editorGroup.notifyIdeaJoin(editor)
    var res = true
    editor.nativeCarets().sortedByDescending { it.offset.point }.forEach { caret ->
      if (!injector.changeGroup.deleteJoinLines(editor, caret, operatorArguments.count1, true, operatorArguments)) {
        res = false
      }
    }
    return res
  }
}
