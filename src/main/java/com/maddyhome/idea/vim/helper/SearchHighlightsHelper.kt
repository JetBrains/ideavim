/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("SearchHighlightsHelper")

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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.inCommandLineModeWithVisual
import com.maddyhome.idea.vim.state.mode.inVisualMode
import org.jetbrains.annotations.Contract
import java.awt.Font
import java.util.*

fun updateSearchHighlights(
  pattern: String?,
  shouldIgnoreSmartCase: Boolean,
  showHighlights: Boolean,
  forceUpdate: Boolean,
) {
  updateSearchHighlights(null, pattern, 1, shouldIgnoreSmartCase, showHighlights, -1, null, true, forceUpdate)
}

fun updateSearchCount(
  pattern: String?,
  shouldIgnoreSmartCase: Boolean,
  currentMatchOffset: Int = -1,
) {
  if (pattern == null) return
  val selectedEditor = injector.editorGroup.getSelectedEditor() ?: return
  updateSearchCount(
    selectedEditor,
    pattern,
    0,
    selectedEditor.lineCount() - 1,
    shouldIgnoreSmartCase,
    currentMatchOffset,
    selectedEditor.ij,
  )
}

fun updateIncsearchHighlights(
  editor: Editor,
  pattern: String,
  count1: Int,
  forwards: Boolean,
  caretOffset: Int,
  searchRange: LineRange?,
  forceShowAllMatches: Boolean = false,
): Int {
  val searchStartOffset = if (searchRange != null && searchRange.startLine < editor.document.lineCount) {
    editor.vim.getLineStartOffset(searchRange.startLine)
  } else {
    caretOffset
  }
  val showHighlights = injector.options(editor.vim).hlsearch || forceShowAllMatches
  return updateSearchHighlights(
    editor.vim,
    pattern,
    count1,
    false,
    showHighlights,
    searchStartOffset,
    searchRange,
    forwards,
    false
  )
}

fun addSubstitutionConfirmationHighlight(editor: Editor, start: Int, end: Int): RangeHighlighter {
  val color = TextAttributes(
    editor.colorsScheme.getColor(EditorColors.SELECTION_FOREGROUND_COLOR),
    editor.colorsScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR),
    editor.colorsScheme.getColor(EditorColors.CARET_COLOR),
    EffectType.ROUNDED_BOX,
    Font.PLAIN,
  )
  return editor.markupModel.addRangeHighlighter(
    start,
    end,
    HighlighterLayer.SELECTION,
    color,
    HighlighterTargetArea.EXACT_RANGE,
  )
}

/**
 * Highlight a single range using the standard search-result attributes, returning the highlighter so the caller can
 * remove it later.
 *
 * Unlike [highlightSearchResults], this does not touch the editor's tracked incsearch highlighters, so it is suitable
 * for transient overlays - such as the `inccommand` preview - that manage their own highlighter lifecycle.
 */
fun highlightPreviewMatch(editor: Editor, start: Int, end: Int, tooltip: String): RangeHighlighter {
  return addSearchMatchHighlighter(editor, start, end, tooltip)
}

/**
 * Refreshes current search highlights for all visible editors
 */
private fun updateSearchHighlights(
  currentEditor: VimEditor?,
  pattern: String?,
  count1: Int,
  shouldIgnoreSmartCase: Boolean,
  showHighlights: Boolean,
  initialOffset: Int,
  searchRange: LineRange?,
  forwards: Boolean,
  forceUpdate: Boolean,
): Int {
  var currentEditorCurrentMatchOffset = -1

  // Update highlights in all visible editors. We update non-visible editors when they get focus.
  // Note that this now includes all editors - main, diff windows, even toolwindows like the Commit editor and consoles
  val editors = injector.editorGroup.getEditors().filter {
    !it.ij.isDisposed
      && (injector.application.isUnitTest() || it.ij.component.isShowing)
      && (currentEditor == null || it.projectId == currentEditor.projectId)
  }

  val isIncsearch = isIncrementalSearchHighlights(initialOffset)

  editors.forEach { vimEditor ->
    val editor = vimEditor.ij
    val isCurrentEditor = editor === currentEditor?.ij

    // Try to keep existing highlights if possible. Update if hlsearch has changed or if the pattern has changed.
    // Force update for the situations where the text is the same, but the ignore case values have changed.
    // E.g., Use `*` to search for a word (which ignores smartcase), then use `/<Up>` to search for the same pattern,
    // which will match smartcase. Or changing the smartcase/ignorecase settings
    if (shouldRemoveSearchHighlights(editor, pattern, showHighlights) || forceUpdate) {
      removeSearchHighlights(editor)
    }

    if (pattern == null) return@forEach

    val searchStartLine = searchRange?.startLine ?: 0
    val searchEndLine = (searchRange?.endLine ?: -1).coerceAtMost(vimEditor.lineCount() - 1)

    var incsearchMatchOffset = -1
    if (shouldAddAllSearchHighlights(editor, pattern, showHighlights)) {
      // hlsearch (+ incsearch/noincsearch)
      addAllSearchHighlights(vimEditor, pattern, searchStartLine, searchEndLine, shouldIgnoreSmartCase)
      editor.vimLastSearch = pattern
    } else if (isCurrentEditor && shouldAddCurrentMatchSearchHighlight(pattern, showHighlights, initialOffset)) {
      // nohlsearch + incsearch. Even though search highlights are disabled, we still show a highlight (current editor
      // only), because 'incsearch' is active
      incsearchMatchOffset =
        addIncsearchMatchHighlight(vimEditor, pattern, initialOffset, count1, forwards, shouldIgnoreSmartCase)
    }

    // The current match comes from the search while 'incsearch' is in progress - in the editor being searched only -
    // and from the caret otherwise
    val currentMatchOffset = when {
      !isIncsearch -> findMatchOffsetAtCaret(editor)
      !isCurrentEditor -> -1
      incsearchMatchOffset != -1 -> incsearchMatchOffset
      else -> findClosestMatch(highlightedMatches(editor), initialOffset, count1, forwards)
    }
    setCurrentSearchMatchHighlight(editor, currentMatchOffset)

    // Only 'incsearch' has an incsearch match. The current match of an accepted search is not one, even though both are
    // highlighted the same way - e.g. `c_CTRL-R_CTRL-W` inserts the word after the incsearch match, but the word under
    // the caret when there is no incsearch in progress
    editor.vimIncsearchCurrentMatchOffset = if (isIncsearch) currentMatchOffset else -1

    if (isCurrentEditor) {
      currentEditorCurrentMatchOffset = currentMatchOffset
    }

    updateSearchCount(
      vimEditor,
      pattern,
      searchStartLine,
      searchEndLine,
      shouldIgnoreSmartCase,
      currentMatchOffset,
      editor
    )
  }

  return currentEditorCurrentMatchOffset
}

private fun addAllSearchHighlights(
  editor: VimEditor,
  pattern: String,
  searchStartLine: Int,
  searchEndLine: Int,
  shouldIgnoreSmartCase: Boolean,
) {
  // Make sure the range fits this editor. Note that Vim will use the same range for all windows. E.g., given
  // `:1,5s/foo`, Vim will highlight all occurrences of `foo` in the first five lines of all visible windows
  if (searchStartLine > editor.lineCount() - 1) return

  val results = injector.searchHelper.findAll(
    editor,
    pattern,
    searchStartLine,
    searchEndLine,
    shouldIgnoreCase(pattern, shouldIgnoreSmartCase)
  )

  highlightSearchResults(editor.ij, pattern, results)
}

/**
 * Highlight the single match that the in-progress search would move to, returning its start offset, or -1 if the
 * pattern doesn't match.
 */
private fun addIncsearchMatchHighlight(
  editor: VimEditor,
  pattern: String,
  initialOffset: Int,
  count1: Int,
  forwards: Boolean,
  shouldIgnoreSmartCase: Boolean,
): Int {
  val searchOptions = EnumSet.of(SearchOptions.WHOLE_FILE)
  if (injector.globalOptions().wrapscan) searchOptions.add(SearchOptions.WRAP)
  if (shouldIgnoreSmartCase) searchOptions.add(SearchOptions.IGNORE_SMARTCASE)
  if (!forwards) searchOptions.add(SearchOptions.BACKWARDS)

  val result = injector.searchHelper.findPattern(editor, pattern, initialOffset, count1, searchOptions) ?: return -1

  // We don't show a highlight if Visual is active (behind Command-line, of course), because the Visual selection is
  // enough. We still return the offset, so the caller can update the selection
  if (!editor.inVisualMode && !editor.inCommandLineModeWithVisual) {
    highlightSearchResults(editor.ij, pattern, listOf(result))
  }
  return result.startOffset
}

fun updateSearchCount(
  vimEditor: VimEditor,
  pattern: String,
  searchStartLine: Int,
  searchEndLine: Int,
  shouldIgnoreSmartCase: Boolean,
  currentMatchOffset: Int,
  editor: Editor,
) {
  val matchOffset = if (currentMatchOffset != -1) currentMatchOffset else editor.caretModel.offset
  val maxSearchCount = injector.globalOptions().maxsearchcount
  val searchCount = computeSearchCount(
    vimEditor,
    pattern,
    searchStartLine,
    searchEndLine,
    shouldIgnoreCase(pattern, shouldIgnoreSmartCase),
    matchOffset,
    maxSearchCount,
  )
  if (searchCount.current <= 0) {
    return
  }
  injector.outputPanel.getOrCreate(
    IjVimEditor(editor),
    injector.executionContextManager.getEditorExecutionContext(IjVimEditor(editor))
  ).statusText = formatSearchCountText(searchCount, maxSearchCount)
}

private data class SearchCount(val current: Int, val total: Int, val exceededMaxCount: Boolean)

private fun computeSearchCount(
  vimEditor: VimEditor,
  pattern: String,
  searchStartLine: Int,
  searchEndLine: Int,
  ignoreCase: Boolean,
  matchOffset: Int,
  maxSearchCount: Int,
): SearchCount {
  val maxMatchesToFind = if (maxSearchCount > 0) maxSearchCount + 1 else Int.MAX_VALUE
  val results = injector.searchHelper.findAll(
    vimEditor,
    pattern,
    searchStartLine,
    searchEndLine,
    ignoreCase,
    maxMatchesToFind,
  )
  var current = 0
  var total = 0
  var exceededMaxCount = false
  for (range in results) {
    total++
    if (range.startOffset <= matchOffset) {
      current = total
    }
    if (maxSearchCount > 0 && total > maxSearchCount) {
      exceededMaxCount = true
      break
    }
  }
  if (current == 0 && total > 0) {
    current = total
  }
  return SearchCount(current, total, exceededMaxCount)
}

private fun formatSearchCountText(searchCount: SearchCount, maxSearchCount: Int): String {
  val current = searchCount.current
  val total = searchCount.total
  if (!searchCount.exceededMaxCount || total <= maxSearchCount) {
    return "[$current/$total]"
  }
  return if (current > maxSearchCount) {
    "[>$maxSearchCount/>$maxSearchCount]"
  } else {
    "[$current/>$maxSearchCount]"
  }
}

/**
 * Remove current search highlights if hlSearch is false, or if the pattern is changed
 */
@Contract("_, _, false -> true; _, null, true -> false")
private fun shouldRemoveSearchHighlights(editor: Editor, newPattern: String?, hlSearch: Boolean): Boolean {
  return !hlSearch || newPattern != null && newPattern != editor.vimLastSearch
}

private fun removeSearchHighlights(editor: Editor) {
  editor.vimLastSearch = null
  val ehl = editor.vimLastHighlighters ?: return
  for (rh in ehl) {
    editor.markupModel.removeHighlighter(rh)
  }
  editor.vimLastHighlighters = null
}

/**
 * Add search highlights if hlSearch is true and the pattern is changed
 */
@Contract("_, _, false -> false; _, null, true -> false")
private fun shouldAddAllSearchHighlights(editor: Editor, newPattern: String?, hlSearch: Boolean): Boolean {
  return hlSearch && newPattern != null && newPattern != editor.vimLastSearch && newPattern != ""
}

/**
 * The current match is deliberately derived from the existing highlights rather than from a fresh search. The highlights
 * are only recreated when the pattern changes, but the current match can move while the pattern stays the same -
 * `c_CTRL-G`/`c_CTRL-T` step the incsearch preview through the matches, and `hl-CurSearch` follows the caret
 */
private fun highlightedMatches(editor: Editor): List<TextRange> {
  val highlighters = editor.vimLastHighlighters ?: return emptyList()
  return highlighters.filter { it.isValid }.map { TextRange(it.startOffset, it.endOffset) }
}

/**
 * The start offset of the highlighted match the caret is inside, or -1 if the caret isn't inside a match. This is Vim's
 * `hl-CurSearch`, and each window highlights the match at its own caret.
 *
 * Note that the caret hasn't been moved to the match yet while highlighting an accepted search - the caret listener
 * calls [updateCurrentSearchMatchHighlight] when it does
 */
private fun findMatchOffsetAtCaret(editor: Editor): Int {
  val highlighters = editor.vimLastHighlighters ?: return -1
  val caretOffset = editor.caretModel.primaryCaret.offset
  // Scan the highlighters directly rather than going through [highlightedMatches] - this is called for every caret
  // movement, and there can be a lot of matches
  return highlighters.firstOrNull { it.isValid && it.containsOffset(caretOffset) }?.startOffset ?: -1
}

private fun RangeHighlighter.containsOffset(offset: Int) = offset >= startOffset && offset < endOffset

private fun findClosestMatch(
  results: List<TextRange>,
  initialOffset: Int,
  count: Int,
  forwards: Boolean,
): Int {
  if (results.isEmpty() || initialOffset == -1) {
    return -1
  }

  val sortedResults = if (forwards) {
    results.sortedBy { it.startOffset }
  } else {
    results.sortedByDescending { it.startOffset }
  }
  val closestIndex = if (forwards) {
    sortedResults.indexOfFirst { it.startOffset > initialOffset }
  } else {
    sortedResults.indexOfFirst { it.startOffset < initialOffset }
  }

  if (closestIndex == -1 && !injector.globalOptions().wrapscan) {
    return -1
  }

  val nextIndex = closestIndex.coerceAtLeast(0) + (count - 1)
  if (nextIndex >= sortedResults.size && !injector.globalOptions().wrapscan) {
    return -1
  }

  return sortedResults[nextIndex % results.size].startOffset
}

/**
 * Add and track a highlight for each of [results]. They are all added as normal matches - the current match is styled
 * separately, by [setCurrentSearchMatchHighlight].
 */
fun highlightSearchResults(
  editor: Editor,
  pattern: String,
  results: List<TextRange>,
) {
  // Don't start tracking highlighters for an empty list - that would make the editor look like it has search highlights
  if (results.isEmpty()) return

  val highlighters = editor.vimLastHighlighters
    ?: mutableListOf<RangeHighlighter>().also { editor.vimLastHighlighters = it }
  for (range in results) {
    highlighters.add(addSearchMatchHighlighter(editor, range.startOffset, range.endOffset, pattern))
  }
}

/** Always uses a text attribute key, so the highlight updates automatically when the colour scheme changes. */
private fun addSearchMatchHighlighter(editor: Editor, start: Int, end: Int, tooltip: String): RangeHighlighter {
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

/**
 * Refresh the current match highlight - e.g. after the caret has moved, or after the highlights have been recreated.
 * Vim redraws the current match as the cursor moves, so we do too. Does nothing if there are no search highlights.
 */
fun updateCurrentSearchMatchHighlight(editor: Editor) {
  if (editor.isDisposed) return
  val highlighters = editor.vimLastHighlighters ?: return

  // An 'incsearch' preview owns the current match highlight while the command line is open - it follows the search, and
  // `c_CTRL-G`/`c_CTRL-T`, rather than the caret - so reapply it rather than working it out from the caret
  val incsearchMatchOffset = editor.vimIncsearchCurrentMatchOffset?.takeIf { it != -1 }
  if (incsearchMatchOffset != null) {
    setCurrentSearchMatchHighlight(editor, incsearchMatchOffset)
    return
  }

  // This is called for every caret movement, so return without looking at the rest of the highlighters if the caret is
  // still inside the current match
  val caretOffset = editor.caretModel.primaryCaret.offset
  val previous = highlighters.firstOrNull { it.isVimCurrentSearchMatch }
  if (previous != null && previous.isValid && previous.containsOffset(caretOffset)) return

  setCurrentSearchMatchHighlight(editor, findMatchOffsetAtCaret(editor))
}

/**
 * Pass -1 to clear the current match highlight. Restyles the existing highlighters in place, so it does not repeat the
 * search, and does nothing if the current match hasn't moved.
 */
private fun setCurrentSearchMatchHighlight(editor: Editor, currentMatchOffset: Int) {
  val highlighters = editor.vimLastHighlighters ?: return
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

/**
 * Add search highlight for current match if hlsearch is false, and we're performing incsearch highlights
 */
@Contract("_, true, _ -> false")
private fun shouldAddCurrentMatchSearchHighlight(pattern: String?, hlSearch: Boolean, initialOffset: Int): Boolean {
  return !hlSearch && isIncrementalSearchHighlights(initialOffset) && !pattern.isNullOrEmpty()
}

/**
 * initialOffset is only valid if we're highlighting incsearch
 */
@Contract(pure = true)
private fun isIncrementalSearchHighlights(initialOffset: Int) = initialOffset != -1
