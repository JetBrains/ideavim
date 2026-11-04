/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.scroll

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VerticalScrollTest {
  /** A window of [height] rows on [lineCount] one row lines, with optional taller lines and filler */
  private class Layout(
    override val height: Int = 35,
    override val lineCount: Int = 200,
    private val rows: Map<Int, Int> = emptyMap(),
    private val filler: Map<Int, Int> = emptyMap(),
  ) : WindowLayout {
    override fun lineRows(line: Int) = rows[line] ?: 1
    override fun fillerRows(line: Int) = filler[line] ?: 0
  }

  private fun show(topLine: Int, topFill: Int = 0, keepBottom: Boolean) =
    ScrollTarget.Show(WindowView(topLine, topFill), keepBottom)

  @Test
  fun `test no scrolling when the cursor is inside the window`() {
    val target = VerticalScroll.scrollCursorIntoView(Layout(), WindowView(10), 20, 5, 1)
    assertEquals(ScrollTarget.None, target)
  }

  @Test
  fun `test moving down into scrolloff scrolls one line`() {
    // Lines 10 to 44 are shown, line 40 has only 4 lines below it
    val target = VerticalScroll.scrollCursorIntoView(Layout(), WindowView(10), 40, 5, 1)
    assertEquals(show(11, keepBottom = true), target)
  }

  @Test
  fun `test moving up into scrolloff scrolls one line`() {
    val target = VerticalScroll.scrollCursorIntoView(Layout(), WindowView(10), 14, 5, 1)
    assertEquals(show(9, keepBottom = false), target)
  }

  @Test
  fun `test scrolljump scrolls at least that many lines`() {
    assertEquals(show(20, keepBottom = true), VerticalScroll.scrollCursorIntoView(Layout(), WindowView(10), 45, 0, 10))
    assertEquals(show(0, keepBottom = false), VerticalScroll.scrollCursorIntoView(Layout(), WindowView(10), 9, 0, 10))
  }

  @Test
  fun `test moving far down centres the cursor`() {
    val target = VerticalScroll.scrollCursorIntoView(Layout(), WindowView(10), 100, 0, 1)
    assertEquals(ScrollTarget.Centre(100, atEnd = false), target)
  }

  @Test
  fun `test moving more than half a window up centres the cursor`() {
    val target = VerticalScroll.scrollCursorIntoView(Layout(), WindowView(50), 33, 0, 1)
    assertEquals(ScrollTarget.Centre(33, atEnd = false), target)
  }

  @Test
  fun `test large scrolloff centres the cursor`() {
    val target = VerticalScroll.scrollCursorIntoView(Layout(), WindowView(10), 20, 999, 1)
    assertEquals(ScrollTarget.Centre(20, atEnd = false), target)
  }

  @Test
  fun `test no scrolling down when the last line is shown`() {
    val layout = Layout(lineCount = 40)
    assertEquals(ScrollTarget.None, VerticalScroll.scrollCursorIntoView(layout, WindowView(5), 39, 5, 1))
  }

  @Test
  fun `test filler rows below the cursor line count towards scrolloff`() {
    // Line 30 is followed by 10 rows of filler, so there are enough rows below line 30 on row 20
    val layout = Layout(filler = mapOf(31 to 10))
    assertEquals(ScrollTarget.None, VerticalScroll.scrollCursorIntoView(layout, WindowView(10), 30, 5, 1))
  }

  @Test
  fun `test tall filler below each line keeps scrolloff in rows`() {
    // Each line is followed by 15 rows of filler. Line 22 is on row 32, so it needs 3 more rows below it: line 20 and
    // two rows of the filler above line 21 scroll off
    val layout = Layout(filler = (1..200).associateWith { 15 })
    val target = VerticalScroll.scrollCursorIntoView(layout, WindowView(20), 22, 5, 1)
    assertEquals(show(21, topFill = 13, keepBottom = true), target)
  }

  @Test
  fun `test tall line counts all its rows`() {
    // Line 30 is 10 rows high, so line 26 on row 16 still has 8 rows below it before line 30 ends at row 30
    val layout = Layout(rows = mapOf(30 to 10))
    assertEquals(ScrollTarget.None, VerticalScroll.scrollCursorIntoView(layout, WindowView(10), 26, 5, 1))
  }

  @Test
  fun `test zt keeps scrolloff rows above the cursor line`() {
    assertEquals(show(45, keepBottom = false), VerticalScroll.scrollCursorToTop(Layout(), WindowView(10), 50, 5))
  }

  @Test
  fun `test zt only keeps scrolloff rows of the filler above the cursor line`() {
    val layout = Layout(filler = mapOf(50 to 10))
    assertEquals(show(50, keepBottom = false), VerticalScroll.scrollCursorToTop(layout, WindowView(10), 50, 0))
    assertEquals(show(50, 3, keepBottom = false), VerticalScroll.scrollCursorToTop(layout, WindowView(10), 50, 3))
  }

  @Test
  fun `test zb keeps scrolloff rows below the cursor line`() {
    assertEquals(show(21, keepBottom = true), VerticalScroll.scrollCursorToBottom(Layout(), WindowView(10), 50, 5))
  }

  @Test
  fun `test line at bottom shows the filler below it`() {
    // 3 rows of filler and line 50 leave 31 rows for lines 19 to 49
    val layout = Layout(filler = mapOf(51 to 3))
    assertEquals(WindowView(19), VerticalScroll.viewWithLineAtBottom(layout, 50))
  }

  @Test
  fun `test line at bottom keeps the line on the window when the filler below it is too tall`() {
    val layout = Layout(filler = mapOf(51 to 50))
    assertEquals(WindowView(50), VerticalScroll.viewWithLineAtBottom(layout, 50))
  }

  @Test
  fun `test scrolling up steps through the filler one row at a time`() {
    val layout = Layout(filler = mapOf(31 to 3))
    assertEquals(WindowView(31, 3), VerticalScroll.scrollLines(layout, WindowView(30), 1))
    assertEquals(WindowView(31, 1), VerticalScroll.scrollLines(layout, WindowView(31, 3), 2))
    assertEquals(WindowView(30), VerticalScroll.scrollLines(layout, WindowView(31, 2), -2))
  }

  @Test
  fun `test filler taller than the window keeps the top line on the window`() {
    val layout = Layout(filler = mapOf(31 to 50))
    assertEquals(WindowView(31, 34), VerticalScroll.scrollLines(layout, WindowView(30), 1))
  }

  @Test
  fun `test cursor correct keeps scrolloff rows`() {
    val layout = Layout()
    assertEquals(15, VerticalScroll.cursorCorrect(layout, WindowView(10), 12, 5))
    assertEquals(39, VerticalScroll.cursorCorrect(layout, WindowView(10), 44, 5))
    assertEquals(20, VerticalScroll.cursorCorrect(layout, WindowView(10), 20, 5))
  }

  @Test
  fun `test cursor correct has no top limit at the start of the file`() {
    assertEquals(2, VerticalScroll.cursorCorrect(Layout(), WindowView(0), 2, 5))
  }

  @Test
  fun `test cursor correct counts filler rows`() {
    // 3 rows of filler above line 11 count as context for line 11
    val layout = Layout(filler = mapOf(11 to 3))
    assertEquals(12, VerticalScroll.cursorCorrect(layout, WindowView(10), 10, 5))
  }
}
