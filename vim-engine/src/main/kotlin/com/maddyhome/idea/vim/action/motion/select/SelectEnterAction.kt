/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.select

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * @author Alex Plate
 */

@CommandOrMotion(keys = ["<CR>", "<C-J>", "<C-M>"], modes = [Mode.SELECT])
class SelectEnterAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

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
    return true
  }
}
