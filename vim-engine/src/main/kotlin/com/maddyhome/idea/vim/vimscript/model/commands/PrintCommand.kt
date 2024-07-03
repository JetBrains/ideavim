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
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :print"
 */
@ExCommand(command = "p[rint],P[rint]")
data class PrintCommand(val range: Range, val argument: String) : Command.SingleExecution(range, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    editor.removeSecondaryCarets()
    val caret = editor.currentCaret()
    val lineRange = getLineRangeWithCount(editor, caret)
    val text = getText(editor, (lineRange.startLine .. lineRange.endLine).toList())

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
    val existingText = exOutputModel.text?.let { if (it.isNotEmpty()) it.plus("\n") else it } ?: ""
    exOutputModel.output(existingText + text)
    return ExecutionResult.Success
  }

  companion object {
    /**
     * @param lines 0-based list of line numbers
     */
    fun getText(editor: VimEditor, lines: List<Int>): String {
      val showNumbers = injector.options(editor).number
      val biggestNumberLength = lines.max().toString().length
      return lines.joinToString("\n") {
        val number = if (showNumbers) (it + 1).toString().padStart(biggestNumberLength, ' ') + " " else ""
        "$number${getLineText(editor, it)}"
      }
    }

    private fun getLineText(editor: VimEditor, line: Int): String {
      val startOffset = editor.getLineStartOffset(line)
      val endOffset = editor.getLineEndOffset(line)
      return editor.getText(startOffset, endOffset)
    }
  }
}
