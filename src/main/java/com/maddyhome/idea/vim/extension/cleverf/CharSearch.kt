/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset

/**
 * A single invocation of the search of [state], with [map] being the effective `f`/`F`/`t`/`T` motion.
 */
internal class CharSearch(
  val state: SearchState,
  map: Char,
  char: Char,
  private val isFirstMove: Boolean,
) {
  private val matcher = CharMatcher(char)
  private val isForward = map.isLowerCase()
  private val isTill = map.equals('t', ignoreCase = true)
  private val step = if (isForward) 1 else -1

  /** In operator-pending mode `f` and `t` include the target character, `F` and `T` don't */
  val isInclusive: Boolean = isForward

  /** Returns the offset [count] matches away from [offset], or null if there are fewer matches */
  fun findTarget(editor: VimEditor, offset: Int, count: Int): Int? {
    val text = editor.text()
    val bounds = searchBounds(editor, offset)
    var position = offset
    for (move in 0 until count) {
      val match = findMatch(text, searchStart(position, move), bounds) ?: return null
      position = targetOf(match)
    }
    return position
  }

  /** Returns the offsets on [line] the search can jump to */
  fun candidatesOnLine(editor: VimEditor, line: Int): List<Int> {
    val text = editor.text()
    val lineRange = editor.getLineStartOffset(line) until editor.getLineEndOffset(line)
    return lineRange.filter { matcher.matches(text[it]) }.map(::targetOf).filter { it in lineRange }
  }

  /**
   * On the first move `t`/`T` accept a match right next to the cursor. On repeats they must skip it, otherwise the
   * cursor would get stuck in front of the same character.
   */
  private fun searchStart(position: Int, move: Int): Int {
    val skipsAdjacentMatch = isTill && !(isFirstMove && move == 0)
    return if (skipsAdjacentMatch) position + step else position
  }

  private fun findMatch(text: CharSequence, start: Int, bounds: IntRange): Int? {
    val candidates = if (isForward) (start + 1)..bounds.last else (start - 1) downTo bounds.first
    return candidates.firstOrNull { matcher.matches(text[it]) }
  }

  private fun targetOf(match: Int): Int = if (isTill) match - step else match

  private fun searchBounds(editor: VimEditor, offset: Int): IntRange =
    if (CleverFSettings.acrossNoLine) {
      editor.getLineStartForOffset(offset) until editor.getLineEndForOffset(offset)
    } else {
      0 until editor.text().length
    }
}
