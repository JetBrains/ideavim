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
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandHandlerFlags
import com.maddyhome.idea.vim.ex.CommandName
import com.maddyhome.idea.vim.ex.ComplicatedNameExCommand
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.ex.handler.MapHandler.SpecialArgument.EXPR
import com.maddyhome.idea.vim.ex.handler.MapHandler.SpecialArgument.SCRIPT
import com.maddyhome.idea.vim.ex.vimscript.VimScriptCommandHandler
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import java.util.*
import javax.swing.KeyStroke

/**
 * @author vlan
 */
class MapHandler : CommandHandler.SingleExecution(), VimScriptCommandHandler, ComplicatedNameExCommand {
  override val names: Array<CommandName> = COMMAND_NAMES
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    return executeCommand(cmd, editor)
  }

  @Throws(ExException::class)
  override fun execute(cmd: ExCommand) {
    executeCommand(cmd, null)
  }

  @Throws(ExException::class)
  private fun executeCommand(cmd: ExCommand, editor: Editor?): Boolean {
    val commandInfo = COMMAND_INFOS.find { cmd.command.startsWith(it.prefix) }
    if (commandInfo != null) {
      val argument = cmd.argument
      val modes = commandInfo.mappingModes
      if (argument.isEmpty()) {
        return editor != null && VimPlugin.getKey().showKeyMappings(modes, editor)
      } else {
        val arguments = try {
          parseCommandArguments(argument)
        } catch (ignored: IllegalArgumentException) {
          return false
        }

        if (arguments != null) {
          for (unsupportedArgument in UNSUPPORTED_SPECIAL_ARGUMENTS) {
            if (unsupportedArgument in arguments.specialArguments) {
              throw ExException("Unsupported map argument: $unsupportedArgument")
            }
          }
          VimPlugin.getKey().putKeyMapping(modes, arguments.fromKeys, arguments.toKeys, null,
            commandInfo.isRecursive)
          return true
        }
      }
    }
    return false
  }

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

  private class CommandArguments(val specialArguments: Set<SpecialArgument>, val fromKeys: List<KeyStroke>,
                                 val toKeys: List<KeyStroke>)

  private class CommandInfo(val prefix: String, suffix: String, val mappingModes: Set<MappingMode>, val isRecursive: Boolean) {
    val command = if (suffix.isBlank()) prefix else "$prefix[$suffix]"
  }

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
    val COMMAND_NAMES = commands(*COMMAND_INFOS.map { it.command }.toTypedArray())
    private val UNSUPPORTED_SPECIAL_ARGUMENTS = EnumSet.of(EXPR, SCRIPT)
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
      val toKeys = parseKeys(toKeysBuilder.toString().trimStart())
      CommandArguments(specialArguments, it, toKeys)
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
