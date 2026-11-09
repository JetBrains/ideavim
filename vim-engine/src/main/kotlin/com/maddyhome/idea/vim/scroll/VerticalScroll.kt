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
 * The vertical layout of a window. Heights are in any unit, e.g. pixels, and [rowHeight] is the height of a row of
 * text in that unit.
 *
 * Lines are visual lines: a soft wrapped segment of a line, a collapsed fold, or a line drawn taller than a row, e.g. a
 * rendered doc comment. Filler is what is drawn between two lines, e.g. block inlays. The window can show only some of
 * the filler above its top line, the part closest to the line.
 */
interface WindowLayout {
  /** The height of a row of text. 'scrolloff' and 'scrolljump' are counted in rows */
  val rowHeight: Int

  /** The height that shows text */
  val height: Int

  /** The number of visual lines */
  val lineCount: Int

  /** The height of [line] itself, at least [rowHeight] */
  fun lineHeight(line: Int): Int

  /**
   * The height of the filler between [line] - 1 and [line]. [line] can be [lineCount], for the filler below the last
   * line
   */
  fun fillerHeight(line: Int): Int
}

/** What a window shows: its first line, and how much of the filler above that line is visible */
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
 * Everything is measured by height, so soft wraps, folds, tall lines and block inlays are all just text of a different
 * height. Filler counts towards 'scrolloff' like any other row.
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
   * `CTRL-E` and `CTRL-Y`: scrolls [count] lines or rows of filler, up for a positive count and down for a negative one
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

/** The height above the first line, and below the last one, as if they were very tall lines */
private const val OUTSIDE_FILE = Int.MAX_VALUE / 2

/** The top line for a cursor line, and the height of the context above the cursor line that it gives */
private class TopLine(val line: Int, val context: Int)

/**
 * The state of a window: [topLine] and [topFill] are what it shows, [bottomLine] is the first line that is not fully
 * shown, [empty] is the height below the last line of the file, and [fillerAtBottom] is the filler above [bottomLine]
 * that is shown at the bottom of the window.
 *
 * [scrollOffsetRows] is in rows, everything else is a height, except where it says it counts lines or rows.
 */
private class Window(private val layout: WindowLayout, view: WindowView, private val scrollOffsetRows: Int) {
  private val row = max(1, layout.rowHeight)
  private val height = max(row, layout.height)
  private val heightInRows = height / row
  private val scrollOffset = rowsToHeight(scrollOffsetRows)
  private val lineCount = layout.lineCount

  private var topLine = view.topLine.coerceIn(0, max(0, lineCount - 1))
  private var topFill = view.topFill.coerceIn(0, fill(topLine))
  var bottomLine = 0
    private set
  private var empty = 0
  private var fillerAtBottom = 0

  /** The filler above [bottomLine] that is below the window */
  private val hiddenFillerAtBottom: Int
    get() = fill(bottomLine) - fillerAtBottom

  init {
    keepTopLineInWindow(movingDown = false)
    measureBottom()
  }

  val view: WindowView
    get() = WindowView(topLine, topFill)

  private fun rowsToHeight(rows: Int): Int = (rows.toLong() * row).coerceAtMost(OUTSIDE_FILE.toLong()).toInt()

  private fun fill(line: Int): Int = if (line in 0..lineCount) layout.fillerHeight(line) else 0

  /** The height of [line], never more than the window */
  private fun lineHeight(line: Int): Int = min(max(row, layout.lineHeight(line)), height)

  /** The height of [line] with the filler above it, but only the shown filler for the top line */
  private fun lineHeightWithFill(line: Int): Int = (if (line == topLine) topFill else fill(line)) + lineHeight(line)

  /**
   * Filler scrolls a row at a time, on rows counted from the line below it. The last row, at the top, can be shorter.
   * These give the shown filler a row more or a row less than [shown], out of [total]
   */
  private fun rowMoreFiller(shown: Int, total: Int): Int = min(total, (shown / row + 1) * row)

  private fun rowLessFiller(shown: Int): Int = max(0, (shown - 1) / row * row)

  fun scrollCursorIntoView(cursor: Int, scrollJumpRows: Int): ScrollTarget {
    val oldView = view
    val scrollJump = rowsToHeight(scrollJumpRows)

    if (topLine > 0 && (cursor < topLine || !hasContextAbove(cursor))) {
      if (isFarAboveWindow(cursor, scrollJumpRows)) return centre(cursor)
      if (!showCursorNearTop(cursor, scrollJump)) return centre(cursor)
    }

    measureBottom()
    if (bottomLine < lineCount && !hasContextBelow(cursor)) {
      if (isFarBelowWindow(cursor)) return centre(cursor)
      // It scrolls in whole lines and filler rows, so needing less than a row short of 'scrolljump' scrolls it anyway
      if (!showCursorNearBottom(cursor, max(0, scrollJump - row + 1))) return centre(cursor)
      return showIfChanged(oldView, keepBottom = true)
    }

    return showIfChanged(oldView, keepBottom = false)
  }

  private fun showIfChanged(oldView: WindowView, keepBottom: Boolean): ScrollTarget =
    if (view == oldView) ScrollTarget.None else ScrollTarget.Show(view, keepBottom)

  /** A long way above the window, so it's better to put the cursor in the middle than at the top. Counts lines */
  private fun isFarAboveWindow(cursor: Int, scrollJumpRows: Int): Boolean {
    val halfHeight = max(2, heightInRows / 2 - 1)
    return topLine.toLong() + scrollOffsetRows - cursor >= halfHeight && scrollJumpRows < halfHeight
  }

  /** More than a window below the window, so it's better to put the cursor in the middle than at the bottom */
  private fun isFarBelowWindow(cursor: Int): Boolean =
    cursor.toLong() - bottomLine + 1 + scrollOffsetRows > heightInRows + 1

  /** True if there are 'scrolloff' rows in the window above the cursor line */
  private fun hasContextAbove(cursor: Int): Boolean {
    // Every line is at least a row, so there's no need to measure if there are enough lines
    if (cursor.toLong() >= topLine.toLong() + scrollOffsetRows) return true
    var context = topFill
    val above = WalkUp(cursor)
    while (context < scrollOffset) {
      above.step()
      if (!above.isBelowFillerAtTop()) break
      context += above.height
    }
    return context >= scrollOffset
  }

  /** True if there are 'scrolloff' rows in the window below the cursor line, counting the empty height */
  private fun hasContextBelow(cursor: Int): Boolean {
    if (cursor >= bottomLine) return false
    if (cursor.toLong() < bottomLine.toLong() - scrollOffsetRows) return true
    var context = empty + fillerAtBottom
    val below = WalkDown(cursor)
    while (context < scrollOffset) {
      below.step()
      if (!below.isAboveFillerAtBottom()) break
      context += below.height
    }
    return context >= scrollOffset
  }

  /** `zt`: makes the cursor line the top line, below 'scrolloff' rows. False if they don't fit in the window */
  fun putCursorAtTop(cursor: Int): Boolean {
    val top = topLineFor(cursor, minScroll = 0) ?: return false
    showFromTopLine(top)
    return true
  }

  /**
   * Moves the top of the window up so that there are 'scrolloff' rows above the cursor line, scrolling at least
   * [minScroll]. It never moves the top down. False if the cursor line and its context don't fit in the window
   */
  private fun showCursorNearTop(cursor: Int, minScroll: Int): Boolean {
    val top = topLineFor(cursor, minScroll) ?: return false
    showFromTopLine(TopLine(min(topLine, top.line), top.context))
    return true
  }

  /**
   * The top line that puts [cursor] below 'scrolloff' rows, moving up at least [minScroll] if it moves up at all.
   * Null if there isn't room for the same context below the cursor line
   */
  private fun topLineFor(cursor: Int, minScroll: Int): TopLine? {
    var newTopLine = cursor
    var lineBelow = cursor + 1
    // The filler above the cursor line is part of it, and counts as context
    var used = lineHeightWithFill(cursor)
    var context = fill(cursor)
    var scrolled = if (cursor < topLine) used else 0

    while (newTopLine > 0) {
      val lineAbove = lineHeight(newTopLine - 1)
      if (newTopLine - 1 < topLine) scrolled += lineAbove
      if (context >= scrollOffset && (newTopLine >= topLine || scrolled > minScroll)) break

      used += lineAbove
      // Keep room for the context below the cursor line
      if (context + lineAbove <= scrollOffset && lineBelow < lineCount) used += lineHeightWithFill(lineBelow)
      if (used > height) break

      context += lineAbove
      newTopLine--
      lineBelow++
    }

    return if (used > height) null else TopLine(newTopLine, context)
  }

  /** Shows [top] at the top of the window, with only as much of the filler above it as 'scrolloff' needs */
  private fun showFromTopLine(top: TopLine) {
    topLine = top.line
    topFill = max(0, fill(topLine) - max(0, top.context - scrollOffset))
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
    var used = min(fill(line + 1), height - lineHeight(line))
    val above = WalkUp(line + 1, fill = fill(line + 1), limitToWindow = false)
    while (true) {
      above.step()
      if (above.height == OUTSIDE_FILE || used + above.height > height) break
      topLine = above.line
      topFill = above.fill
      used += above.height
    }
    measureBelowText(used)
  }

  /**
   * Moves the top of the window down so that there are 'scrolloff' rows below the cursor line, scrolling at least
   * [minScroll]. False if the cursor line and its context don't fit in the window
   */
  private fun showCursorNearBottom(cursor: Int, minScroll: Int): Boolean {
    val count = BottomContext(cursor, minScroll).apply { count() }
    if (count.scrolled <= 0) return true

    // When it doesn't fit, a step per row is as good a guess as any
    val steps = if (count.used > height) count.used / row else stepsToScrollUp(count.scrolled)
    if (steps >= heightInRows && steps.toLong() * row > minScroll) return false
    scrollUp(steps)
    return true
  }

  /**
   * Measures how far to scroll to show [cursor] at the bottom of the window with 'scrolloff' rows below it. It takes a
   * step above and a step below the cursor line in turn, until there is enough context below the cursor line, at least
   * [minScroll] is scrolled, and the height below the window is measured
   */
  private inner class BottomContext(cursor: Int, private val minScroll: Int) {
    private val above = WalkUp(cursor)
    private val below = WalkDown(cursor)
    private var context = 0

    /** The height of the window that would show the cursor line and the text around it */
    var used = lineHeight(cursor)
      private set

    /** The height to scroll. The empty height is part of the bottom line, so it doesn't need scrolling */
    var scrolled = when {
      cursor > bottomLine -> used
      cursor == bottomLine -> used - empty
      else -> 0
    }
      private set

    fun count() {
      while (above.line > 0 && !isDone()) {
        addStepAbove()
        if (used > height) return
        if (below.line < lineCount - 1) {
          addStepBelow()
          if (used > height) return
        }
      }
    }

    private fun isDone(): Boolean {
      val scrolledEnough = scrolled <= 0 || scrolled >= minScroll
      val enoughBelow = scrolledEnough && context >= scrollOffset || below.line + 1 >= lineCount
      return enoughBelow && above.hasMeasuredBelowWindow()
    }

    private fun addStepAbove() {
      val wasBelowBottomLine = above.line > bottomLine
      above.step()
      used += above.height
      val hidden = above.heightBelowWindow()
      if (used > height || hidden == 0) return
      scrolled += hidden
      if (above.line == bottomLine && wasBelowBottomLine) scrolled -= empty
    }

    private fun addStepBelow() {
      val wasAboveBottomLine = below.line < bottomLine
      below.step()
      used += below.height
      if (used > height || context >= scrollOffset && scrolled >= minScroll) return
      context += below.height
      val hidden = below.heightBelowWindow()
      if (hidden == 0) return
      scrolled += hidden
      if (below.line >= bottomLine && wasAboveBottomLine) scrolled -= empty
    }
  }

  /** The filler rows and lines [scrollUp] has to scroll to scroll [distance], or [OUTSIDE_FILE] if it can't */
  private fun stepsToScrollUp(distance: Int): Int {
    var steps = 0
    var scrolled = 0
    var line = topLine
    var lineFill = topFill
    while (scrolled < distance && line < bottomLine) {
      if (lineFill > 0) {
        val less = rowLessFiller(lineFill)
        scrolled += lineFill - less
        lineFill = less
      } else {
        scrolled += lineHeight(line)
        line++
        lineFill = fill(line)
      }
      steps++
    }
    return if (scrolled < distance) OUTSIDE_FILE else steps
  }

  /** Scrolls the text up [count] filler rows or lines */
  fun scrollUp(count: Int) {
    for (i in 0 until count) {
      if (topFill > 0) {
        topFill = rowLessFiller(topFill)
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
      val moreFill = rowMoreFiller(topFill, fill(topLine))
      // Leave at least a row for the top line
      val canShowMoreFill = topFill < fill(topLine) && moreFill <= height - row
      if (canShowMoreFill) {
        topFill = moreFill
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
    val topLineHeight = lineHeight(topLine)
    if (topFill <= 0 || topFill + topLineHeight <= height) return
    if (movingDown && topLine > 0) {
      topLine--
      topFill = 0
    } else {
      topFill = max(0, height - topLineHeight)
    }
  }

  /** Finds [bottomLine] and the height below the last fully shown line */
  private fun measureBottom() {
    var used = 0
    var line = topLine
    while (line < lineCount && used + lineHeightWithFill(line) <= height) {
      used += lineHeightWithFill(line)
      line++
    }
    bottomLine = line
    measureBelowText(used)
  }

  /** Splits the height below the last fully shown line into the filler above [bottomLine] and the empty height */
  private fun measureBelowText(used: Int) {
    // Nothing is shown when a single line doesn't fit
    val free = if (used == 0) 0 else height - used
    fillerAtBottom = if (bottomLine < lineCount) min(fill(bottomLine), free) else 0
    empty = free - fillerAtBottom
  }

  fun cursorCorrect(cursor: Int): Int {
    measureBottom()
    val atStart = topLine == 0
    val atEnd = bottomLine >= lineCount
    val rowsAboveWanted = when {
      atStart -> 0
      atEnd -> min(scrollOffsetRows, (heightInRows - 1) / 2)
      else -> scrollOffsetRows
    }
    val rowsBelowWanted = when {
      atEnd -> 0
      atStart -> min(scrollOffsetRows, heightInRows / 2)
      else -> scrollOffsetRows
    }

    // Every line is at least a row, so there's no need to measure if there are enough lines
    if (cursor.toLong() >= topLine.toLong() + rowsAboveWanted &&
      cursor.toLong() < bottomLine.toLong() - rowsBelowWanted
    ) {
      return cursor
    }

    val (top, bottom) = linesWithContext(rowsToHeight(rowsAboveWanted), rowsToHeight(rowsBelowWanted))
    return when {
      top == bottom || bottom < 0 -> top
      top > bottom -> bottom
      cursor < top && !atStart -> top
      cursor > bottom && !atEnd -> bottom
      else -> cursor
    }
  }

  /**
   * The first and last lines of the window that have the wanted context above and below them. It takes lines from the
   * top and the bottom in turn until there is enough context, or the two meet. Filler counts as context
   */
  private fun linesWithContext(aboveWanted: Int, belowWanted: Int): Pair<Int, Int> {
    var top = topLine
    var bottom = bottomLine - 1
    var above = topFill
    var below = fillerAtBottom
    while ((above < aboveWanted || below < belowWanted) && top < bottom) {
      if (below < belowWanted && (below <= above || above >= aboveWanted)) {
        below += lineHeightWithFill(bottom)
        bottom--
      }
      if (above < aboveWanted && (above < below || below >= belowWanted)) {
        above += lineHeight(top) + fill(top + 1)
        top++
      }
    }
    return top to bottom
  }

  /**
   * Walks up from [line], a filler row or a line at a time. [fill] is the filler above [line] it has passed, and
   * [height] is the height of the last step
   */
  private inner class WalkUp(var line: Int, var fill: Int = 0, private val limitToWindow: Boolean = true) {
    var height = 0
      private set

    fun step() {
      if (fill < fill(line)) {
        val more = rowMoreFiller(fill, fill(line))
        height = more - fill
        fill = more
      } else {
        line--
        fill = 0
        height = when {
          line < 0 -> OUTSIDE_FILE
          limitToWindow -> lineHeight(line)
          else -> max(row, layout.lineHeight(line))
        }
      }
    }

    /** True if it is still below the filler shown above the top line */
    fun isBelowFillerAtTop(): Boolean = line > topLine || line == topLine && fill == 0

    /** How much of the last step is below the window. A row of filler can be partly shown at the bottom */
    fun heightBelowWindow(): Int = when {
      line > bottomLine -> height
      line < bottomLine -> 0
      fill == 0 -> height
      else -> max(0, min(fill, hiddenFillerAtBottom) - (fill - height))
    }

    /** True if the next step is inside the window */
    fun hasMeasuredBelowWindow(): Boolean = line < bottomLine || line == bottomLine && fill >= hiddenFillerAtBottom
  }

  /**
   * Walks down from [line], a filler row or a line at a time. [fill] is the filler below [line] it has passed, and
   * [height] is the height of the last step
   */
  private inner class WalkDown(var line: Int) {
    var fill = 0
      private set
    var height = 0
      private set

    fun step() {
      if (fill < fill(line + 1)) {
        height = min(row, fill(line + 1) - fill)
        fill += height
      } else {
        line++
        fill = 0
        height = if (line >= lineCount) OUTSIDE_FILE else lineHeight(line)
      }
    }

    /** True if it is still above the filler shown below the last line of the window */
    fun isAboveFillerAtBottom(): Boolean = line < bottomLine && (line + 1 < bottomLine || fill == 0)

    /** How much of the last step is below the window. A row of filler can be partly shown at the bottom */
    fun heightBelowWindow(): Int = when {
      line >= bottomLine -> height
      line + 1 == bottomLine && fill > 0 -> max(0, fill - max(fill - height, fillerAtBottom))
      else -> 0
    }
  }
}
