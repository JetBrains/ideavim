/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.CommandCompletionTypes
import com.maddyhome.idea.vim.api.CommandLineCompletion
import com.maddyhome.idea.vim.api.CommandLineCompletionType
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument

@CommandOrMotion(keys = ["<Tab>"], modes = [Mode.CMD_LINE])
class CommandLineCompletionAction : CommandLineActionHandler() {
  override fun execute(
    commandLine: VimCommandLine,
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
  ): Boolean {
    return performCompletion(commandLine, context, forward = true)
  }

  override fun execute(commandLine: VimCommandLine): Boolean {
    return false
  }
}

@CommandOrMotion(keys = ["<S-Tab>"], modes = [Mode.CMD_LINE])
class CommandLineCompletionBackwardAction : CommandLineActionHandler() {
  override fun execute(
    commandLine: VimCommandLine,
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
  ): Boolean {
    return performCompletion(commandLine, context, forward = false)
  }

  override fun execute(commandLine: VimCommandLine): Boolean {
    return false
  }
}

private fun performCompletion(
  commandLine: VimCommandLine,
  context: ExecutionContext,
  forward: Boolean,
): Boolean {
  if (!commandLine.isExCommand()) return false

  val existing = commandLine.activeCompletion
  if (existing != null && existing.expectedText == commandLine.text) {
    cycleExistingCompletion(commandLine, existing, forward)
    return true
  }

  return startNewCompletion(commandLine, context, forward)
}

private fun cycleExistingCompletion(
  commandLine: VimCommandLine,
  completion: CommandLineCompletion,
  forward: Boolean,
) {
  val match = selectMatch(completion, forward)
  if (match == null) {
    injector.messages.indicateError()
    return
  }

  applyMatch(commandLine, completion, match)
  return
}

private fun startNewCompletion(
  commandLine: VimCommandLine,
  context: ExecutionContext,
  forward: Boolean,
): Boolean {
  commandLine.activeCompletion = null

  val text = commandLine.text
  val parsed = parseCommandLineForCompletion(text) ?: return false
  val matches = findMatches(parsed, context) ?: return false

  if (matches.isEmpty()) {
    injector.messages.indicateError()
    return true
  }

  val completion = CommandLineCompletion(text, parsed.completionStart, matches)
  commandLine.activeCompletion = completion

  val match = selectMatch(completion, forward) ?: return true
  applyMatch(commandLine, completion, match)
  return true
}

private fun findMatches(parsed: ParsedCommandLine, context: ExecutionContext): List<String>? {
  val fullCommandName = injector.vimscriptParser.exCommands.getFullCommandName(parsed.commandName) ?: return null
  val completionType = CommandCompletionTypes.getCompletionType(fullCommandName)
  if (completionType == CommandLineCompletionType.NONE) return null

  return injector.file.listFilesForCompletion(parsed.argumentPrefix, context)
}

private fun selectMatch(completion: CommandLineCompletion, forward: Boolean): String? {
  return if (forward) completion.nextMatch() else completion.previousMatch()
}

private fun applyMatch(commandLine: VimCommandLine, completion: CommandLineCompletion, match: String) {
  val prefix = completion.originalText.substring(0, completion.completionStart)
  val newText = prefix + match
  commandLine.setText(newText)
  commandLine.caret.offset = newText.length
  completion.updateExpectedText(newText)
}
