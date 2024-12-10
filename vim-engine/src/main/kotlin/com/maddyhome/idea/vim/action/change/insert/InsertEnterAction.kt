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
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

@CommandOrMotion(keys = ["<CR>", "<C-M>", "<C-J>"], modes = [Mode.INSERT])
class InsertEnterAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (injector.application.isOctopusEnabled()) {
      if (editor.isInForEachCaretScope()) {
        editor.removeSecondaryCarets()
        injector.changeGroup.processEnter(editor, editor.primaryCaret(), context)
      } else {
        editor.forEachNativeCaret({ caret ->
          injector.changeGroup.processEnter(editor, caret, context)
        })
      }
    } else {
      injector.changeGroup.processEnter(editor, context)
    }
    injector.scroll.scrollCaretIntoView(editor)
    return true
  }
}
