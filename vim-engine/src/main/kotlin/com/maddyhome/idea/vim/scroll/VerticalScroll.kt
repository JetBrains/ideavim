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
  ): ScrollTarget = Window(layout, view, scrollOffset).scrollCursorIntoView(cursor, scrollJump)

  /** `zt`: puts the cursor line at the top of the window, below [scrollOffset] rows */
  fun scrollCursorToTop(layout: WindowLayout, view: WindowView, cursor: Int, scrollOffset: Int): ScrollTarget {
    val window = Window(layout, view, scrollOffset)
    if (!window.putCursorAtTop(cursor)) return centre(cursor)
    return ScrollTarget.Show(window.view, keepBottom = false)
  }

  /** `zb`: puts the cursor line at the bottom of the window, above [scrollOffset] rows */
  fun scrollCursorToBottom(layout: WindowLayout, view: WindowView, cursor: Int, scrollOffset: Int): ScrollTarget {
    val window = Window(layout, view, scrollOffset)
    if (!window.putCursorAtBottom(cursor)) return centre(cursor)
    return ScrollTarget.Show(window.view, keepBottom = true)
  }

  /** The view that shows [line] and the filler below it at the bottom of the window */
  fun viewWithLineAtBottom(layout: WindowLayout, line: Int): WindowView =
    Window(layout, WindowView(0), 0).apply { showLineAtBottom(line) }.view

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
  fun bottomLine(layout: WindowLayout, view: WindowView): Int = Window(layout, view, 0).bottomLine
}

private fun centre(cursor: Int) = ScrollTarget.Centre(cursor, atEnd = false)

/** The rows above the first line, and below the last one, as if they were very tall lines */
private const val OUTSIDE_FILE = Int.MAX_VALUE / 2

/** The top line for a cursor line, and the rows of context above the cursor line that it gives */
private class TopLine(val line: Int, val contextRows: Int)

/**
 * The state of a window: [topLine] and [topFill] are what it shows, [bottomLine] is the first line that is not fully
 * shown, [emptyRows] are the rows below the last line of the file, and [fillerRowsAtBottom] are the rows of filler above
 * [bottomLine] that are shown at the bottom of the window
 */
private class Window(private val layout: WindowLayout, view: WindowView, private val scrollOffset: Int) {
  private val height = max(1, layout.height)
  private val lineCount = layout.lineCount

  private var topLine = view.topLine.coerceIn(0, max(0, lineCount - 1))
  private var topFill = view.topFill.coerceIn(0, fill(topLine))
  var bottomLine = 0
    private set
  private var emptyRows = 0
  private var fillerRowsAtBottom = 0

  /** The rows of filler above [bottomLine] that are below the window */
  private val hiddenFillerAtBottom: Int
    get() = fill(bottomLine) - fillerRowsAtBottom

  init {
    keepTopLineInWindow(movingDown = false)
    measureBottom()
  }

  val view: WindowView
    get() = WindowView(topLine, topFill)

  private fun fill(line: Int): Int = if (line in 0..lineCount) layout.fillerRows(line) else 0

  /** The rows of [line], never more than the window */
  private fun lineRows(line: Int): Int = min(layout.lineRows(line), height)

  /** The rows of [line] with the filler above it, but only the shown filler for the top line */
  private fun lineRowsWithFill(line: Int): Int = (if (line == topLine) topFill else fill(line)) + lineRows(line)

  fun scrollCursorIntoView(cursor: Int, scrollJump: Int): ScrollTarget {
    val oldView = view

    if (topLine > 0 && (cursor < topLine || !hasContextAbove(cursor))) {
      if (isFarAboveWindow(cursor, scrollJump)) return centre(cursor)
      if (!showCursorNearTop(cursor, scrollJump)) return centre(cursor)
    }

    measureBottom()
    if (bottomLine < lineCount && !hasContextBelow(cursor)) {
      if (isFarBelowWindow(cursor)) return centre(cursor)
      if (!showCursorNearBottom(cursor, scrollJump)) return centre(cursor)
      return showIfChanged(oldView, keepBottom = true)
    }

    return showIfChanged(oldView, keepBottom = false)
  }

  private fun showIfChanged(oldView: WindowView, keepBottom: Boolean): ScrollTarget =
    if (view == oldView) ScrollTarget.None else ScrollTarget.Show(view, keepBottom)

  /** A long way above the window, so it's better to put the cursor in the middle than at the top */
  private fun isFarAboveWindow(cursor: Int, scrollJump: Int): Boolean {
    val halfHeight = max(2, height / 2 - 1)
    return topLine.toLong() + scrollOffset - cursor >= halfHeight && scrollJump < halfHeight
  }

  /** More than a window below the window, so it's better to put the cursor in the middle than at the bottom */
  private fun isFarBelowWindow(cursor: Int): Boolean = cursor.toLong() - bottomLine + 1 + scrollOffset > height + 1

  /** True if there are 'scrolloff' rows in the window above the cursor line */
  private fun hasContextAbove(cursor: Int): Boolean {
    // Every line is at least a row, so there's no need to count if there are enough lines
    if (cursor.toLong() >= topLine.toLong() + scrollOffset) return true
    var rows = topFill
    val above = WalkUp(cursor)
    while (rows < scrollOffset) {
      above.step()
      if (!above.isBelowFillerAtTop()) break
      rows += above.rows
    }
    return rows >= scrollOffset
  }

  /** True if there are 'scrolloff' rows in the window below the cursor line, counting the empty rows */
  private fun hasContextBelow(cursor: Int): Boolean {
    if (cursor >= bottomLine) return false
    if (cursor.toLong() < bottomLine.toLong() - scrollOffset) return true
    var rows = emptyRows + fillerRowsAtBottom
    val below = WalkDown(cursor)
    while (rows < scrollOffset) {
      below.step()
      if (!below.isAboveFillerAtBottom()) break
      rows += below.rows
    }
    return rows >= scrollOffset
  }

  /** `zt`: makes the cursor line the top line, below 'scrolloff' rows. False if they don't fit in the window */
  fun putCursorAtTop(cursor: Int): Boolean {
    val top = topLineFor(cursor, minScroll = 0) ?: return false
    showFromTopLine(top)
    return true
  }

  /**
   * Moves the top of the window up so that there are 'scrolloff' rows above the cursor line, scrolling at least
   * [minScroll] rows. It never moves the top down. False if the cursor line and its context don't fit in the window
   */
  private fun showCursorNearTop(cursor: Int, minScroll: Int): Boolean {
    val top = topLineFor(cursor, minScroll) ?: return false
    showFromTopLine(TopLine(min(topLine, top.line), top.contextRows))
    return true
  }

  /**
   * The top line that puts [cursor] below 'scrolloff' rows, moving at least [minScroll] rows up if it moves up at all.
   * Null if there isn't room for the same context below the cursor line
   */
  private fun topLineFor(cursor: Int, minScroll: Int): TopLine? {
    var newTopLine = cursor
    var lineBelow = cursor + 1
    // The filler above the cursor line is part of it, and counts as context
    var usedRows = lineRowsWithFill(cursor)
    var contextRows = fill(cursor)
    var scrolledRows = if (cursor < topLine) usedRows else 0

    while (newTopLine > 0) {
      val rows = lineRows(newTopLine - 1)
      if (newTopLine - 1 < topLine) scrolledRows += rows
      if (contextRows >= scrollOffset && (newTopLine >= topLine || scrolledRows > minScroll)) break

      usedRows += rows
      // Keep room for the context below the cursor line
      if (contextRows + rows <= scrollOffset && lineBelow < lineCount) usedRows += lineRowsWithFill(lineBelow)
      if (usedRows > height) break

      contextRows += rows
      newTopLine--
      lineBelow++
    }

    return if (usedRows > height) null else TopLine(newTopLine, contextRows)
  }

  /** Shows [top] at the top of the window, with only as much of the filler above it as 'scrolloff' needs */
  private fun showFromTopLine(top: TopLine) {
    topLine = top.line
    topFill = max(0, fill(topLine) - max(0, top.contextRows - scrollOffset))
    keepTopLineInWindow(movingDown = false)
  }

  /** `zb`: puts the cursor line at the bottom of the window, above 'scrolloff' rows. False if they don't fit */
  fun putCursorAtBottom(cursor: Int): Boolean {
    showLineAtBottom(cursor)
    return showCursorNearBottom(cursor, minScroll = 0)
  }

  /** Sets the top line so that [line] and the filler below it are at the bottom of the window */
  fun showLineAtBottom(line: Int) {
    bottomLine = line + 1
    // The filler below the line can't push the line off the window
    var usedRows = min(fill(line + 1), height - lineRows(line))
    val above = WalkUp(line + 1, fill = fill(line + 1), limitToWindow = false)
    while (true) {
      above.step()
      if (above.rows == OUTSIDE_FILE || usedRows + above.rows > height) break
      topLine = above.line
      topFill = above.fill
      usedRows += above.rows
    }
    measureRowsBelowText(usedRows)
  }

  /**
   * Moves the top of the window down so that there are 'scrolloff' rows below the cursor line, scrolling at least
   * [minScroll] rows. False if the cursor line and its context don't fit in the window
   */
  private fun showCursorNearBottom(cursor: Int, minScroll: Int): Boolean {
    val count = BottomContext(cursor, minScroll).apply { count() }
    if (count.scrolledRows <= 0) return true

    val steps = if (count.usedRows > height) count.usedRows else stepsToScrollUp(count.scrolledRows)
    if (steps >= height && steps > minScroll) return false
    scrollUp(steps)
    return true
  }

  /**
   * Counts the rows to scroll to show [cursor] at the bottom of the window with 'scrolloff' rows below it. It takes a
   * row above and a row below the cursor line in turn, until there is enough context below the cursor line, at least
   * [minScroll] rows are scrolled, and the rows below the window are counted
   */
  private inner class BottomContext(cursor: Int, private val minScroll: Int) {
    private val above = WalkUp(cursor)
    private val below = WalkDown(cursor)
    private var contextRows = 0

    /** The rows of the window that would show the cursor line and the rows around it */
    var usedRows = lineRows(cursor)
      private set

    /** The rows to scroll. The empty rows are part of the bottom line, so they don't need scrolling */
    var scrolledRows = when {
      cursor > bottomLine -> usedRows
      cursor == bottomLine -> usedRows - emptyRows
      else -> 0
    }
      private set

    fun count() {
      while (above.line > 0 && !isDone()) {
        addRowAbove()
        if (usedRows > height) return
        if (below.line < lineCount - 1) {
          addRowBelow()
          if (usedRows > height) return
        }
      }
    }

    private fun isDone(): Boolean {
      val scrolledEnough = scrolledRows <= 0 || scrolledRows >= minScroll
      val enoughBelow = scrolledEnough && contextRows >= scrollOffset || below.line + 1 >= lineCount
      return enoughBelow && above.hasCountedRowsBelowWindow()
    }

    private fun addRowAbove() {
      val wasBelowBottomLine = above.line > bottomLine
      above.step()
      usedRows += above.rows
      if (usedRows > height || !above.isBelowWindow()) return
      scrolledRows += above.rows
      if (above.line == bottomLine && wasBelowBottomLine) scrolledRows -= emptyRows
    }

    private fun addRowBelow() {
      val wasAboveBottomLine = below.line < bottomLine
      below.step()
      usedRows += below.rows
      if (usedRows > height || contextRows >= scrollOffset && scrolledRows >= minScroll) return
      contextRows += below.rows
      if (!below.isBelowWindow()) return
      scrolledRows += below.rows
      if (below.line >= bottomLine && wasAboveBottomLine) scrolledRows -= emptyRows
    }
  }

  /** The filler rows and lines [scrollUp] has to scroll to scroll [rows] rows, or [OUTSIDE_FILE] if it can't */
  private fun stepsToScrollUp(rows: Int): Int {
    var steps = 0
    var scrolledRows = 0
    var line = topLine
    var lineFill = topFill
    while (scrolledRows < rows && line < bottomLine) {
      if (lineFill > 0) {
        lineFill--
        scrolledRows++
      } else {
        scrolledRows += lineRows(line)
        line++
        lineFill = fill(line)
      }
      steps++
    }
    return if (scrolledRows < rows) OUTSIDE_FILE else steps
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
    keepTopLineInWindow(movingDown = false)
    measureBottom()
  }

  /** Scrolls the text down [count] filler rows or lines */
  fun scrollDown(count: Int) {
    for (i in 0 until count) {
      val canShowMoreFill = topFill < height - 1 && topFill < fill(topLine)
      if (canShowMoreFill) {
        topFill++
      } else {
        if (topLine == 0) break
        topLine--
        topFill = 0
      }
    }
    keepTopLineInWindow(movingDown = true)
    measureBottom()
  }

  /** Makes sure the filler above the top line doesn't push the top line off the window */
  private fun keepTopLineInWindow(movingDown: Boolean) {
    val rows = lineRows(topLine)
    if (topFill <= 0 || topFill + rows <= height) return
    if (movingDown && topLine > 0) {
      topLine--
      topFill = 0
    } else {
      topFill = max(0, height - rows)
    }
  }

  /** Finds [bottomLine] and the rows below the last fully shown line */
  private fun measureBottom() {
    var usedRows = 0
    var line = topLine
    while (line < lineCount && usedRows + lineRowsWithFill(line) <= height) {
      usedRows += lineRowsWithFill(line)
      line++
    }
    bottomLine = line
    measureRowsBelowText(usedRows)
  }

  /** Splits the rows below the last fully shown line into the filler above [bottomLine] and the empty rows */
  private fun measureRowsBelowText(usedRows: Int) {
    // Nothing is shown when a single line doesn't fit
    val freeRows = if (usedRows == 0) 0 else height - usedRows
    fillerRowsAtBottom = if (bottomLine < lineCount) min(fill(bottomLine), freeRows) else 0
    emptyRows = freeRows - fillerRowsAtBottom
  }

  fun cursorCorrect(cursor: Int): Int {
    measureBottom()
    val atStart = topLine == 0
    val atEnd = bottomLine >= lineCount
    val rowsAboveWanted = when {
      atStart -> 0
      atEnd -> min(scrollOffset, (height - 1) / 2)
      else -> scrollOffset
    }
    val rowsBelowWanted = when {
      atEnd -> 0
      atStart -> min(scrollOffset, height / 2)
      else -> scrollOffset
    }

    if (cursor.toLong() >= topLine.toLong() + rowsAboveWanted &&
      cursor.toLong() < bottomLine.toLong() - rowsBelowWanted
    ) {
      return cursor
    }

    val (top, bottom) = linesWithContext(rowsAboveWanted, rowsBelowWanted)
    return when {
      top == bottom || bottom < 0 -> top
      top > bottom -> bottom
      cursor < top && !atStart -> top
      cursor > bottom && !atEnd -> bottom
      else -> cursor
    }
  }

  /**
   * The first and last lines of the window that have the wanted rows of context above and below them. It takes lines
   * from the top and the bottom in turn until there is enough context, or the two meet. Filler rows count as context
   */
  private fun linesWithContext(rowsAboveWanted: Int, rowsBelowWanted: Int): Pair<Int, Int> {
    var top = topLine
    var bottom = bottomLine - 1
    var rowsAbove = topFill
    var rowsBelow = fillerRowsAtBottom
    while ((rowsAbove < rowsAboveWanted || rowsBelow < rowsBelowWanted) && top < bottom) {
      if (rowsBelow < rowsBelowWanted && (rowsBelow <= rowsAbove || rowsAbove >= rowsAboveWanted)) {
        rowsBelow += lineRowsWithFill(bottom)
        bottom--
      }
      if (rowsAbove < rowsAboveWanted && (rowsAbove < rowsBelow || rowsBelow >= rowsBelowWanted)) {
        rowsAbove += lineRows(top) + fill(top + 1)
        top++
      }
    }
    return top to bottom
  }

  /**
   * Walks up from [line], a filler row or a line at a time. [fill] is the number of filler rows above [line] it has
   * passed, and [rows] are the rows of the last step
   */
  private inner class WalkUp(var line: Int, var fill: Int = 0, private val limitToWindow: Boolean = true) {
    var rows = 0
      private set

    fun step() {
      if (fill < fill(line)) {
        fill++
        rows = 1
      } else {
        line--
        fill = 0
        rows = when {
          line < 0 -> OUTSIDE_FILE
          limitToWindow -> lineRows(line)
          else -> layout.lineRows(line)
        }
      }
    }

    /** True if it is still below the filler shown above the top line */
    fun isBelowFillerAtTop(): Boolean = line > topLine || line == topLine && fill == 0

    /** True if the last step was a filler row or line below the window */
    fun isBelowWindow(): Boolean = line > bottomLine || line == bottomLine && fill <= hiddenFillerAtBottom

    /** True if the next step is inside the window */
    fun hasCountedRowsBelowWindow(): Boolean = line < bottomLine || line == bottomLine && fill >= hiddenFillerAtBottom
  }

  /**
   * Walks down from [line], a filler row or a line at a time. [fill] is the number of filler rows below [line] it has
   * passed, and [rows] are the rows of the last step
   */
  private inner class WalkDown(var line: Int) {
    var fill = 0
      private set
    var rows = 0
      private set

    fun step() {
      if (fill < fill(line + 1)) {
        fill++
        rows = 1
      } else {
        line++
        fill = 0
        rows = if (line >= lineCount) OUTSIDE_FILE else lineRows(line)
      }
    }

    /** True if it is still above the filler shown below the last line of the window */
    fun isAboveFillerAtBottom(): Boolean = line < bottomLine && (line + 1 < bottomLine || fill == 0)

    /** True if the last step was a filler row or line below the window */
    fun isBelowWindow(): Boolean = line >= bottomLine || line + 1 == bottomLine && fill > fillerRowsAtBottom
  }
}
