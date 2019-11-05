/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.ex.vimscript.VimScriptCommandHandler
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser
import java.util.regex.Pattern

/**
 * @author vlan
 */
class LetHandler : CommandHandler.SingleExecution(), VimScriptCommandHandler {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    execute(cmd)
    return true
  }

  @Throws
  override fun execute(cmd: ExCommand) {
    val argument = cmd.argument
    if (argument.trim().isEmpty()) {
      showVariables()
    } else {
      val matcher = SIMPLE_ASSIGNMENT.matcher(argument)
      if (matcher.matches()) {
        val name = matcher.group(1)
        // TODO: Check that 'name' is global
        val expression = matcher.group(2)
        val env = VimScriptGlobalEnvironment.getInstance()
        val globals = env.variables
        val value = VimScriptParser.evaluate(expression, globals)
        globals[name] = value
      } else {
        throw ExException("Only simple '=' assignments are supported in 'let' expressions")
      }
    }
  }

  @Throws(ExException::class)
  private fun showVariables() {
    throw ExException("'let' without arguments is not supported yet")
  }

  companion object {
    private val SIMPLE_ASSIGNMENT = Pattern.compile("((?:[gswtblav]:)?[A-Za-z_][A-Za-z_0-9]*)[ \\t]*=[ \\t]*(.*)")
  }
}
