/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

// todo make it for each caret
@ExCommand(command = "norm[al]")
data class NormalCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags = flags(
    RangeFlag.RANGE_OPTIONAL,
    ArgumentFlag.ARGUMENT_OPTIONAL,
    Access.WRITABLE,
  )

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val useMappings = modifier != CommandModifier.BANG

    val rangeSpecified = range.size() != 0
    val range = getLineRange(editor, editor.primaryCaret())

    for (line in range.startLine..range.endLine) {
      if (editor.lineCount() < line) {
        break
      }

      // If a range is specified, the caret is moved to the start of each line before the command is executed
      if (rangeSpecified) {
        editor.currentCaret().moveToOffset(injector.motion.moveCaretToLineStart(editor, line))
      }

      // Perform operations
      val keys = injector.parser.stringToKeys(argument)
      val keyHandler = KeyHandler.getInstance()
      keyHandler.reset(editor)
      for (key in keys) {
        keyHandler.handleKey(editor, key, context, useMappings, false, keyHandler.keyHandlerState)
      }

      // Exit if state leaves as insert or cmd_line
      val mode = editor.mode
      if (mode is Mode.CMD_LINE) {
        injector.commandLine.getActiveCommandLine()?.close(refocusOwningEditor = true, resetCaret = false)
      }
      if (mode is Mode.INSERT || mode is Mode.REPLACE) {
        editor.exitInsertMode(context)
      }
    }

    return ExecutionResult.Success
  }
}
