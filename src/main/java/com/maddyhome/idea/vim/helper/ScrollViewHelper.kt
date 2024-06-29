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
import com.maddyhome.idea.vim.api.normalizeVisualColumn
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.helper.EditorHelper.getApproximateScreenHeight
import com.maddyhome.idea.vim.helper.EditorHelper.getApproximateScreenWidth
import com.maddyhome.idea.vim.helper.EditorHelper.getNonNormalizedVisualLineAtBottomOfScreen
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
import com.maddyhome.idea.vim.state.VimStateMachine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Handles scrolling the caret into view
 */
internal object ScrollViewHelper {
  @JvmStatic
  fun scrollCaretIntoView(editor: Editor) {
    // TextComponentEditor doesn't support scrolling. We only support TextComponentEditor for the fallback window
    if (editor is TextComponentEditor) return

    val position = editor.caretModel.visualPosition
    scrollCaretIntoViewVertically(editor, position.line)
    scrollCaretIntoViewHorizontally(editor, position)
  }

  // Vim's version of this method is move.c:update_topline, which will first scroll to fit the current line number at
  // the top of the window and then ensure that the current line fits at the bottom of the window
  private fun scrollCaretIntoViewVertically(editor: Editor, caretLine: Int) {
    val vimEditor: VimEditor = editor.vim

    // TODO: Make this work with soft wraps
    // Vim's algorithm works by counting line heights for wrapped lines. We're using visual lines, which handles
    // collapsed folds, but treats soft wrapped lines as individual lines.
    // Ironically, after figuring out how Vim's algorithm works (although not *why*) and reimplementing, it looks likely
    // that this needs to be replaced as a more or less dumb line for line rewrite.
    val topLine = getVisualLineAtTopOfScreen(editor)
    val bottomLine = getVisualLineAtBottomOfScreen(editor)
    val lastLine = vimEditor.getVisualLineCount() - 1

    // We need the non-normalised value here, so we can handle cases such as so=999 to keep the current line centred
    val scrollOffset = injector.options(vimEditor).scrolloff
    val topBound = topLine + scrollOffset
    val bottomBound = max(topBound, bottomLine - scrollOffset)

    // If we need to scroll the current line more than half a screen worth of lines then we just centre the new
    // current line. This mimics vim behavior of e.g. 100G in a 300 line file with a screen size of 25 centering line
    // 100. It also handles so=999 keeping the current line centred.
    // Note that block inlays means that the pixel height we are scrolling can be larger than half the screen, even if
    // the number of lines is less. I'm not sure what impact this has.
    val height = getNonNormalizedVisualLineAtBottomOfScreen(editor) - topLine + 1

    // Scrolljump isn't handled as you might expect. It is the minimal number of lines to scroll, but that doesn't mean
    // newLine = caretLine +/- MAX(sj, so)
    // <editor-fold desc="// Details">
    // When scrolling up (`k` - scrolling window up in the buffer; more lines are visible at the top of the window), Vim
    // will start at the new cursor line and repeatedly advance lines above and below. The new top line must be at least
    // scrolloff above caretLine. If this takes the new top line above the current top line, we must scroll at least
    // scrolljump. If the new caret line was already above the current top line, this counts as one scroll, and we
    // scroll from the caret line. Otherwise, we scroll from the current top line.
    // (See move.c:scroll_cursor_top)
    //
    // When scrolling down (`j` - scrolling window down in the buffer; more lines are visible at the bottom), Vim again
    // expands lines above and below the new bottom line, but calculates things a little differently. The total number
    // of lines expanded is at least scrolljump and there must be at least scrolloff lines below.
    // Since the lines are advancing simultaneously, it is only possible to get scrolljump/2 above the new cursor line.
    // If there are fewer than scrolljump/2 lines between the current bottom line and the new cursor line, the extra
    // lines are pushed below the new cursor line. Due to the algorithm advancing the "above" line before the "below"
    // line, we can end up with more than just scrolljump/2 lines on the top (hence the sj+1).
    // Therefore, the new top line is (cln + max(so, sj - min(cln-bl, ceiling((sj + 1)/2))))
    // (where cln is caretLine, bl is bottomLine, so is scrolloff and sj is scrolljump)
    // (See move.c:scroll_cursor_bot)
    //
    // On top of that, if the scroll distance is "too large", the new cursor line is positioned in the centre of the
    // screen. What "too large" means depends on scroll direction. There is an initial approximate check before working
    // out correct scroll locations
    // </editor-fold>
    val scrollJump = getScrollJump(vimEditor, height)

    // Unavoidable fudge value. Multiline rendered doc comments can mean we have very few actual lines, and scrolling
    // can get stuck in a loop as we re-centre the cursor instead of actually moving it. But if we ignore all inlays
    // and use the approximate screen height instead of the actual screen height (in lines), we make incorrect
    // assumptions about the top/bottom line numbers and can scroll to the wrong location. E.g. if there are enough doc
    // comments (String.java) it's possible to get 12 lines of actual code on screen. Given scrolloff=5, it's very easy
    // to hit problems, and have (scrolloffset > height / 2) and scroll to the middle of the screen. We'll use this
    // fudge value to make sure we're working with sensible values. Note that this problem doesn't affect code without
    // block inlays as positioning the cursor in the middle of the screen always positions it in a deterministic manner,
    // relative to other text in the file.
    val inlayAwareMinHeightFudge = getApproximateScreenHeight(editor) / 2

    // Note that while these calculations do the same thing that Vim does, it processes them differently. E.g. it
    // optionally checks and moves the top line, then optionally checks the bottom line. This gives us the same results
    // via the tests.
    if (height > inlayAwareMinHeightFudge && scrollOffset > height / 2) {
      scrollVisualLineToMiddleOfScreen(editor, caretLine, false)
    } else if (caretLine < topBound) {
      // Scrolling up, put the cursor at the top of the window (minus scrolloff)
      // Initial approximation in move.c:update_topline (including same calculation for halfHeight)
      if (topLine + scrollOffset - caretLine >= max(2, height / 2 - 1)) {
        scrollVisualLineToMiddleOfScreen(editor, caretLine, false)
      } else {
        // New top line must be at least scrolloff above caretLine. If this is above current top line, we must scroll
        // at least scrolljump. If caretLine was already above topLine, this counts as one scroll, and we scroll from
        // here. Otherwise, we scroll from topLine
        val scrollJumpTopLine =
          max(0, if (caretLine < topLine) caretLine - scrollJump + 1 else topLine - scrollJump)
        val scrollOffsetTopLine = max(0, caretLine - scrollOffset)
        val newTopLine = min(scrollOffsetTopLine, scrollJumpTopLine)

        // Used is set to the line height of caretLine (1 or how many lines soft wraps take up), and then incremented by
        // the line heights of the lines above and below caretLine (up to scrolloff or end of file).
        // Our implementation ignores soft wrap line heights. Folds already have a line height of 1.
        val usedAbove = caretLine - newTopLine
        val usedBelow = min(scrollOffset, vimEditor.getVisualLineCount() - caretLine)
        val used = 1 + usedAbove + usedBelow
        if (used > height) {
          scrollVisualLineToMiddleOfScreen(editor, caretLine, false)
        } else {
          scrollVisualLineToTopOfScreen(editor, newTopLine)
        }
      }
    } else if (caretLine > bottomBound && bottomLine < lastLine) {
      // Scrolling down, put the cursor at the bottom of the window (minus scrolloff)
      // Do nothing if the bottom of the file is already above the bottom of the screen
      // Vim does a quick approximation before going through the full algorithm. It checks the line below the bottom
      // line in the window (bottomLine + 1). See move.c:update_topline
      var lineCount = caretLine - (bottomLine + 1) + 1 + scrollOffset
      if (lineCount > height) {
        scrollVisualLineToMiddleOfScreen(editor, caretLine, false)
      } else {
        // Vim expands out from caretLine at least scrolljump lines. It stops expanding above when it hits the
        // current bottom line, or (because it's expanding above and below) when it's scrolled scrolljump/2. It expands
        // above first, and the initial scroll count is 1, so we used (scrolljump+1)/2
        val scrolledAbove = caretLine - bottomLine
        val extra = max(scrollOffset, scrollJump - min(scrolledAbove, ((scrollJump + 1) / 2.0f).roundToInt()))
        val scrolled = scrolledAbove + extra

        // "used" is the count of lines expanded above and below. We expand below until we hit EOF (or when we've
        // expanded over a screen full) or until we've scrolled enough, and we've expanded at least linesAbove
        // We expand above until usedAbove + usedBelow >= height. Or until we've scrolled enough (scrolled > sj and extra > so)
        // and we've expanded at least linesAbove (and at most, linesAbove - scrolled - scrolledAbove - 1)
        // The minus one is for the current line
        val usedBelow = min(vimEditor.getVisualLineCount() - caretLine, scrolledAbove - 1)
        val used = min(height + 1, scrolledAbove + usedBelow)

        // If we've expanded more than a screen full, redraw with the cursor in the middle of the screen. If we're going
        // scroll more than a screen full or more than scrolloff, redraw with the cursor in the middle of the screen.
        lineCount = if (used > height) used else scrolled
        if (lineCount >= height && lineCount > scrollOffset) {
          scrollVisualLineToMiddleOfScreen(editor, caretLine, false)
        } else {
          scrollVisualLineToBottomOfScreen(editor, caretLine + extra)
        }
      }
    }
  }

  private fun getScrollJump(editor: VimEditor, height: Int): Int {
    val flags = injector.vimState.executingCommandFlags
    val scrollJump = !flags.contains(CommandFlags.FLAG_IGNORE_SCROLL_JUMP)

    // Default value is 1. Zero is a valid value, but we normalise to 1 - we always want to scroll at least one line
    // If the value is negative, it's a percentage of the height.
    if (scrollJump) {
      val scrollJumpSize = injector.options(editor).scrolljump
      return if (scrollJumpSize < 0) {
        (height * (min(100, -scrollJumpSize) / 100.0)).toInt()
      } else {
        max(1, scrollJumpSize)
      }
    }
    return 1
  }

  private fun scrollCaretIntoViewHorizontally(editor: Editor, position: VisualPosition) {
    val vimEditor = editor.vim
    val currentVisualLeftColumn = getVisualColumnAtLeftOfDisplay(editor, position.line)
    val currentVisualRightColumn = getVisualColumnAtRightOfDisplay(editor, position.line)
    val caretColumn = position.column
    val halfWidth = getApproximateScreenWidth(editor) / 2
    val scrollOffset = getNormalizedSideScrollOffset(editor)
    val flags = injector.vimState.executingCommandFlags
    val allowSidescroll = !flags.contains(CommandFlags.FLAG_IGNORE_SIDE_SCROLL_JUMP)
    val sidescroll = injector.options(vimEditor).sidescroll
    val offsetLeft = caretColumn - (currentVisualLeftColumn + scrollOffset)
    val offsetRight = caretColumn - (currentVisualRightColumn - scrollOffset)
    if (offsetLeft < 0 || offsetRight > 0) {
      var diff = if (offsetLeft < 0) -offsetLeft else offsetRight
      if (allowSidescroll && sidescroll == 0 || diff >= halfWidth || offsetRight >= offsetLeft) {
        scrollColumnToMiddleOfScreen(editor, position.line, caretColumn)
      } else {
        if (allowSidescroll && diff < sidescroll) {
          diff = sidescroll
        }
        if (offsetLeft < 0) {
          scrollColumnToLeftOfScreen(editor, position.line, max(0, currentVisualLeftColumn - diff))
        } else {
          scrollColumnToRightOfScreen(
            editor,
            position.line,
            vimEditor.normalizeVisualColumn(position.line, currentVisualRightColumn + diff, false),
          )
        }
      }
    }
  }
}
