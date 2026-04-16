/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.autocmd

/**
 * Vim-style file pattern for autocmd matching.
 *
 * Supports glob patterns:
 * - `*` matches any characters except path separators
 * - `**` matches any characters including path separators
 * - `?` matches a single non-separator character
 * - `[abc]` matches any character in the set
 * - `{foo,bar}` matches "foo" or "bar"
 *
 * If the pattern contains `/`, it matches against the full path.
 * Otherwise, it matches against only the file name.
 */
class AutoCmdPattern(val pattern: String) {

  private val matchesAll = pattern == "*"
  private val matchesFullPath = '/' in pattern || '\\' in pattern
  private val regex: Regex by lazy { toRegex(pattern) }

  fun matches(filePath: String?): Boolean {
    if (matchesAll) return true
    if (filePath == null) return false

    val target = if (matchesFullPath) filePath else fileName(filePath)
    return regex.matches(target)
  }

  private fun fileName(path: String): String {
    return path.substringAfterLast('/').substringAfterLast('\\')
  }

  companion object {
    private const val REGEX_SPECIAL = "\\+^$|()"

    private fun toRegex(pattern: String): Regex {
      val result = StringBuilder("^")
      var i = 0
      var inGroup = false

      while (i < pattern.length) {
        when (val ch = pattern[i]) {
          '*' -> if (isDoubleStar(pattern, i)) {
            result.append(".*")
            i++
          } else {
            result.append("[^/\\\\]*")
          }

          '?' -> result.append("[^/\\\\]")
          '.' -> result.append("\\.")
          '{' -> {
            result.append("(?:"); inGroup = true
          }

          '}' -> {
            result.append(")"); inGroup = false
          }

          ',' -> if (inGroup) result.append("|") else result.append(",")
          '[' -> result.append("[")
          ']' -> result.append("]")
          in REGEX_SPECIAL -> {
            result.append("\\"); result.append(ch)
          }

          else -> result.append(ch)
        }
        i++
      }

      result.append("$")
      return Regex(result.toString())
    }

    private fun isDoubleStar(pattern: String, i: Int): Boolean {
      return i + 1 < pattern.length && pattern[i + 1] == '*'
    }
  }
}
