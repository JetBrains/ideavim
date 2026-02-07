/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import java.io.File
import java.io.IOException

/**
 * see "h :read"
 *
 * Inserts the contents of a file or command output below the current line (or specified line).
 * - `:read file` - insert file contents below current line
 * - `:read !cmd` - insert shell command output below current line
 * - `:0read file` - insert at the top of the buffer (before line 1)
 * - `:$read file` - insert at the end of the buffer
 * - `:{line}read file` - insert after the specified line number
 */
@ExCommand(command = "r[ead]")
data class ReadCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.WRITABLE)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (editor.isOneLineMode()) return ExecutionResult.Error

    val content = readContent(editor)

    if (content.isEmpty()) {
      return ExecutionResult.Success
    }

    val line = if (range.size() == 0) -1 else getLine(editor)
    val putData = createPutData(content, line, editor)
    return if (injector.put.putText(editor, context, putData)) ExecutionResult.Success else ExecutionResult.Error
  }

  private fun readContent(editor: VimEditor): String = if (commandModifier == CommandModifier.BANG) {
    if (commandArgument.isEmpty()) {
      throw exExceptionMessage("E471")
    }
    executeShellCommand(editor, commandArgument)
  } else {
    val filePath = injector.pathExpansion.expandPath(commandArgument)
    readFileContent(filePath)
  }

  private fun executeShellCommand(editor: VimEditor, command: String): String {
    return injector.processGroup.executeCommand(editor, command, null, null) ?: ""
  }

  private fun readFileContent(filePath: String): String {
    val file = File(filePath)
    if (!file.exists()) {
      throw exExceptionMessage("E484", filePath)
    }

    return try {
      file.readText()
    } catch (_: IOException) {
      throw exExceptionMessage("E484", filePath)
    }
  }

  private fun createPutData(content: String, line: Int, editor: VimEditor): PutData {
    val copiedText = injector.clipboardManager.dumbCopiedText(content)
    val caret = editor.currentCaret()
    val address = if (range.addresses.isEmpty()) -1 else range.addresses.last().getLine1(editor, caret)
    val textData = PutData.TextData(null, copiedText, SelectionType.LINE_WISE)
    return PutData(
      textData,
      null,
      1,
      insertTextBeforeCaret = false,
      rawIndent = false,
      caretAfterInsertedText = false,
      putToLine = line,
      putBeforeLine = address == 0,
    )
  }
}
