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
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.ex.ranges.Range

/**
 * `:[range]S /lhs/rhs/[flags]` — abolish's case-aware substitute (also reachable as `:Subvert`).
 * `:S /pattern/` and `:S ?pattern?` — case-aware forward / backward search.
 *
 * The lhs is still matched literally — vim regex atoms aren't honoured, and
 * `\1`-style backreferences in the rhs are stored verbatim in the dictionary.
 * Abolish-specific flags `I`, `v`, `w` are silently dropped.
 */
internal class SubvertCommand : CommandAliasHandler {

  override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
    when (val invocation = SubvertInvocation.parse(command) ?: return) {
      is SubvertInvocation.Substitute -> executeSubstitute(invocation, range, editor, context)
      is SubvertInvocation.Search -> executeSearch(invocation, editor)
    }
  }
}

private fun executeSubstitute(
  invocation: SubvertInvocation.Substitute,
  range: Range,
  editor: VimEditor,
  context: ExecutionContext,
) {
  val dictionary = buildVariantDictionary(invocation.lhsPattern, invocation.rhsPattern)
  val lineRange = range.getLineRange(editor, editor.primaryCaret())
  val rangePrefix = "${lineRange.startLine1},${lineRange.endLine1}"
  runVariantSubstitution(editor, context, dictionary, rangePrefix, invocation.flags)
}

private fun executeSearch(invocation: SubvertInvocation.Search, editor: VimEditor) {
  val dictionary = buildVariantDictionary(invocation.pattern, "")
  runVariantSearch(editor, dictionary.keys, invocation.direction)
}

private sealed interface SubvertInvocation {

  data class Substitute(val lhsPattern: String, val rhsPattern: String, val flags: String) : SubvertInvocation

  data class Search(val pattern: String, val direction: Direction) : SubvertInvocation

  companion object {
    fun parse(commandLine: String): SubvertInvocation? {
      val arguments = commandLine.trim().substringAfter(' ', missingDelimiterValue = "").trim()
      if (arguments.length < 2) return null
      val delimiter = arguments[0]
      val parts = arguments.drop(1).split(delimiter)
      val lhs = parts.firstOrNull().orEmpty()
      if (lhs.isEmpty()) return null

      val direction = if (delimiter == '?') Direction.BACKWARDS else Direction.FORWARDS
      if (parts.hasNoReplacement()) return Search(lhs, direction)

      val rhs = parts[1]
      val flags = parts.getOrNull(2).orEmpty()
      return Substitute(lhs, rhs, flags)
    }

    private fun List<String>.hasNoReplacement(): Boolean =
      size == 1 || (size == 2 && this[1].isEmpty())
  }
}
