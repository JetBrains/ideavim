/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.helper

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.helper.EditorHelper
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterisation tests for the platform APIs that report which part of the document is on screen.
 *
 * These are written to answer a design question rather than to guard IdeaVim behaviour: if search highlighting is to
 * be scoped to the viewport, it needs a visible-range source that works in the headless test fixture. The fixture sets
 * an artificial viewport of [screenWidth] x [screenHeight] characters via `setEditorVisibleSize`, so the question is
 * whether `Editor.calculateVisibleRange()` honours it, or whether only IdeaVim's own
 * `EditorHelper.getVisualLineAtTopOfScreen`/`getVisualLineAtBottomOfScreen` can be trusted here.
 */
class VisibleRangeApiTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test calculateVisibleRange covers whole document when it fits on screen`() {
    // Four lines against a 35 line viewport - everything is visible
    configureByText(
      """I found it in a legendary land
         |all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )

    ApplicationManager.getApplication().invokeAndWait {
      val visibleRange = fixture.editor.calculateVisibleRange()
      assertEquals(0, visibleRange.startOffset, "Visible range should start at the top of the document")
      assertEquals(
        fixture.editor.document.textLength,
        visibleRange.endOffset,
        "Visible range should cover the whole document when it is shorter than the viewport",
      )
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test calculateVisibleRange is limited to the viewport for a long document`() {
    // 200 lines against a 35 line viewport - only a fraction is visible
    configureByLines(200, "lorem ipsum dolor sit amet,")

    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor
      val visibleRange = editor.calculateVisibleRange()

      assertEquals(0, visibleRange.startOffset, "Not scrolled, so the visible range starts at the top")
      assertTrue(
        visibleRange.endOffset < editor.document.textLength,
        "Visible range (${visibleRange.startOffset}, ${visibleRange.endOffset}) should not cover the whole " +
          "${editor.document.textLength} char document - the viewport is only $screenHeight lines",
      )

      // The platform's range should agree with the visible line range IdeaVim already computes for scroll commands
      val bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor)
      val endLine = editor.offsetToLogicalPosition(visibleRange.endOffset).line
      assertTrue(
        endLine >= bottomVisualLine,
        "calculateVisibleRange ends on line $endLine, but IdeaVim thinks the bottom of the screen is " +
          "visual line $bottomVisualLine",
      )
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test calculateVisibleRange follows the scroll position`() {
    configureByLines(200, "lorem ipsum dolor sit amet,")

    // Scroll so that line 100 is at the top of the screen
    typeText("100Gzt")

    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor
      val visibleRange = editor.calculateVisibleRange()
      val startLine = editor.offsetToLogicalPosition(visibleRange.startOffset).line

      assertTrue(
        startLine > 0,
        "After scrolling to line 100 the visible range should no longer start at the top of the document, " +
          "but it starts on line $startLine",
      )
      assertEquals(
        EditorHelper.getVisualLineAtTopOfScreen(editor),
        startLine,
        "calculateVisibleRange should agree with IdeaVim's own top-of-screen calculation",
      )
    }
  }
}
