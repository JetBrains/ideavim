/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.newapi.vim
import com.intellij.openapi.util.TextRange as IjTextRange

/**
 * Owns the `'hlsearch'`/`'incsearch'` highlighters of a single editor.
 *
 * This is the only thing allowed to add or remove a search highlighter. It keeps its own list and the editor's markup
 * model in step, and a second writer would leave one of the two stale - which is exactly the class of bug this type
 * exists to make impossible. Overlays that own their own lifecycle are not search highlights and stay outside: the
 * `inccommand` preview ([highlightPreviewMatch]) and the `:s///c` confirmation
 * ([addSubstitutionConfirmationHighlight]).
 *
 * Highlights are described rather than patched. [ensureUpToDate] says which pattern should be highlighted over which
 * part of the document, and does nothing at all when the editor is already in that state.
 */
internal class SearchHighlights(private val editor: Editor) {

  private enum class Mode {
    /** Nothing is highlighted and no pattern is being tracked. */
    NONE,

    /** Every match of the pattern inside the search range is highlighted - `'hlsearch'`. */
    ALL_MATCHES,

    /** A single match is highlighted - the `'incsearch'` preview while `'hlsearch'` is off. */
    SINGLE_MATCH,
  }

  private val highlighters = mutableListOf<RangeHighlighter>()

  private var mode = Mode.NONE
  private var pattern: String? = null
  private var ignoreCase = false

  /** The `:1,5s/foo` style line range the highlights are limited to. An [searchEndLine] of -1 means "to the end". */
  private var searchStartLine = 0
  private var searchEndLine = -1

  /** The part of the document [highlighters] actually covers. Null when nothing has been realized. */
  private var realized: IjTextRange? = null

  /** True if the editor is showing at least one search highlight, i.e. `v:hlsearch` should report 1. */
  val hasHighlights: Boolean
    get() = highlighters.isNotEmpty()

  /** True if a pattern is being tracked, even if it currently has no matches to show for it. */
  val isActive: Boolean
    get() = mode != Mode.NONE

  /**
   * The highlighters currently in the markup model.
   *
   * Exposed so that the current match can be restyled in place. Callers must not add to or remove from the markup
   * model themselves.
   */
  val activeHighlighters: List<RangeHighlighter>
    get() = highlighters

  /** The ranges currently highlighted, skipping any that the document has since invalidated. */
  fun matches(): List<TextRange> =
    highlighters.filter { it.isValid }.map { TextRange(it.startOffset, it.endOffset) }

  /**
   * Highlight every match of [pattern] between [searchStartLine] and [searchEndLine] (-1 for the end of the
   * document). Does nothing if that is already what the editor is showing.
   */
  fun ensureUpToDate(pattern: String, ignoreCase: Boolean, searchStartLine: Int = 0, searchEndLine: Int = -1) {
    val desired = desiredRange()
    if (mode == Mode.ALL_MATCHES &&
      pattern == this.pattern &&
      ignoreCase == this.ignoreCase &&
      searchStartLine == this.searchStartLine &&
      searchEndLine == this.searchEndLine &&
      realized?.contains(desired) == true
    ) {
      return
    }
    rebuild(pattern, ignoreCase, searchStartLine, searchEndLine, desired)
  }

  /**
   * Show [range] as the only highlight - the `'incsearch'` preview of the match the in-progress search would move to,
   * when `'hlsearch'` is off and there is nothing else to draw.
   */
  fun showSingleMatch(pattern: String, range: TextRange) {
    removeAll()
    mode = Mode.SINGLE_MATCH
    this.pattern = pattern
    realized = null
    highlighters.add(createSearchMatchHighlighter(editor, range.startOffset, range.endOffset, pattern))
  }

  /** The document changed underneath the highlights, so what was realized no longer describes the editor. */
  fun refreshAfterDocumentChange() {
    when (mode) {
      Mode.NONE -> return
      // The edit can create or destroy matches anywhere in the highlighted range, so work it out again
      Mode.ALL_MATCHES -> rebuild(pattern ?: return, ignoreCase, searchStartLine, searchEndLine, desiredRange())
      // The preview owns its single match. Keep it, unless the edit deleted the text it covers
      Mode.SINGLE_MATCH -> removeInvalid()
    }
  }

  /** Remove every highlight and stop tracking a pattern. */
  fun clear() {
    removeAll()
    mode = Mode.NONE
    pattern = null
    ignoreCase = false
    searchStartLine = 0
    searchEndLine = -1
    realized = null
  }

  private fun rebuild(
    pattern: String,
    ignoreCase: Boolean,
    searchStartLine: Int,
    searchEndLine: Int,
    desired: IjTextRange,
  ) {
    removeAll()

    mode = Mode.ALL_MATCHES
    this.pattern = pattern
    this.ignoreCase = ignoreCase
    this.searchStartLine = searchStartLine
    this.searchEndLine = searchEndLine
    this.realized = desired

    val lines = linesToSearch(desired) ?: return
    injector.searchHelper.findAll(editor.vim, pattern, lines.first, lines.last, ignoreCase).forEach {
      highlighters.add(createSearchMatchHighlighter(editor, it.startOffset, it.endOffset, pattern))
    }
  }

  /**
   * The lines to search: the part of the document we want realized, narrowed to the `:1,5s/foo` search range. Null if
   * the two don't overlap, so there is nothing to highlight.
   */
  private fun linesToSearch(desired: IjTextRange): IntRange? {
    val lastLine = editor.document.lineCount - 1
    val startLine = maxOf(searchStartLine, editor.offsetToLogicalPosition(desired.startOffset).line)
    val endLine = minOf(
      if (searchEndLine < 0) lastLine else searchEndLine,
      editor.offsetToLogicalPosition(desired.endOffset).line,
      lastLine,
    )
    return if (startLine > endLine) null else startLine..endLine
  }

  /** The part of the document the highlights should cover. */
  private fun desiredRange(): IjTextRange = IjTextRange(0, editor.document.textLength)

  private fun removeAll() {
    highlighters.forEach { editor.markupModel.removeHighlighter(it) }
    highlighters.clear()
  }

  private fun removeInvalid() {
    val iterator = highlighters.iterator()
    while (iterator.hasNext()) {
      val highlighter = iterator.next()
      if (!highlighter.isValid) {
        iterator.remove()
        editor.markupModel.removeHighlighter(highlighter)
      }
    }
  }
}

/**
 * Create a highlighter styled as a normal search match. Always uses a text attribute key, so the highlight updates
 * automatically when the colour scheme changes.
 *
 * The caller owns the returned highlighter and is responsible for removing it from the markup model.
 */
internal fun createSearchMatchHighlighter(editor: Editor, start: Int, end: Int, tooltip: String): RangeHighlighter {
  val highlighter = editor.markupModel.addRangeHighlighter(
    EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES,
    start,
    end,
    HighlighterLayer.SELECTION - 1,
    HighlighterTargetArea.EXACT_RANGE,
  )
  highlighter.errorStripeTooltip = tooltip
  return highlighter
}
