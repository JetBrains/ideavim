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
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeVisualLine
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.newapi.vim

/**
 * Owns the `'hlsearch'`/`'incsearch'` highlighters of a single editor.
 *
 * This is the only thing allowed to add or remove a search highlighter. It keeps its own list and the editor's markup
 * model in step, and a second writer would leave one of the two stale - which is exactly the class of bug this type
 * exists to make impossible. Overlays that own their own lifecycle are not search highlights and stay outside: the
 * `inccommand` preview ([highlightPreviewMatch]) and the `:s///c` confirmation
 * ([addSubstitutionConfirmationHighlight]).
 *
 * Only the lines that could be on screen are highlighted. Vim computes `'hlsearch'` during redraw, per screen cell,
 * and keeps no highlight objects at all; IdeaVim has to materialise a [RangeHighlighter] per match, so it only
 * materialises the ones that can be seen.
 *
 * Highlights are described rather than patched by the caller. [ensureUpToDate] says which pattern should be
 * highlighted over which lines, and the class works out how little it can get away with doing.
 */
internal class SearchHighlights(private val editor: Editor) {

  private enum class Mode {
    /** Nothing is highlighted and no pattern is being tracked. */
    NONE,

    /** Every match of the pattern inside the realized lines is highlighted - `'hlsearch'`. */
    ALL_MATCHES,

    /** A single match is highlighted - the `'incsearch'` preview while `'hlsearch'` is off. */
    SINGLE_MATCH,
  }

  /** Kept sorted by start offset, so that trimming an end when the viewport moves is a prefix or a suffix. */
  private val highlighters = mutableListOf<RangeHighlighter>()

  private var mode = Mode.NONE
  private var pattern: String? = null
  private var ignoreCase = false

  /** The `:1,5s/foo` style line range the highlights are limited to. A [searchEndLine] of -1 means "to the end". */
  private var searchStartLine = 0
  private var searchEndLine = -1

  /** The lines [highlighters] actually covers. Null when nothing has been realized. */
  private var realized: IntRange? = null

  /**
   * The match styled as Vim's `hl-CurSearch`, or null for none.
   *
   * The class renders the current match but does not decide which one it is - see [setCurrentMatch]. It has to be
   * remembered here rather than only applied to a highlighter, because the highlighter it belongs to is thrown away
   * and recreated as the viewport moves.
   */
  private var currentMatch: TextRange? = null

  /** The highlighter added for a current match that fell outside the realized lines - see [adoptCurrentMatch]. */
  private var adopted: RangeHighlighter? = null

  /**
   * True if the pattern being highlighted matches anywhere in the document - what `v:hlsearch` reports.
   *
   * Deliberately not "is there a highlighter?". The highlights only cover the viewport, but `v:hlsearch` is about the
   * document, so a pattern that only matches off screen still counts. The search is the slow path, and only runs when
   * there is nothing on screen to answer the question with.
   */
  fun hasMatches(): Boolean {
    if (highlighters.isNotEmpty()) return true
    if (mode != Mode.ALL_MATCHES) return false
    val pattern = pattern ?: return false
    val lines = searchRangeLines() ?: return false
    return injector.searchHelper.findAll(editor.vim, pattern, lines.first, lines.last, ignoreCase, 1).isNotEmpty()
  }

  /** True if a pattern is being tracked, even if it currently has no matches to show for it. */
  val isActive: Boolean
    get() = mode != Mode.NONE

  /**
   * Highlight every match of [pattern] between [searchStartLine] and [searchEndLine] (-1 for the end of the
   * document), over the lines that could be on screen. Does nothing if that is already what the editor is showing.
   */
  fun ensureUpToDate(pattern: String, ignoreCase: Boolean, searchStartLine: Int = 0, searchEndLine: Int = -1) {
    val sameRequest = mode == Mode.ALL_MATCHES &&
      pattern == this.pattern &&
      ignoreCase == this.ignoreCase &&
      searchStartLine == this.searchStartLine &&
      searchEndLine == this.searchEndLine

    mode = Mode.ALL_MATCHES
    this.pattern = pattern
    this.ignoreCase = ignoreCase
    this.searchStartLine = searchStartLine
    this.searchEndLine = searchEndLine

    val desired = desiredLines()
    val realized = this.realized
    when {
      !sameRequest || realized == null || desired == null -> rebuild(desired)

      // Already covered - the common case for a small scroll, thanks to the margin
      realized.first <= desired.first && realized.last >= desired.last -> return

      // A jump rather than a scroll, so there is nothing worth reusing
      desired.last < realized.first || desired.first > realized.last -> rebuild(desired)

      else -> extend(desired, realized)
    }
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
   * Style [match] as Vim's `hl-CurSearch`, or pass null to style every match the same.
   *
   * Restyles in place, so it does not repeat the search, and does nothing when the current match hasn't moved.
   */
  fun setCurrentMatch(match: TextRange?) {
    currentMatch = match
    applyCurrentMatch()
  }

  /**
   * The viewport moved, so a different set of lines may need to be realized.
   *
   * Does nothing unless `'hlsearch'` highlights are showing, and nothing unless the new viewport has actually left
   * the lines that are already realized - which is the common case for a small scroll.
   */
  fun refreshForViewport() {
    if (mode != Mode.ALL_MATCHES) return
    ensureUpToDate(pattern ?: return, ignoreCase, searchStartLine, searchEndLine)
  }

  /** The document changed underneath the highlights, so what was realized no longer describes the editor. */
  fun refreshAfterDocumentChange() {
    when (mode) {
      Mode.NONE -> return
      // The edit can create or destroy matches anywhere in the realized lines, and it moves every offset after it,
      // so there is nothing to reuse
      Mode.ALL_MATCHES -> rebuild(desiredLines())
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
    currentMatch = null
  }

  private fun rebuild(desired: IntRange?) {
    removeAll()
    realized = desired
    if (desired != null) {
      addMatches(desired, mutableSetOf())
    }
    applyCurrentMatch()
  }

  /**
   * Move the realized lines to [desired], reusing the highlighters that both ranges have in common.
   *
   * Scrolling is what drives this, and scrolling is monotonic: [desired] and [realized] overlap in most of their
   * length, so throwing everything away and searching again would mean thousands of markup model operations to
   * recreate what is already there. Each end is trimmed or filled independently.
   */
  private fun extend(desired: IntRange, realized: IntRange) {
    trimTo(desired)

    // The boundary lines are searched again rather than skipped. A match can start on the last realized line and
    // reach into the gap, and a search starting on the next line would never report it. Anything already highlighted
    // is filtered out by start offset
    val known = highlighters.mapTo(mutableSetOf()) { it.startOffset }
    if (desired.first < realized.first) addMatches(desired.first..realized.first, known)
    if (desired.last > realized.last) addMatches(realized.last..desired.last, known)

    highlighters.sortBy { it.startOffset }
    this.realized = desired
    applyCurrentMatch()
  }

  /** Drop the highlighters that [desired] no longer reaches. */
  private fun trimTo(desired: IntRange) {
    val document = editor.document
    val startOffset = document.getLineStartOffset(desired.first)
    val endOffset = document.getLineEndOffset(desired.last)
    val iterator = highlighters.iterator()
    while (iterator.hasNext()) {
      val highlighter = iterator.next()
      // Keep anything that still overlaps - a multi-line match can start outside the range and reach into it
      if (highlighter.isValid && highlighter.endOffset >= startOffset && highlighter.startOffset <= endOffset) {
        continue
      }
      iterator.remove()
      if (highlighter === adopted) adopted = null
      editor.markupModel.removeHighlighter(highlighter)
    }
  }

  /** Highlight the matches in [lines], skipping any start offset already in [known]. */
  private fun addMatches(lines: IntRange, known: MutableSet<Int>) {
    val pattern = pattern ?: return
    val limit = highlighterLimit()
    for (match in injector.searchHelper.findAll(editor.vim, pattern, lines.first, lines.last, ignoreCase)) {
      if (highlighters.size >= limit) break
      if (!known.add(match.startOffset)) continue
      highlighters.add(createSearchMatchHighlighter(editor, match.startOffset, match.endOffset, pattern))
    }
  }

  private fun applyCurrentMatch() {
    // Work out the new current match first - it can drop a previously adopted highlighter, which must not then be
    // restyled
    val current = currentMatch?.let { match ->
      highlighters.firstOrNull { it.startOffset == match.startOffset } ?: adoptCurrentMatch(match)
    }
    highlighters.filter { it !== current && it.isVimCurrentSearchMatch }.forEach { it.clearVimCurrentSearchMatch() }
    if (current != null && !current.isVimCurrentSearchMatch) {
      current.setAsVimCurrentSearchMatch(editor)
    }
  }

  /**
   * Highlight the current match even though it is outside the realized lines, returning null if it shouldn't be.
   *
   * `'incsearch'` previews the match before scrolling to it, so the preview is regularly off screen when the
   * highlights are built. Without this the user would type a pattern and see nothing until the caret caught up.
   *
   * A match inside the realized lines that has no highlighter is simply not a match, and a `:1,5s/foo` range still
   * has to be respected. With `'nohlsearch'` the preview is the only highlight and [showSingleMatch] has drawn it
   * already - or deliberately hasn't, because Visual is active and the selection says the same thing.
   */
  private fun adoptCurrentMatch(match: TextRange): RangeHighlighter? {
    if (mode != Mode.ALL_MATCHES) return null
    val pattern = pattern ?: return null
    val line = editor.offsetToLogicalPosition(match.startOffset).line
    if (realized?.contains(line) == true) return null
    if (!isInSearchRange(line)) return null

    // At most one match is ever adopted. `c_CTRL-G` can step the preview through several off-screen matches without
    // the viewport moving, and each one would otherwise leave a highlighter behind
    dropAdopted()
    return createSearchMatchHighlighter(editor, match.startOffset, match.endOffset, pattern).also {
      highlighters.add(it)
      adopted = it
    }
  }

  private fun dropAdopted() {
    val highlighter = adopted ?: return
    adopted = null
    if (highlighters.remove(highlighter)) {
      editor.markupModel.removeHighlighter(highlighter)
    }
  }

  private fun isInSearchRange(line: Int) =
    line >= searchStartLine && (searchEndLine < 0 || line <= searchEndLine)

  /** The `:1,5s/foo` search range, clamped to the document. Null if it doesn't overlap the document at all. */
  private fun searchRangeLines(): IntRange? {
    val lastLine = editor.document.lineCount - 1
    if (lastLine < 0) return null
    val endLine = minOf(if (searchEndLine < 0) lastLine else searchEndLine, lastLine)
    return if (searchStartLine > endLine) null else searchStartLine..endLine
  }

  /**
   * The lines the highlights should cover: what is on screen, plus a screen's worth of margin above and below,
   * narrowed to the search range.
   *
   * The margin is measured in *visual* lines, because that is what a screen is made of, and only then converted to
   * buffer lines. Measuring it in buffer lines gets this wrong in both directions, and badly: one collapsed fold
   * makes a 35 line screen span thousands of buffer lines, so the "one screen" margin becomes thousands of lines and
   * every rebuild walks all of them - and collapsed folds are the normal state of an IntelliJ editor. Soft wrap does
   * the opposite, shrinking the margin to a handful of lines so that a couple of lines of scrolling rebuilds.
   *
   * The margin is what makes scrolling cheap: it has to be left behind entirely before anything is rebuilt, and it
   * is why a match starting above the top edge is still found.
   */
  private fun desiredLines(): IntRange? {
    val searchRange = searchRangeLines() ?: return null
    val vimEditor = editor.vim

    val topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor)
    val bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor)
    val screenHeight = bottomVisualLine - topVisualLine + 1
    val firstVisualLine = vimEditor.normalizeVisualLine(topVisualLine - screenHeight)
    val lastVisualLine = vimEditor.normalizeVisualLine(bottomVisualLine + screenHeight)

    val first = maxOf(searchRange.first, vimEditor.visualLineToBufferLine(firstVisualLine))
    val last = minOf(searchRange.last, vimEditor.visualLineToBufferLine(lastVisualLine))
    return if (first > last) null else first..last
  }

  private fun removeAll() {
    highlighters.forEach { editor.markupModel.removeHighlighter(it) }
    highlighters.clear()
    adopted = null
  }

  private fun removeInvalid() {
    val iterator = highlighters.iterator()
    while (iterator.hasNext()) {
      val highlighter = iterator.next()
      if (!highlighter.isValid) {
        iterator.remove()
        if (highlighter === adopted) adopted = null
        editor.markupModel.removeHighlighter(highlighter)
      }
    }
  }
}

/**
 * Never hold more than this many search highlighters in one editor.
 *
 * A backstop for patterns that match everything: `/.` puts a highlighter on every single character. Scoping the
 * highlights to the viewport bounds the ordinary case; this bounds the pathological one, where even a single screen
 * is thousands of highlighters.
 *
 * The limit is 'maxsearchcount', which already bounds the `[1/999]` search count, so the default is 1000. That is
 * far more than any realistic pattern matches on one screen, but less than `/.` does - which is the point, though it
 * does mean such a search is highlighted only as far as the limit.
 */
private fun highlighterLimit(): Int {
  val maxSearchCount = injector.globalOptions().maxsearchcount
  return if (maxSearchCount > 0) maxSearchCount + 1 else Int.MAX_VALUE
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
