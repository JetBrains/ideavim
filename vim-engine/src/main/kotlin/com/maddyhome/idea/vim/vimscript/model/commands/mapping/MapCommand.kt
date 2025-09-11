/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.CommandModifier
import com.maddyhome.idea.vim.vimscript.model.commands.mapping.MapCommand.SpecialArgument.EXPR
import com.maddyhome.idea.vim.vimscript.model.commands.mapping.MapCommand.SpecialArgument.SCRIPT
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import org.jetbrains.annotations.NonNls
import java.util.*
import javax.swing.KeyStroke

/**
 * @author vlan
 */
@ExCommand(command = "map,nm[ap],vm[ap],xm[ap],smap,om[ap],im[ap],lm[ap],cm[ap],nn[oremap],vn[oremap],xn[oremap],snor[emap],ono[remap],no[remap],ino[remap],ln[oremap],cno[remap]")
data class MapCommand(val range: Range, val cmd: String, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    return if (executeCommand(editor)) ExecutionResult.Success else ExecutionResult.Error
  }

  @Throws(ExException::class)
  private fun executeCommand(editor: VimEditor): Boolean {
    val bang = modifier == CommandModifier.BANG
    val commandInfo = COMMAND_INFOS.find { cmd.startsWith(it.prefix) && it.bang == bang }
    if (commandInfo == null) {
      if (modifier == CommandModifier.BANG) throw exExceptionMessage("E477")
      return false
    }

    val modes = commandInfo.mappingModes

    // Trailing whitespace is trimmed from the argument, unless it's separated with a bar.
    // If there are no spaces (ignoring trailing spaces), then it's a single argument and should be treated as a prefix.
    // Empty string is an empty prefix that matches everything.
    if (argument.isBlank() || !argument.trim().contains(' ')) {
      val prefix = injector.parser.parseKeys(argument.trim())
      return injector.keyGroup.showKeyMappings(modes, prefix, editor)
    }

    val arguments = try {
      parseCommandArguments(argument) ?: return false
    } catch (_: IllegalArgumentException) {
      return false
    }

    for (unsupportedArgument in UNSUPPORTED_SPECIAL_ARGUMENTS) {
      if (unsupportedArgument in arguments.specialArguments) {
        throw ExException("Unsupported map argument: $unsupportedArgument")
      }
    }

    val mappingOwner =
      if (injector.vimscriptExecutor.executingIdeaVimRcConfiguration) {
        MappingOwner.IdeaVim.InitScript
      } else {
        MappingOwner.IdeaVim.Other
      }
    if (arguments.specialArguments.contains(EXPR)) {
      injector.statisticsService.setIfMapExprUsed(true)
      injector.keyGroup
        .putKeyMapping(
          modes,
          arguments.fromKeys,
          mappingOwner,
          arguments.toExpr,
          arguments.secondArgument,
          commandInfo.isRecursive
        )
    } else {
      val mapping = transformOldActionSyntax(arguments.secondArgument)
      val toKeys = injector.parser.parseKeys(mapping)
      val isRecursive = mapping != arguments.secondArgument || commandInfo.isRecursive
      injector.keyGroup.putKeyMapping(modes, arguments.fromKeys, mappingOwner, toKeys, isRecursive)
    }

    return true
  }

  private fun transformOldActionSyntax(argument: String): String {
    extractActionId(argument)?.let {
      // Prefer `<Action>(...)` syntax to `:action ...<CR>` syntax. It's just better.
      // Workaround for https://youtrack.jetbrains.com/issue/VIM-2607/Action-BackForward-no-longer-work-the-same
      val newMapping = "<Action>($it)"
      logger.debug("Replaced $argument with $newMapping")
      return newMapping
    }

    return argument
  }

  private fun extractActionId(argument: String): String? {
    val patternStart = "^:+\\s*action\\s+"
    val patternEnd = "\\s*((<CR>)|(<Cr>)|(<cr>)|(<ENTER>)|(<Enter>)|(<enter>)|(<C-M>)|(<C-m>)|(<c-m>)|\n|\r|(\r\n))$"
    return if (argument.matches(Regex("$patternStart\\w+$patternEnd"))) {
      argument.replace(patternStart.toRegex(), "").replace(patternEnd.toRegex(), "")
    } else {
      null
    }
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
    UNIQUE("<unique>"),
    ;

    override fun toString(): String {
      return this.myName
    }

    companion object {
      fun fromString(s: String): SpecialArgument? {
        for (argument in entries) {
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
    private val COMMAND_INFOS = arrayOf(
      // TODO: Support lmap
      CommandInfo("map", "", MappingMode.NVO, true),
      CommandInfo("map", "", MappingMode.IC, true, bang = true),
      CommandInfo("nm", "ap", MappingMode.N, true),
      CommandInfo("vm", "ap", MappingMode.V, true),
      CommandInfo("xm", "ap", MappingMode.X, true),
      CommandInfo("smap", "", MappingMode.S, true),
      CommandInfo("om", "ap", MappingMode.O, true),
      CommandInfo("im", "ap", MappingMode.I, true),
      CommandInfo("cm", "ap", MappingMode.C, true),

      // TODO: Support lnoremap
      CommandInfo("no", "remap", MappingMode.NVO, false),
      CommandInfo("no", "remap", MappingMode.IC, false, bang = true),
      CommandInfo("nn", "oremap", MappingMode.N, false),
      CommandInfo("vn", "oremap", MappingMode.V, false),
      CommandInfo("xn", "oremap", MappingMode.X, false),
      CommandInfo("snor", "emap", MappingMode.S, false),
      CommandInfo("ono", "remap", MappingMode.O, false),
      CommandInfo("ino", "remap", MappingMode.I, false),
      CommandInfo("cno", "remap", MappingMode.C, false),
    )
    private val UNSUPPORTED_SPECIAL_ARGUMENTS = EnumSet.of(SCRIPT)
    private val logger = vimLogger<MapCommand>()
  }

  private fun parseCommandArguments(input: String): CommandArguments? {
    val specialArguments = HashSet<SpecialArgument>()
    val toKeysBuilder = StringBuilder()
    var fromKeys: List<KeyStroke>? = null

    val preprocessedInput = processBars(input)
    preprocessedInput.split(" ").dropLastWhile { it.isEmpty() }.forEach { part ->
      if (fromKeys != null) {
        toKeysBuilder.append(" ")
        toKeysBuilder.append(part)
      } else {
        val specialArgument = SpecialArgument.fromString(part)
        if (specialArgument != null) {
          specialArguments.add(specialArgument)
        } else {
          fromKeys = injector.parser.parseKeys(processBars(part))
        }
      }
    }
    for (i in preprocessedInput.length - 1 downTo 0) {
      val c = preprocessedInput[i]
      if (c == ' ') {
        toKeysBuilder.append(c)
      } else {
        break
      }
    }
    return fromKeys?.let {
      val toExpr = if (specialArguments.contains(EXPR)) {
        injector.vimscriptParser.parseExpression(toKeysBuilder.toString().trim())
          ?: throw ExException("E15: Invalid expression: ${toKeysBuilder.toString().trim()}")
      } else {
        SimpleExpression(toKeysBuilder.toString().trimStart())
      }
      CommandArguments(specialArguments, it, toExpr, toKeysBuilder.toString().trimStart())
    }
  }

  private fun processBars(fromString: String): String {
    return fromString.replace("\\|", "|")
  }
}
