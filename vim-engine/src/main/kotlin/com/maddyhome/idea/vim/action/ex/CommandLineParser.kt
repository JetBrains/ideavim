/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

/**
 * Lightweight parser for extracting the command name and argument prefix
 * from a partially-typed ex command line. Used for Tab completion context detection.
 */
internal data class ParsedCommandLine(
  val commandName: String,
  val argumentPrefix: String,
  val completionStart: Int,
)

internal fun parseCommandLineForCompletion(text: String): ParsedCommandLine? {
  val trimmed = text.trimStart()
  if (trimmed.isEmpty()) return null

  val commandName = extractCommandName(trimmed) ?: return null
  val commandEnd = commandName.length

  if (!hasArgumentSeparator(trimmed, commandEnd)) return null

  val argStart = skipSpaces(trimmed, commandEnd)
  val argPrefix = trimmed.substring(argStart)
  val leadingSpaces = text.length - trimmed.length

  return ParsedCommandLine(commandName, argPrefix, leadingSpaces + argStart)
}

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
