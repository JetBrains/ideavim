/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("ClassName")

package org.jetbrains.plugins.ideavim.group.motion

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.CustomFoldRegionRenderer
import com.intellij.openapi.editor.markup.TextAttributes
import com.maddyhome.idea.vim.api.getOffset
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// These tests pin down how scrolling handles the things that make a line take more or less than one row: soft wraps,
// collapsed folds, custom fold regions (e.g. rendered doc comments) and block inlays. The screen is 35 rows.
abstract class ScrollGeometryTestCase : VimTestCase() {
  protected fun configureByUnindentedLines(lineCount: Int) = configureByLines(lineCount, "I found it in a legendary land")

  protected fun addCollapsedFold(startLine: Int, endLine: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor
      editor.foldingModel.runBatchFoldingOperation {
        val region = editor.foldingModel.addFoldRegion(
          editor.document.getLineStartOffset(startLine),
          editor.document.getLineEndOffset(endLine),
          "...",
        )
        region!!.isExpanded = false
      }
    }
  }

  // A single visual line that is several rows high, which is how rendered doc comments are shown
  protected fun addCustomFoldRegion(startLine: Int, endLine: Int, heightInRows: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor
      editor.foldingModel.runBatchFoldingOperation {
        editor.foldingModel.addCustomLinesFolding(
          startLine,
          endLine,
          object : CustomFoldRegionRenderer {
            override fun calcWidthInPixels(region: CustomFoldRegion) = 100
            override fun calcHeightInPixels(region: CustomFoldRegion) = heightInRows * editor.lineHeight
            override fun paint(
              region: CustomFoldRegion,
              g: Graphics2D,
              targetRegion: Rectangle2D,
              textAttributes: TextAttributes,
            ) {
            }
          },
        )
      }
    }
  }

  // Like a mouse click, which doesn't scroll
  protected fun moveCaretWithoutScrolling(line: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.caretModel.moveToOffset(fixture.editor.document.getLineStartOffset(line))
    }
  }

  protected fun assertCaretVisualLine(expected: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      assertEquals(expected, fixture.editor.caretModel.visualPosition.line, "Caret visual lines don't match")
    }
  }

  protected fun assertTopVisualLine(expected: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      assertEquals(expected, EditorHelper.getVisualLineAtTopOfScreen(fixture.editor), "Top visual lines don't match")
    }
  }

  /** The row of the screen the caret line starts on, counting from 0 */
  protected fun assertCaretScreenRow(expected: Int) {
    assertEquals(expected, caretScreenRow(), "Caret screen row doesn't match")
  }

  protected fun assertCaretOnScreen() {
    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor
      val caretLine = editor.caretModel.visualPosition.line
      val topLine = EditorHelper.getVisualLineAtTopOfScreen(editor)
      val bottomLine = EditorHelper.getVisualLineAtBottomOfScreen(editor)
      assertTrue(caretLine in topLine..bottomLine, "Caret line $caretLine is outside of screen $topLine..$bottomLine")
    }
  }

  protected fun scrollOffset(): Int {
    var offset = 0
    ApplicationManager.getApplication().invokeAndWait { offset = fixture.editor.scrollingModel.verticalScrollOffset }
    return offset
  }

  protected fun caretScreenRow(): Int {
    var row = 0
    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor
      val caretY = editor.visualLineToY(editor.caretModel.visualPosition.line)
      row = (caretY - EditorHelper.getVisibleArea(editor).y) / editor.lineHeight
    }
    return row
  }
}

// Every line is 200 characters, soft wrapped into 3 rows, so logical line n starts on visual line 3n
@TestWithoutNeovim(SkipNeovimReason.SCROLL)
class MotionGroup_scrolloff_softWrap_Test : ScrollGeometryTestCase() {
  private fun configureByWrappedLines() = configureByLines(100, "x".repeat(200))

  @Test
  fun `test move down scrolls one row at a time with scrolloff`() {
    configureByWrappedLines()
    enterCommand("set scrolloff=5")

    typeText("j".repeat(29))
    assertCaretVisualLine(29)
    assertTopVisualLine(0)

    typeText("j")
    assertCaretVisualLine(30)
    assertTopVisualLine(1)

    typeText("j")
    assertTopVisualLine(2)
  }

  @Test
  fun `test move up scrolls one row at a time with scrolloff`() {
    configureByWrappedLines()
    enterCommand("set scrolloff=5")
    typeText("15G")
    assertTopVisualLine(13)
    assertCaretScreenRow(29)

    typeText("k".repeat(24))
    assertTopVisualLine(13)
    assertCaretScreenRow(5)

    typeText("k")
    assertTopVisualLine(12)
    assertCaretScreenRow(5)
  }

  @Test
  fun `test move up scrolls one row at a time with scrolloff in the second half of the file`() {
    configureByWrappedLines()
    enterCommand("set scrolloff=5")
    typeText("40G")
    assertTopVisualLine(100)
    assertCaretScreenRow(17)

    typeText("k".repeat(12))
    assertTopVisualLine(100)
    assertCaretScreenRow(5)

    typeText("k")
    assertTopVisualLine(99)
    assertCaretScreenRow(5)
  }

  @Test
  fun `test scrolloff=999 keeps the caret row in the middle`() {
    configureByWrappedLines()
    enterCommand("set scrolloff=999")
    typeText("50G")
    assertTopVisualLine(130)
    assertCaretScreenRow(17)

    repeat(3) {
      typeText("j")
      assertCaretScreenRow(17)
    }
    assertTopVisualLine(133)
  }

  @Test
  fun `test zz puts the first row of the line in the middle`() {
    configureByWrappedLines()
    typeText("50Gzz")
    assertTopVisualLine(130)
    assertCaretScreenRow(17)
  }

  @Test
  fun `test zt near the start of the file puts the line at the top`() {
    configureByWrappedLines()
    typeText("10Gzt")
    assertTopVisualLine(27)
    assertCaretScreenRow(0)
  }

  @Test
  fun `test zt puts the line at the top`() {
    configureByWrappedLines()
    typeText("50Gzt")
    assertTopVisualLine(147)
    assertCaretOnScreen()
  }

  @VimBehaviorDiffers(description = "Vim puts the last row of the line at the bottom, so the whole line is visible")
  @Test
  fun `test zb puts the first row of the line at the bottom`() {
    configureByWrappedLines()
    typeText("50Gzb")
    assertTopVisualLine(113)
    assertCaretScreenRow(34)
  }

  @VimBehaviorDiffers(description = "Without 'smoothscroll', Vim scrolls a whole line, i.e. 3 rows", shouldBeFixed = false)
  @Test
  fun `test CTRL-E scrolls one row`() {
    configureByWrappedLines()
    typeText("<C-E>")
    assertTopVisualLine(1)
    assertCaretVisualLine(1)
  }

  @Test
  fun `test CTRL-E scrolls down in the second half of the file`() {
    configureByWrappedLines()
    typeText("50Gzz")
    assertTopVisualLine(130)

    typeText("<C-E>")
    assertTopVisualLine(131)
  }

  @Test
  fun `test CTRL-F keeps scrolling down`() {
    configureByWrappedLines()
    repeat(5) {
      val before = scrollOffset()
      typeText("<C-F>")
      assertTrue(scrollOffset() > before, "CTRL-F didn't scroll down")
      assertCaretOnScreen()
    }
  }

  @Test
  fun `test CTRL-F scrolls a page`() {
    configureByWrappedLines()
    typeText("<C-F>")
    assertTopVisualLine(33)
    assertCaretOnScreen()
  }

  // Half a screen down is the third row of line 5, the caret moves to the start of that line and the screen follows
  @Test
  fun `test CTRL-D scrolls half a screen of rows`() {
    configureByWrappedLines()
    typeText("<C-D>")
    assertPosition(5, 0)
    assertTopVisualLine(15)
  }

  @Test
  fun `test H M L move to wrapped lines on the screen`() {
    configureByWrappedLines()
    typeText("L")
    assertPosition(11, 0)
    typeText("M")
    assertPosition(5, 0)
    typeText("H")
    assertPosition(0, 0)
  }
}

// Lines 20 to 60 are folded into a single visual line, visual line 20. Logical line n > 60 is visual line n - 40
@TestWithoutNeovim(SkipNeovimReason.FOLDING)
class MotionGroup_scrolloff_folds_Test : ScrollGeometryTestCase() {
  private fun configureWithFold() {
    configureByUnindentedLines(200)
    addCollapsedFold(20, 60)
  }

  @Test
  fun `test move down over a fold scrolls one line at a time with scrolloff`() {
    configureWithFold()
    enterCommand("set scrolloff=5")

    typeText("j".repeat(25))
    assertPosition(65, 0)
    assertTopLogicalLine(0)

    typeText("j".repeat(4))
    assertPosition(69, 0)
    assertTopLogicalLine(0)

    typeText("j")
    assertPosition(70, 0)
    assertTopLogicalLine(1)
  }

  @Test
  fun `test move up scrolls the fold to the top with scrolloff`() {
    configureWithFold()
    enterCommand("set scrolloff=5")
    setPositionAndScroll(61, 66)

    typeText("k")
    assertPosition(65, 0)
    assertTopLogicalLine(20)

    typeText("k")
    assertPosition(64, 0)
    assertTopLogicalLine(19)
  }

  @Test
  fun `test CTRL-E and CTRL-Y scroll over a fold as one line`() {
    configureWithFold()
    setPositionAndScroll(19, 19)

    typeText("<C-E>")
    assertTopLogicalLine(20)
    typeText("<C-E>")
    assertTopLogicalLine(61)
    typeText("<C-Y>")
    assertTopLogicalLine(20)
    typeText("<C-Y>")
    assertTopLogicalLine(19)
  }

  @Test
  fun `test zt puts a fold at the top`() {
    configureWithFold()
    typeText("21Gzt")
    assertTopLogicalLine(20)
    assertCaretScreenRow(0)
  }

  @Test
  fun `test H M L count a fold as one line`() {
    configureWithFold()
    typeText("L")
    assertPosition(74, 0)
    typeText("M")
    assertPosition(17, 0)
    typeText("H")
    assertPosition(0, 0)
  }
}

// Lines 30 to 34 are a custom fold region, a single visual line that is 10 rows high. Logical line n > 34 is visual
// line n - 4
@TestWithoutNeovim(SkipNeovimReason.FOLDING)
class MotionGroup_scrolloff_customFoldRegion_Test : ScrollGeometryTestCase() {
  private fun configureWithCustomFoldRegion() {
    configureByUnindentedLines(200)
    addCustomFoldRegion(30, 34, 10)
  }

  @Test
  fun `test move down keeps scrolloff rows below a tall custom fold region`() {
    configureWithCustomFoldRegion()
    enterCommand("set scrolloff=5")

    // Line 29 is on row 29, and the top 5 rows of the region below it are on the screen
    typeText("j".repeat(29))
    assertTopLogicalLine(0)

    // The region is on rows 20 to 29, so there are 5 rows below it
    typeText("j")
    assertPosition(30, 0)
    assertTopLogicalLine(10)
    assertCaretScreenRow(20)

    typeText("j")
    assertPosition(35, 0)
    assertTopLogicalLine(11)
    assertCaretScreenRow(29)
  }

  @Test
  fun `test move up shows the whole custom fold region at the top`() {
    configureWithCustomFoldRegion()
    enterCommand("set scrolloff=5")
    setPositionAndScroll(35, 40)

    typeText("k")
    assertPosition(39, 0)
    assertTopLogicalLine(30)
    assertCaretScreenRow(14)
  }

  @Test
  fun `test zt puts a custom fold region at the top`() {
    configureWithCustomFoldRegion()
    typeText("31Gzt")
    assertTopLogicalLine(30)
    assertCaretScreenRow(0)
  }

  @Test
  fun `test zb puts the bottom of a custom fold region at the bottom`() {
    configureWithCustomFoldRegion()
    typeText("31Gzb")
    assertTopLogicalLine(5)
  }

  @Test
  fun `test zz puts a custom fold region in the middle`() {
    configureWithCustomFoldRegion()
    typeText("31Gzz")
    assertTopLogicalLine(18)
    assertCaretScreenRow(12)
  }

  @Test
  fun `test CTRL-E scrolls over a custom fold region as one line`() {
    configureWithCustomFoldRegion()
    typeText("25Gzt")
    assertTopLogicalLine(24)

    typeText("<C-E>".repeat(6))
    assertTopLogicalLine(30)

    typeText("<C-E>")
    assertTopLogicalLine(35)
  }
}

// A 50 row block inlay below line 30, taller than the 35 row screen
@TestWithoutNeovim(SkipNeovimReason.INLAYS)
class MotionGroup_scrolloff_tallInlay_Test : ScrollGeometryTestCase() {
  private fun configureWithTallInlay() {
    configureByUnindentedLines(200)
    addBlockInlay(fixture.editor.vim.getOffset(30, 0), false, 50)
  }

  @Test
  fun `test move down past an inlay taller than the screen keeps the caret on the screen`() {
    configureWithTallInlay()
    enterCommand("set scrolloff=3")
    typeText("20G")

    repeat(15) {
      typeText("j")
      assertCaretOnScreen()
    }
  }

  @Test
  fun `test move up past an inlay taller than the screen keeps the caret on the screen`() {
    configureWithTallInlay()
    enterCommand("set scrolloff=3")
    setPositionAndScroll(31, 34)

    repeat(15) {
      typeText("k")
      assertCaretOnScreen()
    }
  }

  // Moving up from below the inlay to the line above it leaves no room for the rows below that line, so it is centred
  @Test
  fun `test move up past an inlay taller than the screen after moving down past it`() {
    configureWithTallInlay()
    enterCommand("set scrolloff=3")
    typeText("20G")
    typeText("j".repeat(14))
    assertPosition(33, 0)

    repeat(14) {
      typeText("k")
      assertCaretOnScreen()
    }
    assertPosition(19, 0)
    assertTopLogicalLine(12)
  }

  @Test
  fun `test z commands with an inlay taller than the screen below the line`() {
    configureWithTallInlay()

    typeText("31Gzt")
    assertTopLogicalLine(30)
    assertCaretScreenRow(0)

    // The inlay below the line is shown at the bottom, as much of it as fits with the line on the screen
    typeText("zb")
    assertTopLogicalLine(30)
    assertCaretScreenRow(0)

    typeText("zz")
    assertTopLogicalLine(13)
    assertCaretScreenRow(17)
  }

  @Test
  fun `test CTRL-E scrolls through an inlay taller than the screen one row at a time`() {
    configureWithTallInlay()
    typeText("30Gzt")
    assertTopLogicalLine(29)

    typeText("<C-E>")
    assertTopLogicalLine(30)

    // The next line is at the bottom, below as much of the inlay as fits
    typeText("<C-E>")
    assertTopLogicalLine(31)
    assertPosition(31, 0)
    assertCaretScreenRow(34)

    typeText("<C-E>")
    assertCaretScreenRow(33)
  }

  // Line 30 is the last line on the screen, its inlay fills the rest
  @Test
  fun `test CTRL-F and CTRL-B with an inlay taller than the screen`() {
    configureWithTallInlay()

    typeText("<C-F>")
    assertTopLogicalLine(29)
    assertCaretOnScreen()

    typeText("<C-B>")
    assertTopLogicalLine(0)
    assertCaretOnScreen()
  }

  @Test
  fun `test scrolloff=999 keeps the caret in the middle past an inlay taller than the screen`() {
    configureWithTallInlay()
    enterCommand("set scrolloff=999")
    typeText("25G")

    repeat(8) {
      typeText("j")
      assertCaretScreenRow(17)
    }
    assertPosition(32, 0)
  }
}

// Lines 10 to 12 have a 2 row inlay above them, lines 36 to 40 a 2 row inlay below them
@TestWithoutNeovim(SkipNeovimReason.INLAYS)
class MotionGroup_scrolloff_inlaysInMargin_Test : ScrollGeometryTestCase() {
  private fun configureWithInlays() {
    configureByUnindentedLines(200)
    for (line in 36..40) addBlockInlay(fixture.editor.vim.getOffset(line, 0), false, 2)
    for (line in 10..12) addBlockInlay(fixture.editor.vim.getOffset(line, 0), true, 2)
  }

  @Test
  fun `test move down keeps scrolloff rows below the caret counting inlays`() {
    configureWithInlays()
    enterCommand("set scrolloff=3")

    typeText("j".repeat(32))
    assertPosition(32, 0)
    assertCaretScreenRow(31)

    // From line 36, the rows below the caret include the inlays below the lines
    repeat(10) {
      typeText("j")
      assertCaretScreenRow(31)
    }
    assertPosition(42, 0)
  }

  @Test
  fun `test move up keeps scrolloff rows above the caret counting inlays`() {
    configureWithInlays()
    enterCommand("set scrolloff=3")
    typeText("j".repeat(45))
    typeText("k".repeat(29))
    assertPosition(16, 0)
    assertCaretScreenRow(3)

    // From line 12, the rows above the caret include the inlays above the lines. The whole inlay above the top line is
    // shown, so there can be more rows than needed
    repeat(13) {
      typeText("k")
      assertTrue(caretScreenRow() >= 3, "Fewer than 3 rows above the caret")
    }
    assertPosition(3, 0)
    assertTopLogicalLine(0)
  }

  @Test
  fun `test CTRL-D shows the inlay above the target line at the top of the screen`() {
    configureByUnindentedLines(200)
    addBlockInlay(fixture.editor.vim.getOffset(37, 0), true, 3)
    setPositionAndScroll(20, 20)

    typeText("17<C-D>")
    assertPosition(37, 0)
    assertTopLogicalLine(37)
    assertCaretScreenRow(3)
  }

  @Test
  fun `test CTRL-U shows the inlay below the target line at the bottom of the screen`() {
    configureByUnindentedLines(200)
    addBlockInlay(fixture.editor.vim.getOffset(37, 0), false, 3)
    setPositionAndScroll(20, 51)

    typeText("<C-U>")
    assertPosition(37, 0)
    assertCaretScreenRow(31)
  }
}

// Each of the 50 lines is followed by a 10 row inlay, like a notebook with short cells and their outputs. With line 46
// at the top, line 49 is on row 33 and its output is below the screen
@TestWithoutNeovim(SkipNeovimReason.INLAYS)
class MotionGroup_scrolloff_notebookEndOfFile_Test : ScrollGeometryTestCase() {
  @Test
  fun `test append on the last line does not scroll when the last line is on the screen`() {
    configureByUnindentedLines(50)
    for (line in 0..49) addBlockInlay(fixture.editor.vim.getOffset(line, 0), false, 10)
    setPositionAndScroll(46, 49)
    assertCaretScreenRow(33)

    // The caret is inside the scrolloff margin, but there is nothing to scroll to
    enterCommand("set scrolloff=5")
    assertTopLogicalLine(46)

    typeText("A")
    assertTopLogicalLine(46)
    assertCaretScreenRow(33)
  }
}
