/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change

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
import com.maddyhome.idea.vim.undo.VimKeyBasedUndoService
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService
import java.util.*

@CommandOrMotion(keys = ["<C-G>u"], modes = [Mode.INSERT])
class BreakUndoSequenceAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_UNDO_AWARE)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val undo = injector.undo
    when (undo) {
      is VimKeyBasedUndoService -> undo.setInsertNonMergeUndoKey(refresh = true)
      is VimTimestampBasedUndoService -> {
        val nanoTime = System.nanoTime()
        editor.forEachCaret { undo.endInsertSequence(it, it.offset, nanoTime) }
      }
    }
    return true
  }
}
