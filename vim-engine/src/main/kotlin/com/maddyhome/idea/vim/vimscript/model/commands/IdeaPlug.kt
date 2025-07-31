/*
 * Copyright 2003-2025 The IdeaVim authors
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
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Command that is used for enabling plugins. E.g. `IdeaPlug 'ReplaceWithRegister'`
 */
@ExCommand(command = "IdeaPlug")
class IdeaPlug(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (argument.isEmpty()) return ExecutionResult.Error

    val firstChar = argument.firstOrNull() ?: return ExecutionResult.Error
    if (firstChar != '"' && firstChar != '\'') return ExecutionResult.Error

    val regex = "\"([^\"]+)\"|'([^']+)'"
    val name = Regex(regex).findAll(argument)
      .map { match ->
        match.groupValues[1].ifEmpty { match.groupValues[2] }
      }
      .firstOrNull() ?: return ExecutionResult.Error

    EnabledExtensions.addExtension(name)
    val extension = injector.jsonExtensionProvider.getExtension(name) ?: return ExecutionResult.Error
    injector.extensionLoader.enableExtension(extension)

    return ExecutionResult.Success
  }

  companion object {
    object EnabledExtensions {
      private val _enabledExtensions = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
      val enabledExtensions: List<String> get() = _enabledExtensions.toList()

      fun clearExtensions() {
        _enabledExtensions.clear()
      }

      fun addExtension(name: String) {
        _enabledExtensions.add(name)
      }
    }
  }
}