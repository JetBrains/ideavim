/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.VimApi
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineStringHelper.toPrintableCharacters

/**
 * `:YRShow` - renders the ring the way `s:YRShow` does when `g:yankring_window_use_separate` is
 * off: a banner, a column header, then one line per entry. The separate window comes later.
 */
internal suspend fun VimApi.showYankRing() {
  val elements = YankRing.entries().mapIndexed { index, entry -> formatElement(index + 1, entry) }

  outputPanel {
    setText((listOf(BANNER, COLUMN_HEADER) + elements).joinToString("\n"))
  }
}

/**
 * Formats one entry the way `s:YRDisplayElem` does.
 */
private fun formatElement(number: Int, entry: YankRingEntry): String =
  number.toString().padEnd(ELEM_COLUMN_WIDTH) + printableContent(entry.text)

/**
 * Newlines are shown as a literal `\n` and the remaining control characters in caret notation, so
 * that a multi-line entry still occupies a single row of the listing.
 */
private fun printableContent(text: String): String =
  toPrintableCharacters(injector.parser.stringToKeys(text.replace("\n", "\\n")))

private const val BANNER = "--- YankRing ---"

// `s:YRDisplayElem` left-aligns the element number in a six-character column, and the header lines
// up with it.
private const val ELEM_COLUMN_WIDTH = 6
private val COLUMN_HEADER = "Elem".padEnd(ELEM_COLUMN_WIDTH) + "Content"
