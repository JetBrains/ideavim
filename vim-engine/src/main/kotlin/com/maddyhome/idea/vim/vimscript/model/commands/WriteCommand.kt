/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.toTextRange
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :write"
 */
@ExCommand(command = "w[rite]")
data class WriteCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (argument.isEmpty()) {
      injector.file.saveFile(editor, context)
      return ExecutionResult.Success
    }
    val path = injector.pathExpansion.expandPath(argument.trim())
    val fileExists = injector.file.findFile(path, context) != null
    if (modifier != CommandModifier.BANG && fileExists) {
      injector.messages.showMessage(editor, "E37: File exists")
      return ExecutionResult.Error
    }
    injector.file.createFile(path, context, getText(editor), editor)
    return ExecutionResult.Success
  }

  private fun getText(editor: VimEditor): String {
    if (range.size() != 0) {
      val tr = getLineRange(editor).toTextRange(editor)
      return editor.getText(tr.startOffset, tr.endOffset)
    }
    return editor.text().toString()
  }
}
