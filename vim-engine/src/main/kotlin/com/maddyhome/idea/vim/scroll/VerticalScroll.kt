/*
 * This file is licensed under the Vim license
 *
 * Specifically, the functionality is based on Vim's move.c: update_topline, scroll_cursor_top, scroll_cursor_bot,
 * scrollup, scrolldown, check_topfill and cursor_correct
 */
package com.maddyhome.idea.vim.scroll

import kotlin.math.max
import kotlin.math.min

/**
 * The vertical layout of a window, in rows.
 *
 * Lines are visual lines: a soft wrapped segment of a line, a collapsed fold, or a line drawn taller than a row, e.g. a
 * rendered doc comment. Filler is what is drawn between two lines, e.g. block inlays. The window can show only some of
 * the filler rows above its top line, the ones closest to the line.
 */
interface WindowLayout {
  /** The number of rows that show text */
  val height: Int

  /** The number of visual lines */
  val lineCount: Int

  /** The rows taken by [line] itself, at least one */
  fun lineRows(line: Int): Int

  /** The rows of filler between [line] - 1 and [line]. [line] can be [lineCount], for the filler below the last line */
  fun fillerRows(line: Int): Int
}

/** What a window shows: its first line, and how many rows of the filler above that line are visible */
data class WindowView(val topLine: Int, val topFill: Int = 0)

sealed interface ScrollTarget {
  /** No need to scroll */
  object None : ScrollTarget

  /**
   * Scroll to [view]. When [keepBottom] is true, the bottom of the window is what matters, e.g. after scrolling the
   * caret up from the bottom, so the top of the window may move if its height changes after scrolling
   */
  data class Show(val view: WindowView, val keepBottom: Boolean) : ScrollTarget

  /** Put [line] in the middle of the window. When [atEnd] is true, this can scroll past the end of the file */
  data class Centre(val line: Int, val atEnd: Boolean) : ScrollTarget
}

/**
 * Decides what a window should show for a cursor line, with 'scrolloff' and 'scrolljump'.
 *
 * Everything is counted in rows, so soft wraps, folds, tall lines and block inlays are all just rows of a different
 * height. Filler rows count towards 'scrolloff' like any other row.
 */
object VerticalScroll {
  /**
   * Scrolls the cursor line into view, keeping [scrollOffset] rows above and below it, and scrolling at least
   * [scrollJump] rows when scrolling at all
   */
  fun scrollCursorIntoView(
    layout: WindowLayout,
    view: WindowView,
    cursor: Int,
    scrollOffset: Int,
    scrollJump: Int,
  ): ScrollTarget = Window(layout, view, scrollOffset).updateTopLine(cursor, scrollJump)

  /** `zt`: puts the cursor line at the top of the window, below [scrollOffset] rows */
  fun scrollCursorToTop(layout: WindowLayout, view: WindowView, cursor: Int, scrollOffset: Int): ScrollTarget {
    val window = Window(layout, view, scrollOffset)
    return window.scrollCursorTop(cursor, 0, always = true) ?: ScrollTarget.Show(window.view, keepBottom = false)
  }

  /** `zb`: puts the cursor line at the bottom of the window, above [scrollOffset] rows */
  fun scrollCursorToBottom(layout: WindowLayout, view: WindowView, cursor: Int, scrollOffset: Int): ScrollTarget {
    val window = Window(layout, view, scrollOffset)
    return window.scrollCursorBot(cursor, 0, setTopBot = true) ?: ScrollTarget.Show(window.view, keepBottom = true)
  }

  /** The view that shows [line] and the filler below it at the bottom of the window */
  fun viewWithLineAtBottom(layout: WindowLayout, line: Int): WindowView =
    Window(layout, WindowView(0), 0).apply { setBottomLine(line) }.view

  /**
   * `CTRL-E` and `CTRL-Y`: scrolls [count] rows of filler or lines, up for a positive count and down for a negative one
   */
  fun scrollLines(layout: WindowLayout, view: WindowView, count: Int): WindowView {
    val window = Window(layout, view, 0)
    if (count > 0) window.scrollUp(count) else window.scrollDown(-count)
    return window.view
  }

  /**
   * Moves the cursor line into the part of the window that is at least [scrollOffset] rows from the top and the bottom,
   * without scrolling. At the start or end of the file there is nothing to keep, so there is no limit at that side
   */
  fun cursorCorrect(layout: WindowLayout, view: WindowView, cursor: Int, scrollOffset: Int): Int =
    Window(layout, view, scrollOffset).cursorCorrect(cursor)

  /** The first line below the window, or [WindowLayout.lineCount] if the last line is shown */
  fun bottomLine(layout: WindowLayout, view: WindowView): Int = Window(layout, view, 0).botLine
}

/** The rows above the first line, and below the last one, as if they were very tall lines */
private const val OUTSIDE_FILE = Int.MAX_VALUE / 2

/** A line or filler row next to a line: [fill] is the number of filler rows counted next to [line] */
private class LineOffset(var line: Int, var fill: Int = 0, var height: Int = 0)

/**
 * The state of a window: [topLine] and [topFill] are what it shows, [botLine] is the first line that is not fully
 * shown, [emptyRows] are the rows below the last line of the file, and [fillerRowsAtBottom] are the rows of filler above
 * [botLine] that are shown at the bottom of the window
 */
private class Window(private val layout: WindowLayout, view: WindowView, private val scrollOffset: Int) {
  private val height = max(1, layout.height)
  private val lineCount = layout.lineCount

  var topLine = view.topLine.coerceIn(0, max(0, lineCount - 1))
  var topFill = view.topFill.coerceIn(0, fill(topLine))
  var botLine = 0
  private var emptyRows = 0
  private var fillerRowsAtBottom = 0

  init {
    checkTopFill(down = false)
    validateBotLine()
  }

  val view: WindowView
    get() = WindowView(topLine, topFill)

  private fun fill(line: Int): Int = if (line in 0..lineCount) layout.fillerRows(line) else 0

  /** The rows of [line], never more than the window */
  private fun lineRows(line: Int): Int = min(layout.lineRows(line), height)

  /** The rows of [line] with the filler above it, but only the shown filler for the top line */
  private fun lineRowsWithFill(line: Int): Int = (if (line == topLine) topFill else fill(line)) + lineRows(line)

  fun validateBotLine() {
    var used = 0
    var line = topLine
    while (line < lineCount) {
      val rows = lineRowsWithFill(line)
      if (used + rows > height) break
      used += rows
      line++
    }
    botLine = line
    setEmptyRows(used)
  }

  private fun setEmptyRows(used: Int) {
    fillerRowsAtBottom = 0
    if (used == 0) {
      // A single line that doesn't fit
      emptyRows = 0
      return
    }
    emptyRows = height - used
    if (botLine < lineCount) {
      fillerRowsAtBottom = fill(botLine)
      if (emptyRows > fillerRowsAtBottom) {
        emptyRows -= fillerRowsAtBottom
      } else {
        fillerRowsAtBottom = emptyRows
        emptyRows = 0
      }
    }
  }

  /** Adds the filler row or line above [offset] */
  private fun lineAbove(offset: LineOffset, limitToWindow: Boolean = true) {
    if (offset.fill < fill(offset.line)) {
      offset.fill++
      offset.height = 1
    } else {
      offset.line--
      offset.fill = 0
      offset.height = when {
        offset.line < 0 -> OUTSIDE_FILE
        limitToWindow -> lineRows(offset.line)
        else -> layout.lineRows(offset.line)
      }
    }
  }

  /** Adds the filler row or line below [offset] */
  private fun lineBelow(offset: LineOffset) {
    if (offset.fill < fill(offset.line + 1)) {
      offset.fill++
      offset.height = 1
    } else {
      offset.line++
      offset.fill = 0
      offset.height = if (offset.line >= lineCount) OUTSIDE_FILE else lineRows(offset.line)
    }
  }

  /** True if there are fewer than 'scrolloff' rows above the cursor line */
  private fun isInsideTopScrollOffset(cursor: Int): Boolean {
    // Every line is at least a row, so there's no need to count if there are enough lines
    if (cursor.toLong() >= topLine.toLong() + scrollOffset) return false
    val offset = LineOffset(cursor)
    var rows = topFill
    while (rows < scrollOffset) {
      lineAbove(offset)
      if (offset.line < topLine || offset.line == topLine && offset.fill > 0) break
      rows += offset.height
    }
    return rows < scrollOffset
  }

  fun updateTopLine(cursor: Int, scrollJump: Int): ScrollTarget {
    val oldView = view
    var scrolledDown = false

    val checkTopLine = topLine > 0 && (cursor < topLine || isInsideTopScrollOffset(cursor)) || topFill > fill(topLine)
    if (checkTopLine) {
      // If it's a long way, put the cursor in the middle, otherwise at the top
      val halfHeight = max(2, height / 2 - 1)
      val lines = topLine.toLong() + scrollOffset - cursor
      if (lines >= halfHeight && scrollJump < halfHeight) return ScrollTarget.Centre(cursor, atEnd = false)
      scrollCursorTop(cursor, scrollJump, always = false)?.let { return it }
    }

    validateBotLine()
    if (botLine < lineCount && !hasBottomScrollOffset(cursor)) {
      val lines = cursor.toLong() - botLine + 1 + scrollOffset
      if (lines > height + 1) return ScrollTarget.Centre(cursor, atEnd = false)
      scrollCursorBot(cursor, scrollJump, setTopBot = false)?.let { return it }
      scrolledDown = true
    }

    return if (view == oldView) ScrollTarget.None else ScrollTarget.Show(view, keepBottom = scrolledDown)
  }

  /** True if there are at least 'scrolloff' rows below the cursor line, or the rows below are empty */
  private fun hasBottomScrollOffset(cursor: Int): Boolean {
    if (cursor >= botLine) return false
    if (cursor.toLong() < botLine.toLong() - scrollOffset) return true
    var rows = emptyRows + fillerRowsAtBottom
    val offset = LineOffset(cursor)
    while (offset.line < botLine && (offset.line + 1 < botLine || offset.fill == 0)) {
      rows += offset.height
      if (rows >= scrollOffset) break
      lineBelow(offset)
    }
    return rows >= scrollOffset
  }

  /**
   * Puts the cursor line at the top of the window, below 'scrolloff' rows, scrolling at least [minScroll] rows. If
   * [always] is false, it only scrolls up
   *
   * @return The target if the cursor line and its context don't fit and it should be centred instead, or null
   */
  fun scrollCursorTop(cursor: Int, minScroll: Int, always: Boolean): ScrollTarget? {
    var scrolled = 0
    // The filler above the cursor line is part of it
    var used = lineRowsWithFill(cursor)
    if (cursor < topLine) scrolled = used

    var top = cursor - 1
    var bot = cursor + 1
    var newTopLine = cursor
    // The filler above the cursor line counts as context
    var extra = fill(cursor)

    // Move the top line up until it reaches the start of the file, or there is 'scrolloff' context above and below
    // the cursor line, and we've scrolled at least minScroll rows if we're scrolling at all
    while (top >= 0) {
      val rows = lineRows(top)
      if (top < topLine) scrolled += rows
      if ((newTopLine >= topLine || scrolled > minScroll) && extra >= scrollOffset) break

      used += rows
      if (extra + rows <= scrollOffset && bot < lineCount) {
        used += lineRowsWithFill(bot)
      }
      if (used > height) break

      extra += rows
      newTopLine = top
      top--
      bot++
    }

    // Not enough room. This makes sure we get the same position for `j` and `k` in a small window
    if (used > height) return ScrollTarget.Centre(cursor, atEnd = false)

    if (newTopLine < topLine || always) topLine = newTopLine
    topLine = min(topLine, cursor)
    topFill = fill(topLine)
    if (topFill > 0 && extra > scrollOffset) {
      // Only keep as much of the filler as is needed for 'scrolloff'
      topFill = max(0, topFill - (extra - scrollOffset))
    }
    checkTopFill(down = false)
    return null
  }

  /** Sets the top line so that [line] and the filler below it are at the bottom of the window */
  fun setBottomLine(line: Int) {
    var used = 0
    botLine = line + 1
    // The filler below the line can't push the line off the window
    val maxFillerBelow = height - lineRows(line)
    val offset = LineOffset(line + 1)
    while (true) {
      lineAbove(offset, limitToWindow = false)
      if (offset.line > line && used + offset.height > maxFillerBelow) {
        offset.fill = fill(offset.line)
        continue
      }
      if (offset.height == OUTSIDE_FILE || used + offset.height > height) break
      topFill = offset.fill
      topLine = offset.line
      used += offset.height
    }
    setEmptyRows(used)
  }

  /**
   * Puts the cursor line at the bottom of the window, above 'scrolloff' rows, scrolling at least [minScroll] rows. If
   * [setTopBot] is true, it first puts the cursor line at the bottom of the window, for `zb`
   *
   * @return The target if the cursor line and its context don't fit and it should be centred instead, or null
   */
  fun scrollCursorBot(cursor: Int, minScroll: Int, setTopBot: Boolean): ScrollTarget? {
    if (setTopBot) setBottomLine(cursor) else validateBotLine()

    // The rows of the cursor line itself are always used
    var used = lineRows(cursor)

    // If the cursor is on or below the bottom line, we scroll at least the rows of the cursor line. The empty rows are
    // part of the bottom line
    var scrolled = 0
    if (cursor >= botLine) {
      scrolled = used
      if (cursor == botLine) scrolled -= emptyRows
    }

    val above = LineOffset(cursor)
    val below = LineOffset(cursor)
    val fillBelowWindow = fill(botLine) - fillerRowsAtBottom
    var extra = 0

    // Count the rows to scroll until we've scrolled nothing or at least minScroll rows, found 'scrolloff' context
    // below the cursor, and counted all lines below the window
    while (above.line > 0) {
      if (((scrolled <= 0 || scrolled >= minScroll) && extra >= scrollOffset || below.line + 1 >= lineCount) &&
        above.line <= botLine && (above.line < botLine || above.fill >= fillBelowWindow)
      ) {
        break
      }

      val lineBefore = above.line
      lineAbove(above)
      used = if (above.height == OUTSIDE_FILE) OUTSIDE_FILE else used + above.height
      if (used > height) break
      if (above.line >= botLine && (above.line > botLine || above.fill <= fillBelowWindow)) {
        // Count the rows below the window
        scrolled += above.height
        if (above.line == botLine && lineBefore > botLine) scrolled -= emptyRows
      }

      if (below.line < lineCount - 1) {
        val belowBefore = below.line
        lineBelow(below)
        used += below.height
        if (used > height) break
        if (extra < scrollOffset || scrolled < minScroll) {
          extra += below.height
          if (below.line >= botLine || below.line + 1 == botLine && below.fill > fillerRowsAtBottom) {
            // Count the rows below the window
            scrolled += below.height
            if (below.line >= botLine && belowBefore < botLine) scrolled -= emptyRows
          }
        }
      }
    }

    val linesToScroll = when {
      // There are enough empty rows, no need to scroll
      scrolled <= 0 -> 0
      // More than a window full, put the cursor in the middle
      used > height -> used
      else -> {
        // Scroll the minimal number of filler rows and lines, the same steps as scrollUp
        var count = 0
        var rows = 0
        var line = topLine
        var lineFill = topFill
        while (rows < scrolled && line < botLine) {
          if (lineFill > 0) {
            lineFill--
            rows++
          } else {
            rows += lineRows(line)
            line++
            lineFill = fill(line)
          }
          count++
        }
        if (rows < scrolled) OUTSIDE_FILE else count
      }
    }

    if (linesToScroll >= height && linesToScroll > minScroll) return ScrollTarget.Centre(cursor, atEnd = false)
    if (linesToScroll > 0) scrollUp(linesToScroll)
    return null
  }

  /** Scrolls the text up [count] filler rows or lines */
  fun scrollUp(count: Int) {
    for (i in 0 until count) {
      if (topFill > 0) {
        topFill--
      } else {
        if (topLine >= lineCount - 1) break
        topLine++
        topFill = fill(topLine)
      }
    }
    checkTopFill(down = false)
    validateBotLine()
  }

  /** Scrolls the text down [count] filler rows or lines */
  fun scrollDown(count: Int) {
    for (i in 0 until count) {
      val canFill = topFill < height - 1 && topFill < fill(topLine)
      if (topLine == 0 && !canFill) break
      if (canFill) {
        topFill++
      } else {
        topLine--
        topFill = 0
      }
    }
    checkTopFill(down = true)
    validateBotLine()
  }

  /** Makes sure the filler above the top line doesn't push the top line off the window */
  private fun checkTopFill(down: Boolean) {
    if (topFill <= 0) return
    val rows = lineRows(topLine)
    if (topFill + rows > height) {
      if (down && topLine > 0) {
        topLine--
        topFill = 0
      } else {
        topFill = max(0, height - rows)
      }
    }
  }

  fun cursorCorrect(cursor: Int): Int {
    var aboveWanted = scrollOffset
    var belowWanted = scrollOffset
    if (topLine == 0) {
      aboveWanted = 0
      belowWanted = min(belowWanted, height / 2)
    }
    validateBotLine()
    if (botLine >= lineCount) {
      belowWanted = 0
      aboveWanted = min(aboveWanted, (height - 1) / 2)
    }

    if (cursor.toLong() >= topLine.toLong() + aboveWanted && cursor.toLong() < botLine.toLong() - belowWanted) {
      return cursor
    }

    // Narrow down the lines the cursor can be on, taking lines from the top and the bottom until there is enough
    // context, or the two meet. Filler rows count as context
    var top = topLine
    var bot = botLine - 1
    var above = topFill
    var below = fillerRowsAtBottom
    while ((above < aboveWanted || below < belowWanted) && top < bot) {
      if (below < belowWanted && (below <= above || above >= aboveWanted)) {
        below += lineRowsWithFill(bot)
        bot--
      }
      if (above < aboveWanted && (above < below || below >= belowWanted)) {
        above += lineRows(top)
        if (top < bot) above += fill(top + 1)
        top++
      }
    }

    return when {
      top == bot || bot < 0 -> top
      top > bot -> bot
      cursor < top && topLine > 0 -> top
      cursor > bot && botLine < lineCount -> bot
      else -> cursor
    }
  }
}
