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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.helper.EditorHelper.getApproximateScreenWidth
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualColumnAtLeftOfDisplay
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualColumnAtRightOfDisplay
import com.maddyhome.idea.vim.helper.EditorHelper.scrollColumnToLeftOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollColumnToMiddleOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollColumnToRightOfScreen
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.scroll.VerticalScroll
import kotlin.math.max
import kotlin.math.min

/**
 * Scrolls the caret into view, see [VerticalScroll] for the vertical part
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
    val layout = EditorWindowLayout(editor)
    val scrollOffset = injector.options(editor.vim).scrolloff
    val scrollJump = getScrollJump(editor.vim, layout.height)
    val target = VerticalScroll.scrollCursorIntoView(layout, layout.view, caretLine, scrollOffset, scrollJump)
    EditorHelper.scrollTo(editor, target, caretChangedLine)
  }

  // Scrolljump is the minimal number of rows to scroll
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
