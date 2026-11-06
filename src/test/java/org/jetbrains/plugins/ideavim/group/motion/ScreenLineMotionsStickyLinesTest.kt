/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.motion

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.CustomFoldRegionRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.test.assertTrue

/**
 * H, M and L move the caret to the top, middle and bottom of the text area, below the sticky lines panel, without
 * scrolling. Each position is checked with the panel at different heights, as the scopes at the top of the screen
 * start and end.
 *
 * With doc comments rendered, each comment is a single visual line a fraction of a row taller than the rows it is
 * counted as, so only the bounds are checked: the caret lands within a row of the context, and roughly in the middle.
 */
@TestWithoutNeovim(SkipNeovimReason.SCROLL)
class ScreenLineMotionsStickyLinesTest : StickyLinesTestCase() {
  @ParameterizedTest
  @CsvSource("0, false", "5, false", "12, false", "0, true", "5, true", "12, true")
  fun `test H M L with sticky lines`(scrollOffset: Int, renderedDocs: Boolean) {
    configureByText(classWithMethods())
    enterCommand("set scrolloff=$scrollOffset")

    withMainEditor(40) { editor ->
      addStickyLinesForBlocks(editor)
      if (renderedDocs) renderDocComments(editor)
      val exact = !renderedDocs
      val failures = mutableListOf<String>()
      var maxStickyRows = 0
      fun check(motion: String, assert: (Screen) -> String?) {
        val before = screen(editor)
        typeText(editor, motion)
        val after = screen(editor)
        maxStickyRows = maxOf(maxStickyRows, after.stickyRows)
        if (before.scrollY != after.scrollY) failures += "'$motion' scrolled: $before -> $after"
        assert(after)?.let { failures += "'$motion': $it: $after" }
      }

      typeText(editor, "gg")
      check("H") { atTop(it, scrollOffset, exact) }
      check("L") { atBottom(it, scrollOffset, exact) }
      // Stays clear of the end of the file, where 'scrolloff' doesn't apply at the bottom
      for (line in 40..560) {
        typeText(editor, "${line}zt")
        check("H") { atTop(it, scrollOffset, exact) }
        check("M") { inMiddle(it, exact) }
        check("L") { atBottom(it, scrollOffset, exact) }
      }

      assertTrue(maxStickyRows >= 2, "The sticky lines panel wasn't shown")
      assertTrue(failures.isEmpty(), "${failures.size} failures:\n" + failures.take(10).joinToString("\n"))
    }
  }

  // The first line with 'scrolloff' rows above it, or the first line of the file if it is shown
  private fun atTop(screen: Screen, scrollOffset: Int, exact: Boolean): String? {
    val wanted = if (screen.isTopOfFileVisible) 0 else scrollOffset
    return when {
      screen.rowsAbove < wanted - if (exact) 0 else 1 -> "Fewer than $wanted rows above the caret"
      exact && screen.rowsAboveLineAbove >= wanted -> "The line above the caret is far enough from the top"
      else -> null
    }
  }

  // The last line with 'scrolloff' rows below it
  private fun atBottom(screen: Screen, scrollOffset: Int, exact: Boolean): String? = when {
    screen.rowsBelow < scrollOffset - if (exact) 0 else 1 -> "Fewer than $scrollOffset rows below the caret"
    exact && screen.rowsBelowLineBelow >= scrollOffset -> "The line below the caret is far enough from the bottom"
    else -> null
  }

  // The caret line covers the middle row of the text, give or take a row for the filler around it
  private fun inMiddle(screen: Screen, exact: Boolean): String? {
    val middle = screen.textRows / 2
    val slack = if (exact) 1 else 3
    return if (screen.rowsAbove <= middle + slack && screen.rowsAbove + screen.caretRows - 1 >= middle - slack) {
      null
    } else {
      "Caret is not on the middle row $middle"
    }
  }

  /**
   * Where the caret line is in the text area below the sticky lines panel, in whole rows, rounded down. The rows
   * above and below the visual lines next to the caret are negative if there are no such lines.
   */
  private data class Screen(
    val caretLine: Int,
    val scrollY: Int,
    val stickyRows: Int,
    val isTopOfFileVisible: Boolean,
    val rowsAbove: Int,
    val rowsBelow: Int,
    val rowsAboveLineAbove: Int,
    val rowsBelowLineBelow: Int,
    val caretRows: Int,
    val textRows: Int,
  )

  private fun screen(editor: Editor): Screen {
    var screen: Screen? = null
    ApplicationManager.getApplication().invokeAndWait {
      val area = editor.scrollingModel.visibleArea
      val lineHeight = editor.lineHeight.toDouble()
      val sticky = (editor as EditorImpl).stickyLinesPanelHeight
      val top = area.y + sticky
      val bottom = area.y + area.height
      val caretLine = editor.caretModel.visualPosition.line
      val lastLine = editor.offsetToVisualPosition(editor.document.textLength).line
      val caret = editor.visualLineToYRange(caretLine)
      fun rows(pixels: Int) = floor(pixels / lineHeight).toInt()
      screen = Screen(
        caretLine = editor.caretModel.logicalPosition.line,
        scrollY = area.y,
        stickyRows = (sticky / lineHeight).roundToInt(),
        isTopOfFileVisible = area.y == 0,
        rowsAbove = rows(caret[0] - top),
        rowsBelow = rows(bottom - caret[1]),
        rowsAboveLineAbove = if (caretLine > 0) rows(editor.visualLineToYRange(caretLine - 1)[0] - top) else -1,
        rowsBelowLineBelow = if (caretLine < lastLine) rows(bottom - editor.visualLineToYRange(caretLine + 1)[1]) else -1,
        caretRows = ((caret[1] - caret[0]) / lineHeight).roundToInt(),
        textRows = rows(minOf(bottom, editor.visualLineToYRange(lastLine)[1]) - top),
      )
    }
    return screen!!
  }

  // A class with methods, each with a doc comment and a nested block, so the sticky lines panel changes height while
  // scrolling
  private fun classWithMethods(): String = buildString {
    appendLine("public class MyScroll {")
    for (i in 0 until 40) {
      appendLine("  /**")
      appendLine("   * Returns the value of method $i.")
      appendLine("   * The comment has a few lines, so it takes a few rows when it is rendered.")
      appendLine("   */")
      appendLine("  static int method$i(int value) {")
      appendLine("    if (value > 0) {")
      repeat(6) { appendLine("      value = value + $it;") }
      appendLine("    }")
      appendLine("    return value + $i;")
      appendLine("  }")
      appendLine()
    }
    append("}")
  }

  // Each doc comment becomes a custom fold region of 2.6 rows, as a rendered doc comment is
  private fun renderDocComments(editor: Editor) {
    ApplicationManager.getApplication().invokeAndWait {
      val document = editor.document
      editor.foldingModel.runBatchFoldingOperation {
        for (line in 0 until document.lineCount) {
          val text = document.getText(TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)))
          if (text.trim() != "/**") continue
          editor.foldingModel.addCustomLinesFolding(line, line + 3, DocCommentRenderer(editor))
        }
      }
    }
  }

  private class DocCommentRenderer(private val editor: Editor) : CustomFoldRegionRenderer {
    override fun calcWidthInPixels(region: CustomFoldRegion) = 100
    override fun calcHeightInPixels(region: CustomFoldRegion) = (2.6 * editor.lineHeight).roundToInt()
    override fun paint(
      region: CustomFoldRegion,
      g: Graphics2D,
      targetRegion: Rectangle2D,
      textAttributes: TextAttributes,
    ) {
    }
  }
}
