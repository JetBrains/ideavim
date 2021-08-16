/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.mapping.MapCommand.SpecialArgument.EXPR
import com.maddyhome.idea.vim.vimscript.model.commands.mapping.MapCommand.SpecialArgument.SCRIPT
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.annotations.NonNls
import java.util.*
import javax.swing.KeyStroke

/**
 * @author vlan
 */
data class MapCommand(val ranges: Ranges, val argument: String, val cmd: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun processCommand(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult {
    return if (executeCommand(editor)) ExecutionResult.Success else ExecutionResult.Error
  }

  @Throws(ExException::class)
  private fun executeCommand(editor: Editor?): Boolean {
    val commandInfo = COMMAND_INFOS.find { cmd.startsWith(it.prefix) } ?: return false
    val modes = commandInfo.mappingModes

    if (argument.isEmpty()) return editor != null && VimPlugin.getKey().showKeyMappings(modes, editor)

    val arguments = try {
      parseCommandArguments(argument) ?: return false
    } catch (ignored: IllegalArgumentException) {
      return false
    }

    for (unsupportedArgument in UNSUPPORTED_SPECIAL_ARGUMENTS) {
      if (unsupportedArgument in arguments.specialArguments) {
        throw ExException("Unsupported map argument: $unsupportedArgument")
      }
    }

    if (arguments.specialArguments.contains(EXPR)) {
      VimPlugin.getKey()
        .putKeyMapping(modes, arguments.fromKeys, MappingOwner.IdeaVim, arguments.toExpr, arguments.secondArgument, commandInfo.isRecursive)
    } else {
      val toKeys = parseKeys(arguments.secondArgument)
      VimPlugin.getKey()
        .putKeyMapping(modes, arguments.fromKeys, MappingOwner.IdeaVim, toKeys, commandInfo.isRecursive)
    }

    return true
  }

  @Suppress("unused")
  @NonNls
  private enum class SpecialArgument(val myName: String) {
    BUFFER("<buffer>"),
    NOWAIT("<nowait>"),
    SILENT("<silent>"),
    SPECIAL("<special>"),
    SCRIPT("<script>"),
    EXPR("<expr>"),
    UNIQUE("<unique>");

    override fun toString(): String {
      return this.myName
    }

    companion object {
      fun fromString(s: String): SpecialArgument? {
        for (argument in values()) {
          if (s == argument.myName) {
            return argument
          }
        }
        return null
      }
    }
  }

  private class CommandArguments(
    val specialArguments: Set<SpecialArgument>,
    val fromKeys: List<KeyStroke>,
    val toExpr: Expression,
    val secondArgument: String,
  )

  companion object {
    private const val CTRL_V = '\u0016'
    private val COMMAND_INFOS = arrayOf(
      // TODO: Support smap, map!, lmap
      CommandInfo("map", "", MappingMode.NVO, true),
      CommandInfo("nm", "ap", MappingMode.N, true),
      CommandInfo("vm", "ap", MappingMode.V, true),
      CommandInfo("xm", "ap", MappingMode.X, true),
      CommandInfo("om", "ap", MappingMode.O, true),
      CommandInfo("im", "ap", MappingMode.I, true),
      CommandInfo("cm", "ap", MappingMode.C, true),

      // TODO: Support snoremap, noremap!, lnoremap
      CommandInfo("no", "remap", MappingMode.NVO, false),
      CommandInfo("nn", "oremap", MappingMode.N, false),
      CommandInfo("vn", "oremap", MappingMode.V, false),
      CommandInfo("xn", "oremap", MappingMode.X, false),
      CommandInfo("ono", "remap", MappingMode.O, false),
      CommandInfo("ino", "remap", MappingMode.I, false),
      CommandInfo("cno", "remap", MappingMode.C, false)
    )
    private val UNSUPPORTED_SPECIAL_ARGUMENTS = EnumSet.of(SCRIPT)
  }

  private fun parseCommandArguments(input: String): CommandArguments? {
    val firstBarSeparatedCommand = getFirstBarSeparatedCommand(input)

    val specialArguments = HashSet<SpecialArgument>()
    val toKeysBuilder = StringBuilder()
    var fromKeys: List<KeyStroke>? = null
    firstBarSeparatedCommand.split(" ").dropLastWhile { it.isEmpty() }.forEach { part ->
      if (fromKeys != null) {
        toKeysBuilder.append(" ")
        toKeysBuilder.append(part)
      } else {
        val specialArgument = SpecialArgument.fromString(part)
        if (specialArgument != null) {
          specialArguments.add(specialArgument)
        } else {
          fromKeys = parseKeys(part)
        }
      }
    }
    for (i in firstBarSeparatedCommand.length - 1 downTo 0) {
      val c = firstBarSeparatedCommand[i]
      if (c == ' ') {
        toKeysBuilder.append(c)
      } else {
        break
      }
    }
    return fromKeys?.let {
      val toExpr = if (specialArguments.contains(EXPR)) {
        VimscriptParser.parseExpression(toKeysBuilder.toString().trim()) ?: throw ExException("E15: Invalid expression: ${toKeysBuilder.toString().trim()}")
      } else {
        SimpleExpression(VimString(toKeysBuilder.toString().trimStart()))
      }
      CommandArguments(specialArguments, it, toExpr, toKeysBuilder.toString().trimStart())
    }
  }

  private fun getFirstBarSeparatedCommand(input: String): String {
    val inputBuilder = StringBuilder()
    var escape = false
    for (element in input) {
      if (escape) {
        escape = false
        if (element != '|') {
          inputBuilder.append('\\')
        }
        inputBuilder.append(element)
      } else if (element == '\\' || element == CTRL_V) {
        escape = true
      } else if (element == '|') {
        break
      } else {
        inputBuilder.append(element)
      }
    }
    if (input.endsWith("\\")) {
      inputBuilder.append("\\")
    }
    return inputBuilder.toString()
  }
}
