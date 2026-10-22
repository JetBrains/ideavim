/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.grep

import java.nio.charset.StandardCharsets

/**
 * Parses `git grep -n --column` output lines, like fugitive's `s:GrepParseLine`.
 */
internal object GitGrepOutputParser {

  /** `path:line:column:text`. The column is missing when git is older than 2.19 */
  private val LINE_REGEX = Regex("""^(.+?):([1-9]\d*):(?:([1-9]\d*):)?(.*)$""")

  /**
   * Lines without a position (binary files, `--count`, `--name-only`) are skipped.
   *
   * @param workTree the directory git ran in; paths in the output are relative to it
   */
  fun parse(lines: List<String>, workTree: String, protocol: String): List<GrepMatch> {
    val root = workTree.trimEnd('/')
    return lines.mapNotNull { parseLine(it, root, protocol) }
  }

  private fun parseLine(line: String, root: String, protocol: String): GrepMatch? {
    val (path, lineNumber, byteColumn, text) = LINE_REGEX.matchEntire(line)?.destructured ?: return null
    return GrepMatch(
      filepath = "$root/$path",
      protocol = protocol,
      line = lineNumber.toInt() - 1,
      col = if (byteColumn.isEmpty()) 0 else charColumn(text, byteColumn.toInt() - 1),
      text = text,
    )
  }

  /**
   * git reports the column as a byte offset into the UTF-8 encoded line.
   */
  private fun charColumn(text: String, byteColumn: Int): Int {
    val bytes = text.toByteArray(StandardCharsets.UTF_8)
    return String(bytes, 0, byteColumn.coerceIn(0, bytes.size), StandardCharsets.UTF_8).length
  }
}
