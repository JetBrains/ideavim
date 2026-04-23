/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

/** See `:help 'comments'` for the input format. */
object CommentLeaderParser {
  private val flagChars: Map<Char, CommentLeader.Flag> = mapOf(
    'b' to CommentLeader.Flag.BLANK_REQUIRED,
    'f' to CommentLeader.Flag.NO_CONTINUATION,
    's' to CommentLeader.Flag.START,
    'm' to CommentLeader.Flag.MIDDLE,
    'e' to CommentLeader.Flag.END,
    'x' to CommentLeader.Flag.END_SHORTCUT,
    'n' to CommentLeader.Flag.NESTED,
    'l' to CommentLeader.Flag.LEFT_ALIGN,
    'r' to CommentLeader.Flag.RIGHT_ALIGN,
    'O' to CommentLeader.Flag.NO_OPEN_BELOW,
  )

  private val trailingSignedInt = Regex("-?\\d+$")

  fun parse(input: String): List<CommentLeader> {
    if (input.isEmpty()) return emptyList()
    return splitOnUnescapedCommas(input).mapNotNull { parseEntry(it) }
  }

  private fun splitOnUnescapedCommas(input: String): List<String> {
    val entries = mutableListOf(StringBuilder())
    var escaped = false
    for (c in input) {
      when {
        escaped -> {
          entries.last().append(c)
          escaped = false
        }

        c == '\\' -> escaped = true
        c == ',' -> entries.add(StringBuilder())
        else -> entries.last().append(c)
      }
    }
    return entries.map { it.toString() }
  }

  private fun parseEntry(entry: String): CommentLeader? {
    val colonIndex = entry.indexOf(':')
    if (colonIndex < 0) return null
    val flagsStr = entry.substring(0, colonIndex)
    val text = entry.substring(colonIndex + 1)
    return CommentLeader(
      text = text,
      flags = parseFlags(flagsStr),
      offset = parseOffset(flagsStr),
    )
  }

  private fun parseFlags(flagsStr: String): Set<CommentLeader.Flag> =
    flagsStr.mapNotNull { flagChars[it] }.toSet()

  private fun parseOffset(flagsStr: String): Int =
    trailingSignedInt.find(flagsStr)?.value?.toIntOrNull() ?: 0
}

data class CommentLeader(
  val text: String,
  val flags: Set<Flag> = emptySet(),
  val offset: Int = 0,
) {
  val isStart: Boolean get() = Flag.START in flags
  val isMiddle: Boolean get() = Flag.MIDDLE in flags
  val isEnd: Boolean get() = Flag.END in flags
  val requiresBlank: Boolean get() = Flag.BLANK_REQUIRED in flags
  val hasNoContinuation: Boolean get() = Flag.NO_CONTINUATION in flags
  val isNested: Boolean get() = Flag.NESTED in flags

  enum class Flag {
    BLANK_REQUIRED,
    NO_CONTINUATION,
    START,
    MIDDLE,
    END,
    END_SHORTCUT,
    NESTED,
    LEFT_ALIGN,
    RIGHT_ALIGN,
    NO_OPEN_BELOW,
  }
}
