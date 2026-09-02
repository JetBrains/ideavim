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
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.VimRegexOptions
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.expressions.CurlyBracesName
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

@ExCommand(command = "fu[nction]")
internal class FunctionCommand private constructor(
  val range: Range,
  modifier: CommandModifier,
  private val functionNamePrefix: String?,
  private val functionName: CurlyBracesName?,
  private val literalDictionaryKey: String?,
  private val pattern: String?,
  private val trailingCharacters: String?,
  private val missingEndFunction: Boolean = false,
  )
  : Command.SingleExecution(range, modifier) {

  constructor(range: Range, modifier: CommandModifier)
    : this(
    range,
    modifier,
    functionNamePrefix = null,
    functionName = null,
    literalDictionaryKey = null,
    pattern = null,
    trailingCharacters = null
  )

  constructor(range: Range, modifier: CommandModifier, pattern: String?)
    : this(
    range,
    modifier,
    functionNamePrefix = null,
    functionName = null,
    literalDictionaryKey = null,
    pattern = pattern,
    trailingCharacters = null
  )

  /**
   * The text looks like the start of a function definition, but there is no matching `:endfunction`
   */
  constructor(range: Range, modifier: CommandModifier, missingEndFunction: Boolean)
    : this(
    range,
    modifier,
    functionNamePrefix = null,
    functionName = null,
    literalDictionaryKey = null,
    pattern = null,
    trailingCharacters = null,
    missingEndFunction = missingEndFunction
  )

  constructor(
    range: Range,
    modifier: CommandModifier,
    functionNamePrefix: String?,
    functionName: CurlyBracesName?,
    literalDictionaryKey: String?,
    trailingCharacters: String?,
  )
    : this(
    range,
    modifier,
    functionNamePrefix = functionNamePrefix,
    functionName = functionName,
    literalDictionaryKey = literalDictionaryKey,
    pattern = null,
    trailingCharacters = trailingCharacters
  )

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (missingEndFunction) {
      throw exExceptionMessage("E126")
    }

    if (trailingCharacters?.isNotBlank() == true && literalDictionaryKey == null) {
      throw exExceptionMessage("E488", trailingCharacters)
    }

    if (functionNamePrefix != null) {
      if (functionNamePrefix.equals("<SID>", ignoreCase = true)) {
        // IdeaVim doesn't currently support <SID> prefix, which would be translated to a `<SNR>_{number}` prefix. It's
        // also not valid from the Command-line, which isn't a script context. (It's not clear if the function command
        // is supported inside a script context - why would you want to output a list of functions from a script?)
        throw exExceptionMessage("E81")
      }
      else if (!functionNamePrefix.equals("<lambda>", ignoreCase = true)) {
        // TODO: Support <SNR> prefix to list script-local functions (which are saved as global functions)
        throw exExceptionMessage("E129")
      }
    }

    if (pattern != null) {
      printAllMatchingFunctions(pattern, editor, context)
    }
    else if (literalDictionaryKey != null && functionName != null) {
      // The text is a literal dictionary expression, but the function name is still a curly brace expression. We need
      // to evaluate that name, then concatenate the literal dictionary key, and then finally evaluate that to a FuncRef
      if (literalDictionaryKey.isBlank()) {
        throw exExceptionMessage("E713")
      }

      val nameValue = functionName.evaluate(editor, context, this).value
      val dictFuncName = "$nameValue.$literalDictionaryKey"
      val dictFuncExpression = injector.vimscriptParser.parseExpression(dictFuncName)
        ?: throw exExceptionMessage("E716", dictFuncName)
      val result = dictFuncExpression.evaluate(editor, context, this)
      if (result !is VimFuncref) {
        throw exExceptionMessage("E718")
      }
      if (result.handler !is DefinedFunctionHandler) {
        throw exExceptionMessage("E123", dictFuncName)
      }

      // Finally, we check for trailing characters
      if (trailingCharacters?.isNotBlank() == true) {
        throw exExceptionMessage("E488", trailingCharacters)
      }

      printFunctions(listOf(result.handler.function), editor, context)
    } else if (functionName != null) {
      val lambdaPrefix = if (functionNamePrefix.equals("<lambda>")) functionNamePrefix else ""
      val nameValue = lambdaPrefix + functionName.evaluate(editor, context, this).value
      if (nameValue.isEmpty()) {
        printAllFunctions(editor, context)
      } else {
        val (scope, name) = Scope.split(nameValue)
        if (scope == Scope.SCRIPT_VARIABLE) {
          throw exExceptionMessage("E81")
        }
        if (name.isBlank() || name.startsWith('#') || name[0].isDigit()) {
          throw exExceptionMessage("E129")
        }
        if (scope != null && scope != Scope.GLOBAL_VARIABLE) {
          throw exExceptionMessage("E884", functionName.originalString)
        }
        val colon = nameValue.indexOf(':')
        if (colon != -1 && scope != Scope.GLOBAL_VARIABLE) {
          throw exExceptionMessage("E488", nameValue.substring(colon))
        }
        if (name[0].isLowerCase() && !name.contains('#')) {
          throw exExceptionMessage("E128", functionName.originalString)
        }

        printMatchingFunction(scope, name, editor, context)
      }
    } else {
      printAllFunctions(editor, context)
    }

    return ExecutionResult.Success
  }

  private fun printAllFunctions(editor: VimEditor, context: ExecutionContext) {
    val functions = injector.functionService.getAllUserDefinedFunctions()
      .filterNot { it.name.startsWith("<lambda>") }
      .sortedBy { it.name }
    printFunctions(functions, editor, context)
  }

  private fun printAllMatchingFunctions(pattern: String, editor: VimEditor, context: ExecutionContext) {
    if (pattern.isEmpty()) {
      printAllFunctions(editor, context)
      return
    }
    try {
      val regex = VimRegex(pattern)

      // Vim uses `rm_ic = p_ic` when matching function names, so only 'ignorecase' applies - 'smartcase' does not.
      // A `\c` or `\C` in the pattern still overrides this
      val options = enumSetOf<VimRegexOptions>()
      if (injector.globalOptions().ignorecase) options.add(VimRegexOptions.IGNORE_CASE)

      val functions = injector.functionService.getAllUserDefinedFunctions()
        .sortedBy { it.name }
        .filter { regex.containsMatchIn(it.name, options) }
      printFunctions(functions, editor, context)
    }
    catch (e: VimRegexException) {
      // TODO: VimRegex should throw more appropriate exceptions. E.g. `E54: Unmatched \(`
      if (e.message.contains("E383")) {
        throw exExceptionMessage("E383", pattern)
      }
      else throw e
    }
  }

  private fun printMatchingFunction(scope: Scope?, name: String, editor: VimEditor, context: ExecutionContext) {
    val function = if (name.startsWith("<lambda>")) {
      injector.functionService.getAllUserDefinedFunctions().firstOrNull { it.name == name }
    } else {
      injector.functionService.getUserDefinedFunction(scope, name, CommandLineVimLContext)
    } ?: throw exExceptionMessage("E123", name)
    printFunctions(listOf(function), editor, context)
  }

  private fun printFunctions(functions: List<FunctionDeclaration>, editor: VimEditor, context: ExecutionContext) {
    val text = buildString {
      functions.forEach { func ->
        append("function ")
        append(func.name)
        append("(")
        func.args.forEach {
          append(it)
          append(", ")
        }
        func.defaultArgs.forEach {
          append(it.first)
          append(" = ")
          append(it.second.originalString)
          append(", ")
        }
        if (func.hasOptionalArguments) {
          append("...")
        }
        else if (func.args.isNotEmpty() || func.defaultArgs.isNotEmpty()) {
          delete(length - 2, length)
        }
        append(")")
        if (func.flags.contains(FunctionFlag.ABORT)) {
          append(" abort")
        }
        if (func.flags.contains(FunctionFlag.RANGE)) {
          append(" range")
        }
        if (func.flags.contains(FunctionFlag.DICT)) {
          append(" dict")
        }
        // We don't add "closure", because that only applies to local functions, which we don't list
        appendLine()
      }
    }
    injector.outputPanel.output(editor, context, text)
  }
}
