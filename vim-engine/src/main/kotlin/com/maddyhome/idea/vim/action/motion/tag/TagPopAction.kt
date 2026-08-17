/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.tag

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["<C-T>"], modes = [Mode.NORMAL])
class TagPopAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val count = operatorArguments.count1
    val jump = injector.tagStack.popTag(editor, count) ?: run {
      injector.messages.showErrorMessage(editor, injector.messages.message("E555"))
      return false
    }

    val lp = BufferPosition(jump.line, jump.col, false)
    if (editor.getPath() != jump.filepath) {
      val newEditor = injector.file.selectEditor(editor.projectId, jump.filepath, jump.protocol)
        ?: return false
      newEditor.currentCaret().moveToOffset(
        newEditor.normalizeOffset(newEditor.bufferPositionToOffset(lp), false)
      )
    } else {
      editor.currentCaret().moveToOffset(
        editor.normalizeOffset(editor.bufferPositionToOffset(lp), false)
      )
    }
    return true
  }
}
