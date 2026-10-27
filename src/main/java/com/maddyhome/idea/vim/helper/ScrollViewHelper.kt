/*
 * This file is licensed under the Vim license
 *
 * Specifically, the functionality to scroll the caret into view is based
 * on Vim's update_topline, scroll_cursor_bot and scroll_cursor_top
 */
package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.textarea.TextComponentEditor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getVisualLineCount
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.helper.EditorHelper.getApproximateScreenHeight
import com.maddyhome.idea.vim.helper.EditorHelper.getApproximateScreenWidth
import com.maddyhome.idea.vim.helper.EditorHelper.getNonNormalizedVisualLineAtBottomOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.getVisibleArea
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualColumnAtLeftOfDisplay
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualColumnAtRightOfDisplay
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualLineAtBottomOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualLineAtTopOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollColumnToLeftOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollColumnToMiddleOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollColumnToRightOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollVisualLineToBottomOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollVisualLineToMiddleOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollVisualLineToTopOfScreen
import com.maddyhome.idea.vim.newapi.vim
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Scrolls the caret into view, the way Vim's move.c:update_topline does
 */
internal object ScrollViewHelper {
  /**
   * @param caretChangedLine True if the caret has just moved to a different visual line, see
   * [EditorHelper.scrollVisualLineToMiddleOfScreen]
   */
  @JvmStatic
  fun scrollCaretIntoView(editor: Editor, caretChangedLine: Boolean = false) {
    // TextComponentEditor doesn't support scrolling. We only support TextComponentEditor for the fallback window
    if (editor is TextComponentEditor) return

    val position = editor.caretModel.visualPosition
    scrollCaretIntoViewVertically(editor, position.line, caretChangedLine)
    scrollCaretIntoViewHorizontally(editor, position)
  }

  private fun scrollCaretIntoViewVertically(editor: Editor, caretLine: Int, caretChangedLine: Boolean) {
    val window = WindowLines(editor)
    val scrollOffset = injector.options(editor.vim).scrolloff

    // We need the raw scrolloff value here, so that so=999 keeps the caret centred
    when {
      scrollOffset > window.halfHeight -> centre(editor, caretLine, caretChangedLine)
      window.isAboveScrollOffset(caretLine, scrollOffset) ->
        scrollUpToCaret(editor, window, caretLine, scrollOffset, caretChangedLine)
      window.isBelowScrollOffset(caretLine, scrollOffset) ->
        scrollDownToCaret(editor, window, caretLine, scrollOffset, caretChangedLine)
    }
  }

  /**
   * Puts the caret at the top of the window, minus scrolloff. See move.c:scroll_cursor_top
   *
   * Vim starts at the new caret line and repeatedly advances lines above and below. The new top line must be at least
   * scrolloff above caretLine. If this takes the new top line above the current top line, we must scroll at least
   * scrolljump. If the new caret line was already above the current top line, this counts as one scroll, and we scroll
   * from the caret line. Otherwise, we scroll from the current top line.
   */
  private fun scrollUpToCaret(
    editor: Editor,
    window: WindowLines,
    caretLine: Int,
    scrollOffset: Int,
    caretChangedLine: Boolean,
  ) {
    // Initial approximation in move.c:update_topline (including same calculation for halfHeight)
    val linesToScroll = window.topLine + scrollOffset - caretLine
    if (window.countsLinesReliably && linesToScroll >= max(2, window.height / 2 - 1)) {
      centre(editor, caretLine, caretChangedLine)
      return
    }

    val scrollJump = getScrollJump(editor.vim, window.height)
    val scrollJumpTopLine =
      max(0, if (caretLine < window.topLine) caretLine - scrollJump + 1 else window.topLine - scrollJump)
    val scrollOffsetTopLine = max(0, caretLine - scrollOffset)
    val newTopLine = min(scrollOffsetTopLine, scrollJumpTopLine)

    // Used is set to the line height of caretLine (1 or how many lines soft wraps take up), and then incremented by
    // the line heights of the lines above and below caretLine (up to scrolloff or end of file).
    // Our implementation ignores soft wrap line heights. Folds already have a line height of 1.
    val usedAbove = caretLine - newTopLine
    val usedBelow = min(scrollOffset, editor.vim.getVisualLineCount() - caretLine)
    val used = 1 + usedAbove + usedBelow
    if (window.countsLinesReliably && used > window.height) {
      centre(editor, caretLine, caretChangedLine)
    } else {
      scrollVisualLineToTopOfScreen(editor, newTopLine)
    }
  }

  /**
   * Puts the caret at the bottom of the window, minus scrolloff. See move.c:scroll_cursor_bot
   *
   * Vim expands lines above and below the new bottom line. The total number of lines expanded is at least scrolljump
   * and there must be at least scrolloff lines below. Since the lines are advancing simultaneously, it is only possible
   * to get scrolljump/2 above the new caret line. If there are fewer than scrolljump/2 lines between the current bottom
   * line and the new caret line, the extra lines are pushed below the new caret line. Due to the algorithm advancing
   * the "above" line before the "below" line, we can end up with more than just scrolljump/2 lines on the top (hence
   * the sj+1). Therefore, the new bottom line is (cln + max(so, sj - min(cln-bl, ceiling((sj + 1)/2))))
   * (where cln is caretLine, bl is bottomLine, so is scrolloff and sj is scrolljump)
   */
  private fun scrollDownToCaret(
    editor: Editor,
    window: WindowLines,
    caretLine: Int,
    scrollOffset: Int,
    caretChangedLine: Boolean,
  ) {
    // Vim does a quick approximation before going through the full algorithm. It checks the line below the bottom
    // line in the window (bottomLine + 1). See move.c:update_topline
    val linesToScroll = caretLine - window.bottomLine + scrollOffset
    if (window.countsLinesReliably && linesToScroll > window.height) {
      centre(editor, caretLine, caretChangedLine)
      return
    }

    val scrollJump = getScrollJump(editor.vim, window.height)
    val scrolledAbove = caretLine - window.bottomLine
    val extra = max(scrollOffset, scrollJump - min(scrolledAbove, ((scrollJump + 1) / 2.0f).roundToInt()))
    val scrolled = scrolledAbove + extra

    // "used" is the count of lines expanded above and below. We expand below until we hit EOF (or when we've
    // expanded over a screen full) or until we've scrolled enough, and we've expanded at least linesAbove
    // We expand above until usedAbove + usedBelow >= height. Or until we've scrolled enough (scrolled > sj and
    // extra > so) and we've expanded at least linesAbove (and at most, linesAbove - scrolled - scrolledAbove - 1)
    // The minus one is for the current line
    val usedBelow = min(editor.vim.getVisualLineCount() - caretLine, scrolledAbove - 1)
    val used = min(window.height + 1, scrolledAbove + usedBelow)

    // If we've expanded more than a screen full, or we're going to scroll more than a screen full and more than
    // scrolloff, redraw with the caret in the middle of the screen
    val lineCount = if (used > window.height) used else scrolled
    if (window.countsLinesReliably && lineCount >= window.height && lineCount > scrollOffset) {
      centre(editor, caretLine, caretChangedLine)
    } else {
      scrollVisualLineToBottomOfScreen(editor, caretLine + extra)
    }
  }

  private fun centre(editor: Editor, caretLine: Int, caretChangedLine: Boolean) =
    scrollVisualLineToMiddleOfScreen(editor, caretLine, allowVirtualSpace = false, caretChangedLine)

  // Scrolljump is the minimal number of lines to scroll. It doesn't mean newLine = caretLine +/- MAX(sj, so), see
  // scrollUpToCaret and scrollDownToCaret
  private fun getScrollJump(editor: VimEditor, height: Int): Int {
    if (injector.vimState.executingCommandFlags.contains(CommandFlags.FLAG_IGNORE_SCROLL_JUMP)) return 1

    // Default value is 1. Zero is a valid value, but we normalise to 1 - we always want to scroll at least one line
    // If the value is negative, it's a percentage of the height.
    val scrollJump = injector.options(editor).scrolljump
    return if (scrollJump < 0) {
      (height * (min(100, -scrollJump) / 100.0)).toInt()
    } else {
      max(1, scrollJump)
    }
  }

  private fun scrollCaretIntoViewHorizontally(editor: Editor, position: VisualPosition) {
    val currentVisualLeftColumn = getVisualColumnAtLeftOfDisplay(editor, position.line)
    val currentVisualRightColumn = getVisualColumnAtRightOfDisplay(editor, position.line)
    val caretColumn = position.column
    val halfWidth = getApproximateScreenWidth(editor) / 2
    val scrollOffset = getNormalizedSideScrollOffset(editor)
    val allowSidescroll = !injector.vimState.executingCommandFlags.contains(CommandFlags.FLAG_IGNORE_SIDE_SCROLL_JUMP)
    val sidescroll = injector.options(editor.vim).sidescroll
    val offsetLeft = caretColumn - (currentVisualLeftColumn + scrollOffset)
    val offsetRight = caretColumn - (currentVisualRightColumn - scrollOffset)
    if (offsetLeft >= 0 && offsetRight <= 0) return

    var diff = if (offsetLeft < 0) -offsetLeft else offsetRight
    if (allowSidescroll && sidescroll == 0 || diff >= halfWidth || offsetRight >= offsetLeft) {
      scrollColumnToMiddleOfScreen(editor, position.line, caretColumn)
      return
    }

    if (allowSidescroll && diff < sidescroll) {
      diff = sidescroll
    }
    if (offsetLeft < 0) {
      scrollColumnToLeftOfScreen(editor, position.line, max(0, currentVisualLeftColumn - diff))
    } else {
      val rightColumn =
        EditorHelper.normalizeVisualColumn(editor, position.line, currentVisualRightColumn + diff, false)
      scrollColumnToRightOfScreen(editor, position.line, rightColumn)
    }
  }
}

/**
 * The lines in the window, as far as scrolling the caret into view is concerned.
 *
 * Vim counts lines. But block inlays, e.g. rendered doc comments, take up rows without being lines. When they take up
 * most of the window, counting lines says the window is tiny, and Vim's "if it's a long way, centre the caret" rules
 * keep firing, re-centring the caret instead of moving it. So then we measure in pixels, and skip those rules.
 *
 * TODO: Make this work with soft wraps
 * Vim's algorithm works by counting line heights for wrapped lines. We're using visual lines, which handles collapsed
 * folds, but treats soft wrapped lines as individual lines.
 */
private class WindowLines(private val editor: Editor) {
  val topLine = getVisualLineAtTopOfScreen(editor)
  val bottomLine = getVisualLineAtBottomOfScreen(editor)
  private val lastLine = editor.vim.getVisualLineCount() - 1

  /** The number of text lines in the window */
  val height = getNonNormalizedVisualLineAtBottomOfScreen(editor) - topLine + 1

  private val approximateHalfHeight = getApproximateScreenHeight(editor) / 2

  val countsLinesReliably = height > approximateHalfHeight

  val halfHeight = if (countsLinesReliably) height / 2 else approximateHalfHeight

  fun isAboveScrollOffset(line: Int, scrollOffset: Int): Boolean =
    if (countsLinesReliably) {
      line < topLine + scrollOffset
    } else {
      editor.visualLineToY(line) < getVisibleArea(editor).y + scrollOffset * editor.lineHeight
    }

  // Do nothing if the bottom of the file is already above the bottom of the window
  fun isBelowScrollOffset(line: Int, scrollOffset: Int): Boolean {
    if (bottomLine >= lastLine) return false
    return if (countsLinesReliably) {
      line > max(topLine + scrollOffset, bottomLine - scrollOffset)
    } else {
      val visibleArea = getVisibleArea(editor)
      editor.visualLineToY(line) > visibleArea.y + visibleArea.height - scrollOffset * editor.lineHeight
    }
  }
}
