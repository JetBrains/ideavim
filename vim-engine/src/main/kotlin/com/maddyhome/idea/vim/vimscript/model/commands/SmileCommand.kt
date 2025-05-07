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
import com.maddyhome.idea.vim.api.VimVirtualFile
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

@ExCommand(command = "smile")
class SmileCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_FORBIDDEN, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val vimVirtualFile: VimVirtualFile? = editor.getVirtualFile()
    val extension: String? = vimVirtualFile?.extension

    val smileResourcePath: String = extensionToResourceMap[extension] ?: DEFAULT_RESOURCE_PATH

    val smileText: String = runCatching {
      javaClass.getResourceAsStream(smileResourcePath)
        ?.bufferedReader()
        ?.use { it.readText() }
    }.getOrNull() ?: return ExecutionResult.Error

    injector.outputPanel.output(editor, context, smileText)
    return ExecutionResult.Success
  }

  companion object {
    private val extensionToResourceMap: Map<String, String> = mapOf(
      "kt" to KOTLIN_RESOURCE_PATH,
      "kts" to KOTLIN_RESOURCE_PATH,
      "java" to JAVA_RESOURCE_PATH,
      "py" to PYTHON_RESOURCE_PATH
    )

    const val DEFAULT_RESOURCE_PATH = "/ascii-art/default.txt"
    const val KOTLIN_RESOURCE_PATH = "/ascii-art/kotlin.txt"
    const val JAVA_RESOURCE_PATH = "/ascii-art/java.txt"
    const val PYTHON_RESOURCE_PATH = "/ascii-art/python.txt"
  }
}