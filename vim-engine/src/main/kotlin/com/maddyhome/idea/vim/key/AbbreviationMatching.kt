/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper.isKeyword

/**
 * Editor-stable inputs to the abbreviation matcher.
 *
 *   * [text] is the buffer (editor body or cmdline) being scanned.
 *   * [lineStart] is the offset of the first char on the current line — `0` for cmdline, the
 *     current line's start offset for editor buffers.
 *   * [editor] is needed only for the `'iskeyword'` lookup.
 */
internal class AbbreviationContext(
  val text: CharSequence,
  val lineStart: Int,
  val editor: VimEditor,
)

/** A resolved abbreviation that's ready to replace a region in the buffer. */
internal data class AbbreviationExpansion(val lhsRange: TextRange, val rhs: String)

/**
 * Find the lhs candidate immediately to the left of [caretOffset] and, if it matches a registered
 * abbreviation in [mode], return the range to replace plus the resolved rhs.
 *
 * Combines the lhs walk-back with the storage lookup so the two expansion sites (insert mode and
 * cmdline mode) don't repeat the substring/lookup dance.
 */
internal fun findAndResolveAbbreviation(
  ctx: AbbreviationContext,
  caretOffset: Int,
  mode: MappingMode,
): AbbreviationExpansion? {
  val lhsRange = findAbbreviationLhsRange(ctx, caretOffset) ?: return null
  val lhs = ctx.text.subSequence(lhsRange.startOffset, lhsRange.endOffset).toString()
  val rhs = injector.abbreviationGroup.resolveAbbreviation(lhs, mode, ctx.editor) ?: return null
  return AbbreviationExpansion(lhsRange, rhs)
}

/**
 * Find the lhs candidate immediately to the left of [caretOffset], per Vim's `:help abbreviations`:
 *
 *   * If the char before the cursor is a keyword char, the lhs covers full-id and end-id: the
 *     trailing keyword char plus every preceding non-whitespace char that matches the class of
 *     the char two-before-cursor.
 *   * If the char before the cursor is a non-keyword char, the lhs covers non-id: every
 *     contiguous non-whitespace char up to and including it.
 *
 * Returns null when there is no preceding char on the current line.
 */
internal fun findAbbreviationLhsRange(ctx: AbbreviationContext, caretOffset: Int): TextRange? {
  if (caretOffset <= ctx.lineStart) return null
  val start = if (isKeyword(ctx.editor, ctx.text[caretOffset - 1])) {
    walkBackKeywordLhs(ctx, caretOffset)
  } else {
    walkBackNonIdLhs(ctx.text, caretOffset, ctx.lineStart)
  }
  return TextRange(start, caretOffset)
}

/**
 * Walk back from a keyword-ending cursor. The lhs is the trailing keyword char plus every
 * preceding non-whitespace char of the same class as the char two-before-cursor. If there is
 * no char two-before-cursor, the class defaults to keyword (matching Vim's full-id behavior).
 */
private fun walkBackKeywordLhs(ctx: AbbreviationContext, caretOffset: Int): Int {
  val hasSecondChar = caretOffset - 1 > ctx.lineStart
  val expectedKeywordClass =
    if (hasSecondChar) isKeyword(ctx.editor, ctx.text[caretOffset - 2]) else true
  var start = caretOffset - 1
  while (start > ctx.lineStart) {
    val c = ctx.text[start - 1]
    if (c.isWhitespace() || isKeyword(ctx.editor, c) != expectedKeywordClass) break
    start--
  }
  return start
}

/** Walk back from a non-keyword-ending cursor through every contiguous non-whitespace char. */
private fun walkBackNonIdLhs(text: CharSequence, caretOffset: Int, lineStart: Int): Int {
  var start = caretOffset - 1
  while (start > lineStart && !text[start - 1].isWhitespace()) {
    start--
  }
  return start
}
