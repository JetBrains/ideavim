/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.abolish

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.ex.ranges.Range

/**
 * `:[range]S /lhs/rhs/[flags]` — abolish's case-aware substitute, also reachable as `:Subvert`.
 *
 * Limitations vs. tpope's plugin: flags other than `g` are silently ignored;
 * the lhs is matched literally rather than as a vim regex.
 */
internal class SubvertCommand : CommandAliasHandler {

  override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
    val invocation = SubvertInvocation.parse(command) ?: return
    val dictionary = buildVariantDictionary(invocation.lhsPattern, invocation.rhsPattern)
    if (dictionary.isEmpty()) return
    val matcher = Regex(buildAlternationPattern(dictionary.keys))
    val lineRange = range.getLineRange(editor, editor.primaryCaret())
    // One write action so the whole substitution is one undo step.
    injector.application.runWriteAction {
      applySubstitutionToLines(editor, lineRange.startLine, lineRange.endLine, matcher, dictionary, invocation.replaceAll)
    }
  }
}

private fun applySubstitutionToLines(
  editor: VimEditor,
  startLine: Int,
  endLine: Int,
  matcher: Regex,
  dictionary: Map<String, String>,
  replaceAll: Boolean,
) {
  for (line in startLine..endLine) {
    val (lineStart, lineEnd) = editor.getLineRange(line)
    val lineText = editor.text().substring(lineStart, lineEnd)
    val newLineText = substituteInLine(lineText, matcher, dictionary, replaceAll) ?: continue
    injector.changeGroup.replaceText(editor, editor.primaryCaret(), lineStart, lineEnd, newLineText)
  }
}

private fun substituteInLine(
  lineText: String,
  matcher: Regex,
  dictionary: Map<String, String>,
  replaceAll: Boolean,
): String? {
  val replaceMatch: (MatchResult) -> CharSequence = { match -> dictionary[match.value] ?: match.value }
  val rewritten = if (replaceAll) {
    matcher.replace(lineText, replaceMatch)
  } else {
    val firstMatch = matcher.find(lineText) ?: return null
    val replacement = replaceMatch(firstMatch)
    lineText.replaceRange(firstMatch.range, replacement)
  }
  return rewritten.takeIf { it != lineText }
}

// Longest-first so a prefix key (`box`) can't shadow a longer one (`boxes`).
// `Regex.escape` keeps regex specials in user input harmless. No anchoring —
// vim's `:substitute` doesn't anchor and neither does tpope's `:Subvert`.
private fun buildAlternationPattern(keys: Set<String>): String =
  keys.sortedByDescending(String::length).joinToString(separator = "|", transform = Regex::escape)

private data class SubvertInvocation(
  val lhsPattern: String,
  val rhsPattern: String,
  val replaceAll: Boolean,
) {
  companion object {
    fun parse(commandLine: String): SubvertInvocation? {
      val arguments = commandLine.trim().substringAfter(' ', missingDelimiterValue = "").trim()
      if (arguments.length < 2) return null
      val delimiter = arguments[0]
      val parts = arguments.drop(1).split(delimiter)
      if (parts.size < 2) return null
      val lhs = parts[0]
      val rhs = parts[1]
      val flags = parts.getOrNull(2).orEmpty()
      if (lhs.isEmpty()) return null
      return SubvertInvocation(lhs, rhs, replaceAll = flags.contains('g'))
    }
  }
}
