/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.abolish

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * `:[range]S /lhs/rhs/[flags]` — abolish's case-aware substitute, also reachable as `:Subvert`.
 *
 * Delegates to vim's `:substitute` so users get every standard flag (`g`, `c`, etc.)
 * Tpope uses the same trick: build the variant dictionary, stash it as a global,
 * then run `:s/.../\=get(g:abolish_last_dict, submatch(0), submatch(0))/...`.
 *
 * The lhs is still matched literally — vim regex atoms aren't honoured, and
 * `\1`-style backreferences in the rhs are stored verbatim in the dictionary.
 * Abolish-specific flags `I`, `v`, `w` are silently dropped.
 */
internal class SubvertCommand : CommandAliasHandler {

  override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
    val invocation = SubvertInvocation.parse(command) ?: return
    val dictionary = buildVariantDictionary(invocation.lhsPattern, invocation.rhsPattern)
    if (dictionary.isEmpty()) return

    storeAsLastDict(dictionary)
    val lineRange = range.getLineRange(editor, editor.primaryCaret())
    val substitute = buildSubstituteCommand(dictionary.keys, invocation.flags, lineRange.startLine1, lineRange.endLine1)
    injector.vimscriptExecutor.execute(substitute, editor, context, skipHistory = true, indicateErrors = true, null)
  }
}

/** Pushes the dictionary into `g:abolish_last_dict` so `\=get(...)` can read it. */
private fun storeAsLastDict(dictionary: Map<String, String>) {
  val entries = LinkedHashMap<VimString, VimDataType>()
  dictionary.forEach { (key, value) -> entries[VimString(key)] = VimString(value) }
  VimPlugin.getVariableService().storeGlobalVariable("abolish_last_dict", VimDictionary(entries))
}

private fun buildSubstituteCommand(keys: Set<String>, flags: String, startLine1: Int, endLine1: Int): String {
  val pattern = buildVimAlternationPattern(keys)
  return "${startLine1},${endLine1}s/$pattern/\\=get(g:abolish_last_dict, submatch(0), submatch(0))/$flags"
}

/**
 * Build the alternation pattern in vim's very-magic case-sensitive mode.
 *
 * Sort longest-first so `box` can't shadow `boxes`; escape vim regex specials
 * with the same character set tpope uses in `s:subesc`.
 */
private fun buildVimAlternationPattern(keys: Set<String>): String {
  val alternation = keys.sortedByDescending(String::length).joinToString(separator = "|", transform = ::vimEscape)
  return "\\v\\C%($alternation)"
}

private val VIM_REGEX_SPECIALS = setOf(']', '[', '\\', '/', '.', '*', '+', '?', '~', '%', '(', ')', '&', '|')

private fun vimEscape(input: String): String = buildString {
  input.forEach { c ->
    if (c in VIM_REGEX_SPECIALS) append('\\')
    append(c)
  }
}

private data class SubvertInvocation(
  val lhsPattern: String,
  val rhsPattern: String,
  val flags: String,
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
      return SubvertInvocation(lhs, rhs, flags)
    }
  }
}
