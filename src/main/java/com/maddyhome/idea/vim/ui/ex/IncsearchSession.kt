/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimSearchGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.shouldIgnoreCase
import com.maddyhome.idea.vim.helper.updateIncsearchHighlights
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import kotlin.math.max

/**
 * The 'incsearch' state of one command line: where the caret and the scroll were when it was opened, and how far
 * `c_CTRL-G`/`c_CTRL-T` moved the current match
 *
 * Used by [SearchPreview] for `/` and `?`, and by [ExCommandPreview] for `:s` and `:g`.
 */
internal class IncsearchSession(editor: Editor) {
  private val caretOffset = editor.caretModel.offset
  private val verticalOffset = editor.scrollingModel.verticalScrollOffset
  private val horizontalOffset = editor.scrollingModel.horizontalScrollOffset

  /**
   * How many matches past the first `c_CTRL-G` has advanced the incsearch preview. Added to the command count so the
   * "current match" moves forward as the user presses <C-G>
   */
  var matchOffset = 0
    private set

  /** Editing the pattern restarts incsearch from the first match, cancelling any `c_CTRL-G` advancement */
  fun restartFromFirstMatch() {
    matchOffset = 0
  }

  /** `c_CTRL-G` ([next] == true) / `c_CTRL-T` ([next] == false) */
  fun advanceMatch(next: Boolean) {
    matchOffset += if (next) 1 else -1
  }

  /**
   * Highlight the matches of the search pattern at the start of [searchText] and move the caret to the current match
   *
   * @param separator The character that ends the pattern, e.g. `/` in `foo/e`
   */
  fun showSearch(editor: Editor, searchText: String, separator: Char, forwards: Boolean) {
    show(editor, searchText, separator, forwards, searchRange = null, isExCommand = false)
  }

  /**
   * Highlight the matches of the pattern of an Ex command like `:s` in [searchRange] and move the caret to the current
   * match
   *
   * @param separator The character that ends the pattern, e.g. `/` in `foo/bar/g`
   */
  fun showExCommand(editor: Editor, searchText: String, separator: Char, searchRange: LineRange) {
    // :s, :g, :v are treated as forwards
    show(editor, searchText, separator, forwards = true, searchRange, isExCommand = true)
  }

  private fun show(
    editor: Editor,
    searchText: String,
    separator: Char,
    forwards: Boolean,
    searchRange: LineRange?,
    isExCommand: Boolean,
  ) {
    val patternEnd = injector.searchGroup.findEndOfPattern(searchText, separator, 0)
    val pattern = searchText.take(patternEnd)
    val count1 = currentMatchCount(editor, pattern, searchRange)
    if (count1 == null) {
      resetCaretAndScroll(editor)
      return
    }

    injector.editorGroup.closeEditorSearchSession(IjVimEditor(editor))
    // A substitute/global command highlights every match in its range, even without 'hlsearch'.
    val currentMatchStart = updateIncsearchHighlights(
      editor, pattern, count1, forwards, caretOffset, searchRange, forceShowAllMatches = isExCommand
    )
    if (currentMatchStart == -1) {
      resetCaretAndScroll(editor)
      return
    }

    // Moving the caret will update the Visual selection, which is only valid while performing a search. We want to
    // remove the Visual selection when the incsearch is for a command, as this is always unrelated to the current
    // selection. E.g. `V/foo` should update the selection to the search result, but `V` followed by `:<C-U>%s/foo`
    // should remove the selection first. We're in Command-line with Visual pending; exiting Visual leaves Command-line.
    if (isExCommand) IjVimEditor(editor).exitVisualMode()
    IjVimCaret(editor.caretModel.primaryCaret).moveToOffset(currentMatchStart)
  }

  /** Restore the highlights of the last accepted search, and the original caret and scroll */
  fun restoreLastSearch(editor: Editor) {
    resetHighlights()
    resetCaretAndScroll(editor)
  }

  /**
   * Called when the command line is closed
   *
   * @param editor      The editor of the command line, or null if it was disposed
   * @param resetCaret  Whether the caret should go back to where it was when the command line was opened
   */
  fun close(editor: Editor?, resetCaret: Boolean) {
    // TODO: Reduce the amount of unnecessary work here
    // If incsearch and hlsearch are enabled, and if this is a search panel, we'll have all of the results correctly
    // highlighted. But because we don't know why we're being closed, and what handler is being called next, we need
    // to reset state. This will remove all highlights and reset back to the last accepted search results. This is
    // fine for <Esc>. But if we hit <Enter>, the search handler will remove the highlights again, perform the same
    // search that we did for incsearch and add highlights back. The `:nohlsearch` command, even if bound to a
    // shortcut, is still processed by the ex entry panel, so deactivating will force update remove, search and add
    // of the current search results before the `NoHLSearchHandler` will remove all highlights again
    if (editor != null && resetCaret) {
      resetCaret(editor)
    }
    resetHighlights()
  }

  /**
   * The 1-based number of the current match, or null if the pattern has no matches
   *
   * Starts from a snapshot of the count for the in-progress command, coerced to 1. This value will include all count
   * components - selecting register(s), operator and motions. E.g. `2"a3"b4"c5d6/` will return 720. If we're showing
   * highlights for an Ex command like `:s`, the command builder will be empty, but we'll still get a valid value.
   * Includes any `c_CTRL-G`/`c_CTRL-T` advancement so the current match moves as the user presses <C-G>/<C-T>.
   */
  private fun currentMatchCount(editor: Editor, pattern: String, searchRange: LineRange?): Int? {
    val count0 = KeyHandler.getInstance().keyHandlerState.editorCommandBuilder.calculateCount0Snapshot()
    return normalizeCount(editor, pattern, max(1, count0) + matchOffset, searchRange)
  }

  /**
   * Coerce the incsearch match count into the valid `[1, matchCount]` range, wrapping around like 'wrapscan'.
   *
   * `c_CTRL-T` can step [count1] below 1 (before the first match); wrap it to the last match. Only recomputes the total
   * match count when needed (i.e. the fast path of typing a pattern, where the count is already >= 1, is untouched).
   * Returns null when the pattern has no matches, so the caller can reset the caret.
   */
  private fun normalizeCount(editor: Editor, pattern: String, count1: Int, searchRange: LineRange?): Int? {
    if (count1 >= 1) return count1
    val startLine = searchRange?.startLine ?: 0
    val endLine = searchRange?.endLine ?: -1
    val ignoreCase = shouldIgnoreCase(pattern, false)
    val total = injector.searchHelper.findAll(IjVimEditor(editor), pattern, startLine, endLine, ignoreCase).size
    if (total == 0) return null
    return Math.floorMod(count1 - 1, total) + 1
  }

  private fun resetHighlights() {
    (VimPlugin.getSearch() as VimSearchGroupBase).resetIncsearchHighlights()
  }

  private fun resetCaretAndScroll(editor: Editor) {
    // Reset the original caret, with original scroll offsets
    resetCaret(editor)
    resetScroll(editor)
  }

  private fun resetCaret(editor: Editor) {
    val primaryCaret = editor.caretModel.primaryCaret
    if (primaryCaret.offset != caretOffset) {
      primaryCaret.moveToOffset(caretOffset)
    }
  }

  private fun resetScroll(editor: Editor) {
    val scrollingModel = editor.scrollingModel
    if (scrollingModel.horizontalScrollOffset != horizontalOffset ||
      scrollingModel.verticalScrollOffset != verticalOffset
    ) {
      scrollingModel.scroll(horizontalOffset, verticalOffset)
    }
  }
}
