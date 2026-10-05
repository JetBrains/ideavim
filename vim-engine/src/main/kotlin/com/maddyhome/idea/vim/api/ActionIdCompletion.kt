/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Completion of IDE action IDs, shared by the command line (`<Tab>`) and the ideavimrc editor (IDE lookup).
 *
 * Both places look at the text before the caret and complete the action ID that is being typed:
 *  - an unclosed `<Action>(` notation, e.g. `nmap x <Action>(Ref`
 *  - an argument of `:action` or `:actionlist`, e.g. `action Ref` or `nmap x :action Ref`
 *
 * Matching is a case-insensitive prefix match, so `reformat` finds `ReformatCode`.
 */
object ActionIdCompletion {

  /** Key notation is case-insensitive, so `<action>(` works too */
  private val ACTION_NOTATION_REGEX = Regex("<action>\\(([^()\\s]*)$", RegexOption.IGNORE_CASE)

  /** `:action` / `:actionl[ist]` at the start of the text, after `:` (mapping RHS) or after a `|` command separator */
  private val ACTION_COMMAND_REGEX = Regex("(?:^|[:|])[\\s:]*action(?:l(?:i(?:st?)?)?)?\\s+([^\\s<|]*)$")

  /**
   * The action ID being typed: its [prefix] and the offset where it starts in the analysed text.
   */
  data class Prefix(val prefix: String, val startOffset: Int)

  /**
   * Finds the action ID being typed at the end of [textBeforeCaret], or null if the caret is not on an action ID.
   *
   * [textBeforeCaret] is the command line text, or the ideavimrc line up to the caret.
   */
  fun findPrefix(textBeforeCaret: String): Prefix? {
    if (textBeforeCaret.trimStart().startsWith('"')) return null

    val match = ACTION_NOTATION_REGEX.find(textBeforeCaret) ?: ACTION_COMMAND_REGEX.find(textBeforeCaret) ?: return null
    val prefix = match.groups[1] ?: return null
    return Prefix(prefix.value, prefix.range.first)
  }

  /**
   * Returns the [actionIds] matching [prefix], sorted case-insensitively.
   */
  fun findMatches(actionIds: Collection<String>, prefix: String): List<String> {
    return actionIds
      .filter { it.startsWith(prefix, ignoreCase = true) }
      .distinct()
      .sortedWith(String.CASE_INSENSITIVE_ORDER)
  }
}
