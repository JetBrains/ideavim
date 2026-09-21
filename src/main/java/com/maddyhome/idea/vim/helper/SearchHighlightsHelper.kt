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
 * Unlike the search highlights, this is not tracked by [SearchHighlights], so it is suitable for transient overlays -
 * such as the `inccommand` preview - that manage their own highlighter lifecycle.
 */
fun highlightPreviewMatch(editor: Editor, start: Int, end: Int, tooltip: String): RangeHighlighter {
  return createSearchMatchHighlighter(editor, start, end, tooltip)
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

    // The current match belongs to the 'incsearch' preview, and only to the editor being searched. Once the search has
    // been accepted or cancelled there is no current match any more, however the caret is then moved - the result is
    // simply where the caret is, and every match looks the same again (VIM-4308)
    val currentMatch = if (isIncsearch && isCurrentEditor) {
      findIncsearchMatch(vimEditor, pattern, initialOffset, count1, forwards, shouldIgnoreSmartCase)
    } else {
      null
    }
    val currentMatchOffset = currentMatch?.startOffset ?: -1

    if (shouldAddAllSearchHighlights(pattern, showHighlights)) {
      // hlsearch (+ incsearch/noincsearch)
      addAllSearchHighlights(vimEditor, pattern, searchStartLine, searchEndLine, shouldIgnoreSmartCase)
      editor.vimLastSearch = pattern
    } else if (currentMatch != null && shouldAddCurrentMatchSearchHighlight(pattern, showHighlights, initialOffset)) {
      // nohlsearch + incsearch. Even though search highlights are disabled, we still show a highlight (current editor
      // only), because 'incsearch' is active. We don't show one if Visual is active (behind Command-line, of course),
      // because the Visual selection is enough
      if (!vimEditor.inVisualMode && !vimEditor.inCommandLineModeWithVisual) {
        editor.vimSearchHighlights.showSingleMatch(pattern, currentMatch)
      }
    }

    editor.vimSearchHighlights.setCurrentMatch(currentMatchOffset)

    // Remember the incsearch match, so that `c_CTRL-R_CTRL-W` can insert the word after it
    editor.vimIncsearchCurrentMatch = currentMatch

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

  editor.ij.vimSearchHighlights.ensureUpToDate(
    pattern,
    shouldIgnoreCase(pattern, shouldIgnoreSmartCase),
    searchStartLine,
    searchEndLine,
  )
}

/**
 * Find the match that the in-progress search would move to - Vim's `hl-CurSearch`, and what `c_CTRL-G`/`c_CTRL-T`
 * step through. Returns null if the pattern doesn't match.
 *
 * This is a real search rather than a lookup among the highlighted matches. The highlights only cover part of the
 * document, and are only rebuilt when something about them changes, but the current match moves while the pattern
 * stays the same.
 */
private fun findIncsearchMatch(
  editor: VimEditor,
  pattern: String,
  initialOffset: Int,
  count1: Int,
  forwards: Boolean,
  shouldIgnoreSmartCase: Boolean,
): TextRange? {
  val searchOptions = EnumSet.of(SearchOptions.WHOLE_FILE)
  if (injector.globalOptions().wrapscan) searchOptions.add(SearchOptions.WRAP)
  if (shouldIgnoreSmartCase) searchOptions.add(SearchOptions.IGNORE_SMARTCASE)
  if (!forwards) searchOptions.add(SearchOptions.BACKWARDS)

  return injector.searchHelper.findPattern(editor, pattern, initialOffset, count1, searchOptions)
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
  editor.vimSearchHighlights.clear()
}

/**
 * Add search highlights if hlSearch is true and there is a pattern to highlight.
 *
 * There is deliberately no check for the pattern being unchanged - [SearchHighlights.ensureUpToDate] compares the
 * request against what the editor is already showing, so asking for highlights that are already there costs nothing.
 */
@Contract("_, false -> false; null, true -> false")
private fun shouldAddAllSearchHighlights(newPattern: String?, hlSearch: Boolean): Boolean {
  return hlSearch && !newPattern.isNullOrEmpty()
}

/**
 * Remove the current match highlight, e.g. after the caret has moved or the document has been edited. Does nothing if
 * there are no search highlights.
 *
 * The highlight belongs to the 'incsearch' preview and to nothing else, so there is never anything to work out from the
 * caret - moving it just takes the highlight away. While the command line is open, though, the preview owns the
 * highlight and follows the search and `c_CTRL-G`/`c_CTRL-T` rather than the caret, so reapply it instead.
 */
fun clearCurrentSearchMatchHighlight(editor: Editor) {
  if (editor.isDisposed) return
  editor.vimSearchHighlights.setCurrentMatch(editor.vimIncsearchCurrentMatch?.startOffset ?: -1)
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
