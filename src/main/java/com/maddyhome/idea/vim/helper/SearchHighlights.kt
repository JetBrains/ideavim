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
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
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

  /**
   * The start offset of the match styled as Vim's `hl-CurSearch`, or -1 for none.
   *
   * The layer renders the current match but does not decide which one it is - see [setCurrentMatch]. It has to be
   * remembered here rather than only applied to a highlighter, because a rebuild throws every highlighter away and
   * the style has to be put back on the new one.
   */
  private var currentMatchOffset = -1

  /** True if the editor is showing at least one search highlight, i.e. `v:hlsearch` should report 1. */
  val hasHighlights: Boolean
    get() = highlighters.isNotEmpty()

  /** True if a pattern is being tracked, even if it currently has no matches to show for it. */
  val isActive: Boolean
    get() = mode != Mode.NONE

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
    applyCurrentMatch()
  }

  /**
   * Style the match starting at [offset] as Vim's `hl-CurSearch`, or pass -1 to style every match the same.
   *
   * Restyles in place, so it does not repeat the search, and does nothing when the current match hasn't moved. If
   * the offset isn't one of the highlighted matches - the caller searched the whole file but the highlights are
   * limited to a `:1,5s/foo` range, say - then nothing is styled, which is what Vim shows too.
   */
  fun setCurrentMatch(offset: Int) {
    currentMatchOffset = offset
    applyCurrentMatch()
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
    currentMatchOffset = -1
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

    val lines = linesToSearch(desired)
    if (lines != null) {
      injector.searchHelper.findAll(editor.vim, pattern, lines.first, lines.last, ignoreCase).forEach {
        highlighters.add(createSearchMatchHighlighter(editor, it.startOffset, it.endOffset, pattern))
      }
    }

    // Every highlighter is new, so the current match has lost its style and has to be given it again
    applyCurrentMatch()
  }

  private fun applyCurrentMatch() {
    val previous = highlighters.firstOrNull { it.isVimCurrentSearchMatch }
    val current = if (currentMatchOffset == -1) {
      null
    } else {
      highlighters.firstOrNull { it.startOffset == currentMatchOffset }
    }
    if (previous === current) return

    previous?.clearVimCurrentSearchMatch()
    current?.setAsVimCurrentSearchMatch(editor)
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

/** True if this highlighter is for the current match, i.e. it is styled as Vim's `hl-CurSearch`. */
val RangeHighlighter.isVimCurrentSearchMatch: Boolean
  get() = getUserData(CURRENT_SEARCH_MATCH) == true

/** Marks the highlighter of the current match, so we can find and restyle it as the caret moves. */
private val CURRENT_SEARCH_MATCH = Key.create<Boolean>("ideavim.search.currentMatch")

/** Apply the current match style - Vim's `hl-CurSearch`, as opposed to `hl-Search` for the other matches. */
private fun RangeHighlighter.setAsVimCurrentSearchMatch(editor: Editor) {
  val highlighter = this as? RangeHighlighterEx ?: return
  highlighter.setTextAttributes(currentSearchMatchAttributes(editor))
  putUserData(CURRENT_SEARCH_MATCH, true)
}

/** Remove the current match style, falling back to the text attribute key the highlighter was created with. */
private fun RangeHighlighter.clearVimCurrentSearchMatch() {
  val highlighter = this as? RangeHighlighterEx ?: return
  highlighter.setTextAttributes(null)
  putUserData(CURRENT_SEARCH_MATCH, null)
}

/**
 * The attributes of the current match - Vim's `hl-CurSearch`. These are the same modifications that the Find live
 * preview makes to the search result attributes.
 *
 * There is no text attribute key for the current match, so unlike the other matches, it won't follow a change to the
 * editor's colour scheme until it's restyled by the next caret move or search.
 */
private fun currentSearchMatchAttributes(editor: Editor): TextAttributes {
  return editor.colorsScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES).clone().apply {
    effectType = EffectType.ROUNDED_BOX
    effectColor = editor.colorsScheme.getColor(EditorColors.CARET_COLOR)
  }
}
