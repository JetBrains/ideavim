/*
 * Copyright 2003-2024 The IdeaVim authors
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
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["<C-H>", "<BS>"], modes = [Mode.INSERT])
internal class InsertBackspaceAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.insertMode) {
      injector.changeGroup.processBackspace(editor, context)
    } else {
      for (caret in editor.carets()) {
        val offset = (caret.offset - 1).takeIf { it > 0 } ?: continue
        val oldChar = editor.replaceMask?.popChange(editor, offset)
        if (oldChar != null) {
          injector.changeGroup.replaceText(editor, caret, offset, offset + 1, oldChar.toString())
        }
        caret.moveToOffset(offset)
      }
    }
    return true
  }
}
