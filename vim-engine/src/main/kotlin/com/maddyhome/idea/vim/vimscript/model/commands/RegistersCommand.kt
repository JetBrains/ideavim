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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :registers" / "h :display"
 */
@ExCommand(command = "dis[play],reg[isters]")
public data class RegistersCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val registerGroup = injector.registerGroup
    val regs = registerGroup.getRegisters()
      .filter { argument.isEmpty() || argument.contains(it.name) }
      .joinToString("\n", prefix = "Type Name Content\n") { reg ->
        val type = when (reg.type) {
          SelectionType.LINE_WISE -> "l"
          SelectionType.CHARACTER_WISE -> "c"
          SelectionType.BLOCK_WISE -> "b"
        }
        val text = reg.rawText?.let { injector.parser.parseKeys(it) } ?: reg.keys
        "  $type  \"${reg.name}   ${EngineStringHelper.toPrintableCharacters(text).take(200)}"
      }

    injector.exOutputPanel.getPanel(editor).output(regs)

    return ExecutionResult.Success
  }
}
