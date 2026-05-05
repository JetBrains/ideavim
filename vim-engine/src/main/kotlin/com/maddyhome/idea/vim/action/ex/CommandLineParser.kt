/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

/**
 * Parsed completion context for a partially-typed ex command line. Used for Tab completion
 * context detection.
 *
 *  - [CommandNameCompletionContext]: the user is still typing the command name
 *    (e.g. `:vs` -> complete to `:vsplit`).
 *  - [ArgumentCompletionContext]: a command name plus a separator has been typed,
 *    so completion targets the argument (e.g. `:edit foo` -> complete file paths).
 */
internal sealed interface CommandLineCompletionContext {
  val completionStart: Int
}

internal data class CommandNameCompletionContext(
  val prefix: String,
  override val completionStart: Int,
) : CommandLineCompletionContext

internal data class ArgumentCompletionContext(
  val commandName: String,
  val argumentPrefix: String,
  override val completionStart: Int,
) : CommandLineCompletionContext

internal fun parseCommandLineForCompletion(text: String): CommandLineCompletionContext? {
  val trimmed = text.trimStart()
  if (trimmed.isEmpty()) return null

  val commandName = extractCommandName(trimmed) ?: return null
  val leadingSpacesLength = text.length - trimmed.length

  return if (isCommandNameOnly(trimmed, commandName)) {
    parseCommandNameContext(commandName, leadingSpacesLength)
  } else {
    parseArgumentContext(trimmed, commandName, leadingSpacesLength)
  }
}

private fun isCommandNameOnly(trimmed: String, commandName: String): Boolean =
  commandName.length == trimmed.length

private fun parseCommandNameContext(commandName: String, leadingSpacesLength: Int): CommandNameCompletionContext? {
  if (isExplicitBangForm(commandName)) return null
  return CommandNameCompletionContext(commandName, leadingSpacesLength)
}

private fun parseArgumentContext(
  trimmed: String,
  commandName: String,
  leadingSpacesLength: Int,
): ArgumentCompletionContext? {
  val commandLength = commandName.length
  if (!hasArgumentSeparator(trimmed, commandLength)) return null

  val argStart = skipSpaces(trimmed, commandLength)
  val argPrefix = trimmed.substring(argStart)
  return ArgumentCompletionContext(commandName, argPrefix, leadingSpacesLength + argStart)
}

/**
 * A trailing bang means the user has committed to a specific command form;
 * completing the name (e.g. `vs!` -> `vsplit`) would silently change their intent.
 */
private fun isExplicitBangForm(commandName: String): Boolean = commandName.endsWith('!')

private fun extractCommandName(text: String): String? {
  var end = 0
  while (end < text.length && (text[end].isLetter() || text[end] == '!')) {
    end++
  }
  if (end == 0) return null
  return text.substring(0, end)
}

private fun hasArgumentSeparator(text: String, offset: Int): Boolean {
  return offset < text.length && text[offset] == ' '
}

private fun skipSpaces(text: String, from: Int): Int {
  var i = from
  while (i < text.length && text[i] == ' ') i++
  return i
}
