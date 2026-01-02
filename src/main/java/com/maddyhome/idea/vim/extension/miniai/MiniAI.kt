/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.miniai

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.models.Range
import com.intellij.vim.api.scopes.TextObjectRange
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.api

/**
 * A simplified imitation of mini.ai approach for motions "aq", "iq", "ab", "ib".
 * Instead of "closest" text object, we apply a "cover-or-next" logic:
 *
 * 1) Among all candidate pairs, pick any that cover the caret (start <= caret < end).
 *    Among those, pick the smallest range.
 * 2) If none cover the caret, pick the "next" pair (start >= caret) that is closest.
 * 3) If none are next, pick the "previous" pair (end <= caret) that is closest.
 *
 * For "i" text object, we shrink the boundaries inward by one character on each side.
 */
class MiniAI : VimExtension {

  override fun getName() = "mini-ai"

  override fun init() {
    val api = api()
    api.textObjects {
      register("aq", preserveSelectionAnchor = false) { _ ->
        findQuoteRange(isOuter = true)
      }
      register("iq", preserveSelectionAnchor = false) { _ ->
        findQuoteRange(isOuter = false)
      }
      register("ab", preserveSelectionAnchor = false) { _ ->
        findBracketRange(isOuter = true)
      }
      register("ib", preserveSelectionAnchor = false) { _ ->
        findBracketRange(isOuter = false)
      }
    }
  }
}

/* -------------------------------------------------------------------------
 * Core mini.ai-like logic
 * ------------------------------------------------------------------------- */

/**
 * Find a text range for quotes (", ', `) around the caret using a "cover-or-next" approach.
 * - If one or more pairs **cover** the caret, pick the *smallest* covering pair.
 * - Else pick the "next" pair whose start >= caret offset (closest).
 * - Else pick the "previous" pair whose end <= caret offset (closest).
 *
 * If [isOuter] == false (i.e. 'iq'), shrink the final range by 1 char on each side.
 */
private fun VimApi.findQuoteRange(isOuter: Boolean): TextObjectRange? {
  val text = editor { read { text } }
  val caretOffset = editor { read { withPrimaryCaret { offset } } }
  val caretLine = editor { read { withPrimaryCaret { line.number } } }

  // 1) Gather quotes in *this caret's line*
  val lineStart = editor { read { getLineStartOffset(caretLine) } }
  val lineEnd = editor { read { getLineEndOffset(caretLine, true) } }
  val lineText = text.substring(lineStart, lineEnd)
  val lineRanges = gatherAllQuoteRanges(lineText).map {
    Range.Simple(it.start + lineStart, it.end + lineStart)
  }

  val localBest = pickBestRange(lineRanges, caretOffset)
  if (localBest != null) {
    return adjustRangeForInnerOuter(localBest, isOuter)
  }

  // 2) Fallback: entire buffer
  val allRanges = gatherAllQuoteRanges(text)
  val bestOverall = pickBestRange(allRanges, caretOffset) ?: return null
  return adjustRangeForInnerOuter(bestOverall, isOuter)
}

/** Adjust final range if user requested 'inner' (i.e. skip bounding chars). */
private fun adjustRangeForInnerOuter(range: Range.Simple, isOuter: Boolean): TextObjectRange? {
  if (isOuter) return TextObjectRange.CharacterWise(range.start, range.end)
  // For 'inner', skip bounding chars if possible
  if (range.end - range.start < 2) return null
  return TextObjectRange.CharacterWise(range.start + 1, range.end - 1)
}

/**
 * Gather all "balanced" pairs for single/double/backtick quotes in the entire text.
 * For simplicity, we treat each ["]...["] or [']...['] or [`]...[`] as one range,
 * ignoring complicated cases of escaping, multi-line, etc.
 */
private fun gatherAllQuoteRanges(text: CharSequence): List<Range.Simple> {
  val results = mutableListOf<Range.Simple>()
  val patterns = listOf(
    "\"([^\"]*)\"",
    "'([^']*)'",
    "`([^`]*)`"
  )
  for (p in patterns) {
    Regex(p).findAll(text).forEach {
      results.add(Range.Simple(it.range.first, it.range.last + 1))
    }
  }
  return results
}

/**
 * Find a text range for brackets using a "cover-or-next" approach.
 * We treat bracket pairs ( (), [], {}, <> ) in a naive balanced scanning way.
 * If [isOuter] is false, we shrink boundaries to skip the bracket chars.
 */
private fun VimApi.findBracketRange(isOuter: Boolean): TextObjectRange? {
  val text = editor { read { text } }
  val caretOffset = editor { read { withPrimaryCaret { offset } } }
  val caretLine = editor { read { withPrimaryCaret { line.number } } }

  // 1) Gather bracket pairs in *this caret's line*
  val lineStart = editor { read { getLineStartOffset(caretLine) } }
  val lineEnd = editor { read { getLineEndOffset(caretLine, false) } }
  val bracketChars = listOf('(', ')', '[', ']', '{', '}', '<', '>')
  // Gather local line bracket pairs
  val lineText = text.substring(lineStart, lineEnd)
  val lineRanges = gatherAllBracketRanges(lineText, bracketChars).map {
    // Shift each range's offsets to the global text
    Range.Simple(it.start + lineStart, it.end + lineStart)
  }

  // Pick the best match on this line
  val localBest = pickBestRange(lineRanges, caretOffset)
  if (localBest != null) {
    return adjustRangeForInnerOuter(localBest, isOuter)
  }

  // 2) Fallback: gather bracket pairs in the entire file
  val allRanges = gatherAllBracketRanges(text, bracketChars)
  val bestOverall = pickBestRange(allRanges, caretOffset) ?: return null

  return adjustRangeForInnerOuter(bestOverall, isOuter)
}

/**
 * Gathers naive "balanced bracket" ranges for the given bracket pairs.
 * This is a simplified stack-based approach scanning the entire file text.
 */
private fun gatherAllBracketRanges(
  text: CharSequence,
  brackets: List<Char>
): List<Range.Simple> {
  val pairs = mapOf('(' to ')', '[' to ']', '{' to '}', '<' to '>')
  val results = mutableListOf<Range.Simple>()
  val stack = ArrayDeque<Int>()         // offsets of open bracket
  val bracketTypeStack = ArrayDeque<Char>() // store which bracket

  text.forEachIndexed { i, ch ->
    if (pairs.containsKey(ch)) {
      // Opening bracket
      stack.addLast(i)
      bracketTypeStack.addLast(ch)
    } else {
      // Maybe a closing bracket?
      val top = bracketTypeStack.lastOrNull() ?: '\u0000'
      if (pairs[top] == ch) {
        // Balanced pair
        val openPos = stack.removeLast()
        bracketTypeStack.removeLast()
        results.add(Range.Simple(openPos, i + 1)) // i+1 for end
      }
    }
  }
  return results
}

/**
 * Picks best range among [candidates] in a "cover-or-next" approach:
 *   1) Among those covering [caretOffset], pick the narrowest.
 *   2) Else pick the "next" bracket whose start >= caret, if any (closest).
 *   3) Else pick the "previous" bracket whose end <= caret, if any (closest).
 */
private fun pickBestRange(candidates: List<Range.Simple>, caretOffset: Int): Range.Simple? {
  if (candidates.isEmpty()) return null
  val covering = mutableListOf<Range.Simple>()
  val nextOnes = mutableListOf<Range.Simple>()
  val prevOnes = mutableListOf<Range.Simple>()

  for (r in candidates) {
    if (r.start <= caretOffset && caretOffset < r.end) {
      covering.add(r)
    } else if (r.start >= caretOffset) {
      nextOnes.add(r)
    } else if (r.end <= caretOffset) {
      prevOnes.add(r)
    }
  }

  // 1) Covering, smallest width
  if (covering.isNotEmpty()) {
    return covering.minByOrNull { it.end - it.start }
  }

  // 2) Next (closest by start)
  if (nextOnes.isNotEmpty()) {
    return nextOnes.minByOrNull { kotlin.math.abs(it.start - caretOffset) }
  }

  // 3) Previous (closest by end)
  if (prevOnes.isNotEmpty()) {
    return prevOnes.minByOrNull { kotlin.math.abs(it.end - caretOffset) }
  }

  return null
}
