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
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.vimscript.model.Script

@CommandOrMotion(keys = ["&"], modes = [Mode.NORMAL])
class ChangeLastSearchReplaceAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    var result = true
    for (caret in editor.carets()) {
      val line = caret.getBufferPosition().line
      if (!injector.searchGroup
          .processSubstituteCommand(editor, caret, context, LineRange(line, line), "s", "//~/", Script(listOf()))
      ) {
        result = false
      }
    }
    return result
  }
}
