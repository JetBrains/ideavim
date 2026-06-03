/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.resize

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.InvalidCommandException
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.CommandModifier

/**
 * The `:vertical` command modifier, supported here only in front of `resize` to give `:vertical resize`,
 * which sets the *width* of the current window (Vim counts columns). The resizing itself is delegated to
 * [ResizeService]; the argument forms (`{n}`, `+{n}`, `-{n}`, none) match [ResizeCommand].
 *
 * `:vertical` in front of any other command is not supported yet.
 *
 * see "h :vertical" / "h :vertical-resize"
 */
@ExCommand(command = "vert[ical]")
internal data class VerticalCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.SELF_SYNCHRONIZED)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val (subCommand, subArgument) = splitOnFirstWhitespace(argument.trim())

    if (!isResize(subCommand)) {
      throw InvalidCommandException("E492: Not an editor command: $subCommand", null)
    }

    ResizeService().resizeCurrentWindowWidth(editor, ResizeArgument.parse(subArgument))
    return ExecutionResult.Success
  }

  private fun splitOnFirstWhitespace(text: String): Pair<String, String> {
    val parts = text.split(WHITESPACE, limit = 2)
    return parts[0] to parts.getOrElse(1) { "" }
  }

  /** Matches `res[ize]`: any prefix of "resize" that is at least the mandatory "res". */
  private fun isResize(name: String): Boolean = name.length >= 3 && "resize".startsWith(name)

  companion object {
    private val WHITESPACE = Regex("\\s+")
  }
}
