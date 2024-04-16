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
 * see "h :print"
 */
@ExCommand(command = "p[rint],P[rint]")
public data class PrintCommand(val range: Range, val argument: String) : Command.SingleExecution(range, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    editor.removeSecondaryCarets()
    val caret = editor.currentCaret()
    val lineRange = getLineRangeWithCount(editor, caret)
    val textRange = lineRange.toTextRange(editor)
    val text = editor.getText(textRange)

    // Move the caret to the start of the last line of the range
    val offset = injector.motion.moveCaretToLineStartSkipLeading(editor, lineRange.endLine)
    caret.moveToOffset(offset)

    // Note that we append to the existing text because we can be called multiple times by the :global command
    // TODO: We need a better way to handle output. This is not very efficient, especially if we have a lot of output
    val exOutputModel = injector.exOutputPanel.getPanel(editor)
    if (!exOutputModel.isActive) {
      // When we add text, we make the panel active. So if we're appending, it should already be active. If it's not,
      // make sure it's clear
      exOutputModel.clear()
    }
    exOutputModel.output((exOutputModel.text ?: "") + text)
    return ExecutionResult.Success
  }
}
