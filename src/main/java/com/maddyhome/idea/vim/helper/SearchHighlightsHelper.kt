/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("SearchHighlightsHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.inCommandLineModeWithVisual
import com.maddyhome.idea.vim.state.mode.inVisualMode
import org.jetbrains.annotations.Contract
import java.awt.Font
import java.util.*

internal fun updateSearchHighlights(
  pattern: String?,
  shouldIgnoreSmartCase: Boolean,
  showHighlights: Boolean,
  forceUpdate: Boolean,
) {
  updateSearchHighlights(null, pattern, 1, shouldIgnoreSmartCase, showHighlights, -1, null, true, forceUpdate)
}

internal fun updateIncsearchHighlights(
  editor: Editor,
  pattern: String,
  count1: Int,
  forwards: Boolean,
  caretOffset: Int,
  searchRange: LineRange?,
): Int {
  val searchStartOffset = if (searchRange != null && searchRange.startLine < editor.document.lineCount) {
    editor.vim.getLineStartOffset(searchRange.startLine)
  } else {
    caretOffset
  }
  val showHighlights = injector.options(editor.vim).hlsearch
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

internal fun addSubstitutionConfirmationHighlight(editor: Editor, start: Int, end: Int): RangeHighlighter {
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
    (injector.application.isUnitTest() || it.ij.component.isShowing)
      && (currentEditor == null || it.projectId == currentEditor.projectId)
  }

  editors.forEach {
    val editor = it.ij
    var currentMatchOffset = -1

    // Try to keep existing highlights if possible. Update if hlsearch has changed or if the pattern has changed.
    // Force update for the situations where the text is the same, but the ignore case values have changed.
    // E.g., Use `*` to search for a word (which ignores smartcase), then use `/<Up>` to search for the same pattern,
    // which will match smartcase. Or changing the smartcase/ignorecase settings
    if (shouldRemoveSearchHighlights(editor, pattern, showHighlights) || forceUpdate) {
      removeSearchHighlights(editor)
    }

    if (pattern == null) return@forEach

    if (shouldAddAllSearchHighlights(editor, pattern, showHighlights)) {
      // hlsearch (+ incsearch/noincsearch)
      // Make sure the range fits this editor. Note that Vim will use the same range for all windows. E.g., given
      // `:1,5s/foo`, Vim will highlight all occurrences of `foo` in the first five lines of all visible windows
      val vimEditor = editor.vim
      val editorLastLine = vimEditor.lineCount() - 1
      val searchStartLine = searchRange?.startLine ?: 0
      val searchEndLine = (searchRange?.endLine ?: -1).coerceAtMost(editorLastLine)
      if (searchStartLine <= editorLastLine) {
        val results =
          injector.searchHelper.findAll(
            vimEditor,
            pattern,
            searchStartLine,
            searchEndLine,
            shouldIgnoreCase(pattern, shouldIgnoreSmartCase)
          )
        if (results.isNotEmpty()) {
          if (editor === currentEditor?.ij) {
            currentMatchOffset = findClosestMatch(results, initialOffset, count1, forwards)
          }
          highlightSearchResults(editor, pattern, results, currentMatchOffset)
        }
      }
      editor.vimLastSearch = pattern
    } else if (shouldAddCurrentMatchSearchHighlight(pattern, showHighlights, initialOffset)) {
      // nohlsearch + incsearch. Even though search highlights are disabled, we still show a highlight (current editor
      // only), because 'incsearch' is active. But we don't show a search if Visual is active (behind Command-line of
      // course), because the Visual selection is enough. We still need to find the current offset to update the
      // selection
      if (editor === currentEditor?.ij) {
        val searchOptions = EnumSet.of(SearchOptions.WHOLE_FILE)
        if (injector.globalOptions().wrapscan) searchOptions.add(SearchOptions.WRAP)
        if (shouldIgnoreSmartCase) searchOptions.add(SearchOptions.IGNORE_SMARTCASE)
        if (!forwards) searchOptions.add(SearchOptions.BACKWARDS)
        val result = injector.searchHelper.findPattern(it, pattern, initialOffset, count1, searchOptions)
        if (result != null) {
          if (!it.inVisualMode && !it.inCommandLineModeWithVisual) {
            val results = listOf(result)
            highlightSearchResults(editor, pattern, results, result.startOffset)
          }
          currentMatchOffset = result.startOffset
        }
      }
    } else if (shouldMaintainCurrentMatchOffset(pattern, initialOffset)) {
      // incsearch. If nothing has changed (e.g., we've edited offset values in `/foo/e+2`) make sure we return the
      // current match offset so the caret remains at the current incsarch match
      val offset = editor.vimIncsearchCurrentMatchOffset
      if (offset != null && editor === currentEditor?.ij) {
        currentMatchOffset = offset
      }
    }

    if (editor === currentEditor?.ij) {
      currentEditorCurrentMatchOffset = currentMatchOffset
    }
  }

  return currentEditorCurrentMatchOffset
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

internal fun highlightSearchResults(
  editor: Editor,
  pattern: String,
  results: List<TextRange>,
  currentMatchOffset: Int,
) {
  var highlighters = editor.vimLastHighlighters
  if (highlighters == null) {
    highlighters = mutableListOf()
    editor.vimLastHighlighters = highlighters
  }
  for (range in results) {
    val current = range.startOffset == currentMatchOffset
    val highlighter = highlightMatch(editor, range.startOffset, range.endOffset, current, pattern)
    highlighters.add(highlighter)
  }
  editor.vimIncsearchCurrentMatchOffset = currentMatchOffset
}

private fun highlightMatch(editor: Editor, start: Int, end: Int, current: Boolean, tooltip: String): RangeHighlighter {
  val layer = HighlighterLayer.SELECTION - 1
  val targetArea = HighlighterTargetArea.EXACT_RANGE
  if (!current) {
    // If we use a text attribute key, it will update automatically when the editor's colour scheme changes
    val highlighter =
      editor.markupModel.addRangeHighlighter(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES, start, end, layer, targetArea)
    highlighter.errorStripeTooltip = tooltip
    return highlighter
  }

  // There isn't a text attribute key for current selection. This means we won't update automatically when the editor's
  // colour scheme changes. However, this is only used during incsearch, so it should be replaced pretty quickly. It's a
  // small visual glitch that will fix itself quickly. Let's not bother implementing an editor colour scheme listener
  // just for this.
  // These are the same modifications that the Find live preview does. We could look at using LivePreviewPresentation,
  // which might also be useful for text attributes in selection (if we supported that)
  val attributes = editor.colorsScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES).clone().apply {
    effectType = EffectType.ROUNDED_BOX
    effectColor = editor.colorsScheme.getColor(EditorColors.CARET_COLOR)
  }
  return editor.markupModel.addRangeHighlighter(start, end, layer, attributes, targetArea).apply {
    errorStripeTooltip = tooltip
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
 * Keep the current match offset if the pattern is still valid, and we're performing incremental search highlights
 * This will keep the caret position when editing the offset in e.g. `/foo/e+1`
 */
@Contract("null, _ -> false")
private fun shouldMaintainCurrentMatchOffset(pattern: String?, initialOffset: Int): Boolean {
  return !pattern.isNullOrEmpty() && isIncrementalSearchHighlights(initialOffset)
}

/**
 * initialOffset is only valid if we're highlighting incsearch
 */
@Contract(pure = true)
private fun isIncrementalSearchHighlights(initialOffset: Int) = initialOffset != -1
