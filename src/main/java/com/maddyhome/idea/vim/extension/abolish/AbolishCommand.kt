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
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.key.AbbreviationEntry

class AbolishCommand : CommandAliasHandler {
  override fun execute(
    command: String,
    range: Range,
    editor: VimEditor,
    context: ExecutionContext,
  ) {
    val trimmed = command.trim()
    val head = trimmed.substringBefore(' ', missingDelimiterValue = trimmed)
    val bang = head.lastOrNull() == '!'
    val arguments = trimmed.substringAfter(' ', missingDelimiterValue = "").trim()
    when (val invocation = AbolishInvocation.parse(arguments, bang) ?: return) {
      is AbolishInvocation.AbolishAdd -> applyAdd(invocation, editor)
      is AbolishInvocation.AbolishDelete -> applyDelete(invocation)
      is AbolishInvocation.AbolishSearch -> applySearch(invocation, editor)
      is AbolishInvocation.AbolishSubstitute -> applySubstitute(invocation, editor, context)
    }
  }

  private fun applySearch(invocation: AbolishInvocation.AbolishSearch, editor: VimEditor) {
    runVariantSearch(editor, buildVariantKeys(invocation.pattern), invocation.direction)
  }

  private fun applySubstitute(
    invocation: AbolishInvocation.AbolishSubstitute,
    editor: VimEditor,
    context: ExecutionContext,
  ) {
    val dictionary = buildVariantDictionary(invocation.lhs, invocation.rhs)
    val rangePrefix = if (invocation.currentLineOnly) "." else "%"
    runVariantSubstitution(editor, context, dictionary, rangePrefix, "g")
  }

  private fun applyAdd(invocation: AbolishInvocation.AbolishAdd, editor: VimEditor) {
    val dictionary = buildVariantDictionary(invocation.lhs, invocation.rhs)
    if (dictionary.isEmpty()) return
    val mode = if (invocation.cmdline) MappingMode.CMD_LINE else MappingMode.INSERT
    val modes = setOf(mode)
    dictionary.forEach { (lhs, rhs) ->
      val entry = AbbreviationEntry(lhs, rhs, modes = modes, recursive = false, expression = false)
      if (invocation.bufferLocal) {
        injector.abbreviationGroup.setBufferLocalAbbreviation(entry, editor)
      } else {
        injector.abbreviationGroup.setAbbreviation(entry, editor)
      }
    }
  }

  private fun applyDelete(invocation: AbolishInvocation.AbolishDelete) {
    buildVariantKeys(invocation.lhs).forEach { key ->
      injector.abbreviationGroup.removeAbbreviation(key, modes = setOf(MappingMode.INSERT))
    }
  }
}

private sealed interface AbolishInvocation {
  data class AbolishAdd(
    val lhs: String,
    val rhs: String,
    val bufferLocal: Boolean,
    val cmdline: Boolean,
  ) : AbolishInvocation

  data class AbolishDelete(val lhs: String) : AbolishInvocation
  data class AbolishSearch(val pattern: String, val direction: Direction) : AbolishInvocation
  data class AbolishSubstitute(val lhs: String, val rhs: String, val currentLineOnly: Boolean) : AbolishInvocation

  companion object {
    fun parse(arguments: String, bang: Boolean): AbolishInvocation? {
      if (arguments.length < 2) return null

      val (flags, remainder) = stripFlags(arguments)

      if (Flag.DELETE in flags) {
        val lhs = remainder.trim()
        return lhs.takeIf { it.isNotEmpty() }?.let(::AbolishDelete)
      }

      if (Flag.SEARCH in flags) {
        val pattern = remainder.trim()
        return pattern.takeIf { it.isNotEmpty() }
          ?.let { AbolishSearch(it, if (bang) Direction.BACKWARDS else Direction.FORWARDS) }
      }

      if (Flag.SUBSTITUTE in flags) {
        val (lhs, rhs) = splitLhsRhs(remainder) ?: return null
        return AbolishSubstitute(lhs, rhs, currentLineOnly = bang)
      }

      val (lhs, rhs) = splitLhsRhs(remainder) ?: return null
      return AbolishAdd(
        lhs = lhs,
        rhs = rhs,
        bufferLocal = Flag.BUFFER in flags,
        cmdline = Flag.CMDLINE in flags,
      )
    }

    private fun stripFlags(arguments: String): Pair<Set<Flag>, String> {
      val flags = mutableSetOf<Flag>()
      var remainder = arguments
      while (true) {
        val token = remainder.substringBefore(' ', missingDelimiterValue = remainder)
        val flag = Flag.entries.firstOrNull { token == it.token } ?: break
        flags += flag
        remainder = remainder.removePrefix(token).trimStart()
        if (flag.terminal) return flags to remainder
      }
      return flags to remainder
    }

    private fun splitLhsRhs(remainder: String): Pair<String, String>? {
      val parts = remainder.split(' ', limit = 2)
      val lhs = parts[0]
      val rhs = parts.getOrNull(1).orEmpty()
      return if (lhs.isEmpty() || rhs.isEmpty()) null else lhs to rhs
    }
  }
}

private enum class Flag(val token: String, val terminal: Boolean = false) {
  DELETE("-delete", terminal = true),
  SEARCH("-search", terminal = true),
  SUBSTITUTE("-substitute", terminal = true),
  BUFFER("-buffer"),
  CMDLINE("-cmdline"),
}
