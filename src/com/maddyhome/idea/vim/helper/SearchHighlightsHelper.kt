/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:JvmName("SearchHighlightsHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.ColorUtil
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.option.OptionsManager.hlsearch
import com.maddyhome.idea.vim.option.OptionsManager.wrapscan
import org.jetbrains.annotations.Contract
import java.awt.Color
import java.awt.Font
import java.util.*

fun updateSearchHighlights(
  pattern: String?,
  shouldIgnoreSmartCase: Boolean,
  showHighlights: Boolean,
  forceUpdate: Boolean
) {
  updateSearchHighlights(pattern, shouldIgnoreSmartCase, showHighlights, -1, null, true, forceUpdate)
}

fun updateIncsearchHighlights(
  editor: Editor,
  pattern: String,
  forwards: Boolean,
  caretOffset: Int,
  searchRange: LineRange?
): Int {
  val searchStartOffset =
    if (searchRange != null) EditorHelper.getLineStartOffset(editor, searchRange.startLine) else caretOffset
  val showHighlights = hlsearch.isSet
  return updateSearchHighlights(pattern, false, showHighlights, searchStartOffset, searchRange, forwards, false)
}

fun addSubstitutionConfirmationHighlight(editor: Editor, start: Int, end: Int): RangeHighlighter {
  val color = TextAttributes(
    editor.colorsScheme.getColor(EditorColors.SELECTION_FOREGROUND_COLOR),
    editor.colorsScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR),
    editor.colorsScheme.getColor(EditorColors.CARET_COLOR),
    EffectType.ROUNDED_BOX, Font.PLAIN
  )
  return editor.markupModel.addRangeHighlighter(
    start, end, HighlighterLayer.SELECTION,
    color, HighlighterTargetArea.EXACT_RANGE
  )
}

/**
 * Refreshes current search highlights for all editors of currently active text editor/document
 */
private fun updateSearchHighlights(
  pattern: String?, shouldIgnoreSmartCase: Boolean, showHighlights: Boolean,
  initialOffset: Int, searchRange: LineRange?, forwards: Boolean, forceUpdate: Boolean
): Int {
  var currentMatchOffset = -1
  val projectManager = ProjectManager.getInstanceIfCreated() ?: return currentMatchOffset
  for (project in projectManager.openProjects) {
    val current = FileEditorManager.getInstance(project).selectedTextEditor ?: continue
    // [VERSION UPDATE] 202+ Use editors
    val editors = EditorFactory.getInstance().getEditors(current.document, project) ?: continue
    for (editor in editors) {
      // Try to keep existing highlights if possible. Update if hlsearch has changed or if the pattern has changed.
      // Force update for the situations where the text is the same, but the ignore case values have changed.
      // E.g. Use `*` to search for a word (which ignores smartcase), then use `/<Up>` to search for the same pattern,
      // which will match smartcase. Or changing the smartcase/ignorecase settings
      if (shouldRemoveSearchHighlights(editor, pattern, showHighlights) || forceUpdate) {
        removeSearchHighlights(editor)
      }

      if (pattern == null) continue

      if (shouldAddAllSearchHighlights(editor, pattern, showHighlights)) {
        // hlsearch (+ incsearch/noincsearch)
        val startLine = searchRange?.startLine ?: 0
        val endLine = searchRange?.endLine ?: -1
        val results =
          SearchHelper.findAll(editor, pattern, startLine, endLine, shouldIgnoreCase(pattern, shouldIgnoreSmartCase))
        if (results.isNotEmpty()) {
          currentMatchOffset = findClosestMatch(editor, results, initialOffset, forwards)
          highlightSearchResults(editor, pattern, results, currentMatchOffset)
        }
        editor.vimLastSearch = pattern
      } else if (shouldAddCurrentMatchSearchHighlight(pattern, showHighlights, initialOffset)) {
        // nohlsearch + incsearch
        val searchOptions = EnumSet.of(SearchOptions.WHOLE_FILE)
        if (wrapscan.isSet) searchOptions.add(SearchOptions.WRAP)
        if (shouldIgnoreSmartCase) searchOptions.add(SearchOptions.IGNORE_SMARTCASE)
        if (!forwards) searchOptions.add(SearchOptions.BACKWARDS)
        val result = SearchHelper.findPattern(editor, pattern, initialOffset, 1, searchOptions)
        if (result != null) {
          currentMatchOffset = result.startOffset
          val results = listOf(result)
          highlightSearchResults(editor, pattern, results, currentMatchOffset)
        }
      } else if (shouldMaintainCurrentMatchOffset(pattern, initialOffset)) {
        // incsearch. If nothing has changed (e.g. we've edited offset values in `/foo/e+2`) make sure we return the
        // current match offset so the caret remains at the current incsarch match
        val offset = editor.vimIncsearchCurrentMatchOffset
        if (offset != null) {
          currentMatchOffset = offset
        }
      }
    }
  }
  return currentMatchOffset
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

private fun findClosestMatch(editor: Editor, results: List<TextRange>, initialOffset: Int, forwards: Boolean): Int {
  if (results.isEmpty() || initialOffset == -1) {
    return -1
  }
  val size = editor.fileSize
  val max = Collections.max(results) { r1: TextRange, r2: TextRange ->
    val d1 = distance(r1, initialOffset, forwards, size)
    val d2 = distance(r2, initialOffset, forwards, size)
    if (d1 < 0 && d2 >= 0) {
      return@max Int.MAX_VALUE
    }
    d2 - d1
  }
  if (!wrapscan.isSet) {
    val start = max.startOffset
    if (forwards && start < initialOffset) {
      return -1
    } else if (start >= initialOffset) {
      return -1
    }
  }
  return max.startOffset
}

private fun distance(range: TextRange, pos: Int, forwards: Boolean, size: Int): Int {
  val start = range.startOffset
  return if (start <= pos) {
    if (forwards) size - pos + start else pos - start
  } else {
    if (forwards) start - pos else pos + size - start
  }
}

fun highlightSearchResults(editor: Editor, pattern: String, results: List<TextRange>, currentMatchOffset: Int) {
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
  var attributes = editor.colorsScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)
  if (current) {
    // This mimics what IntelliJ does with the Find live preview
    attributes = attributes.clone()
    attributes.effectType = EffectType.ROUNDED_BOX
    attributes.effectColor = editor.colorsScheme.getColor(EditorColors.CARET_COLOR)
  }
  if (attributes.errorStripeColor == null) {
    attributes.errorStripeColor = getFallbackErrorStripeColor(attributes, editor.colorsScheme)
  }
  val highlighter = editor.markupModel.addRangeHighlighter(
    start, end, HighlighterLayer.SELECTION - 1,
    attributes, HighlighterTargetArea.EXACT_RANGE
  )
  highlighter.errorStripeTooltip = tooltip
  return highlighter
}

/**
 * Return a valid error stripe colour based on editor background
 *
 *
 * Based on HighlightManager#addRangeHighlight behaviour, which we can't use because it will hide highlights
 * when hitting Escape.
 */
private fun getFallbackErrorStripeColor(attributes: TextAttributes, colorsScheme: EditorColorsScheme): Color? {
  if (attributes.backgroundColor != null) {
    val isDark = ColorUtil.isDark(colorsScheme.defaultBackground)
    return if (isDark) attributes.backgroundColor.brighter() else attributes.backgroundColor.darker()
  }
  return null
}

/**
 * Add search highlight for current match if hlsearch is false and we're performing incsearch highlights
 */
@Contract("_, true, _ -> false")
private fun shouldAddCurrentMatchSearchHighlight(pattern: String?, hlSearch: Boolean, initialOffset: Int): Boolean {
  return !hlSearch && isIncrementalSearchHighlights(initialOffset) && pattern != null && pattern.isNotEmpty()
}

/**
 * Keep the current match offset if the pattern is still valid and we're performing incremental search highlights
 * This will keep the caret position when editing the offset in e.g. `/foo/e+1`
 */
@Contract("null, _ -> false")
private fun shouldMaintainCurrentMatchOffset(pattern: String?, initialOffset: Int): Boolean {
  return pattern != null && pattern.isNotEmpty() && isIncrementalSearchHighlights(initialOffset)
}

/**
 * initialOffset is only valid if we're highlighting incsearch
 */
@Contract(pure = true)
private fun isIncrementalSearchHighlights(initialOffset: Int) = initialOffset != -1
