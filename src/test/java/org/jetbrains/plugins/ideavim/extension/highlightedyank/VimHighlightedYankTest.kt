/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.highlightedyank

import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.TextRange
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.extension.highlightedyank.DEFAULT_HIGHLIGHT_DURATION
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.assertHappened
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.awt.Color
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VimHighlightedYankTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("highlightedyank")
  }

  @Test
  fun `test highlighting whole line when whole line is yanked`() {
    doTest("yy", code, code)

    assertAllHighlightersCount(1)
    assertHighlighterRange(1, 40, getFirstHighlighter())
  }

  @Test
  fun `test highlighting multiple lines`() {
    doTest("2yy", code, code)

    assertAllHighlightersCount(1)
    assertHighlighterRange(1, 59, getFirstHighlighter())
  }

  @Test
  fun `test highlighting single word when single word is yanked`() {
    doTest("yiw", code, code)

    assertAllHighlightersCount(1)
    assertHighlighterRange(5, 8, getFirstHighlighter())
  }

  @Test
  fun `test removing previous highlight when yanking again`() {
    // Move the caret back to original position :)
    doTest(listOf("yy", "j", "yy", "k"), code, code)

    assertAllHighlightersCount(1)
    assertHighlighterRange(40, 59, getFirstHighlighter())
  }

  @Test
  fun `test removing previous highlight when entering insert mode`() {
    doTest("yyi", code, code, Mode.INSERT)

    assertAllHighlightersCount(0)
  }

  @Test
  fun `test highlighting with multiple cursors`() {
    doTest(
      "yiw",
      codeWithMultipleCursors,
      codeWithMultipleCursors,
    )

    val highlighters = fixture.editor.markupModel.allHighlighters
    assertAllHighlightersCount(3)
    assertHighlighterRange(12, 15, highlighters[1])
    assertHighlighterRange(20, 23, highlighters[0])
    assertHighlighterRange(28, 31, highlighters[2])
  }

  @Test
  fun `test clearing all highlighters with multiple cursors`() {
    doTest(
      "yiwi",
      codeWithMultipleCursors,
      codeWithMultipleCursors,
      Mode.INSERT,
    )

    assertAllHighlightersCount(0)
  }

  @Test
  fun `test highlighting for a correct default amount of time`() {
    doTest("yiw", code, code)

    assertHappened(DEFAULT_HIGHLIGHT_DURATION, 200) {
      getAllHighlightersCount() == 0
    }
  }

  @Test
  fun `test highlighting for a correct user provided amount of time`() {
    configureByText(code)
    enterCommand("let g:highlightedyank_highlight_duration = 1000")
    typeText("yiw")

    assertHappened(1000, 200) {
      getAllHighlightersCount() == 0
    }
  }

  @Test
  fun `test highlighting for a correct user provided amount of time when value passed as string`() {
    configureByText(code)
    enterCommand("let g:highlightedyank_highlight_duration = \"1000\"")
    typeText("yiw")

    assertHappened(1000, 200) {
      getAllHighlightersCount() == 0
    }
  }

  @Test
  fun `test indicating error when incorrect highlight duration was provided by user`() {
    configureByText(code)
    enterCommand("let g:highlightedyank_highlight_duration = \"500.15\"")
    typeText("yy")

    assertEquals(
      "highlightedyank: Invalid value of g:highlightedyank_highlight_duration -- For input string: \"500.15\"",
      VimPlugin.getMessage(),
    )
  }

  @Test
  fun `test not indicating error when correct highlight duration was provided by user`() {
    configureByText(code)
    enterCommand("let g:highlightedyank_highlight_duration = \"-1\"")
    typeText("yy")

    assertEquals(null, VimPlugin.getMessage())
  }

  @Test
  fun `test not indicating error when correct highlight duration was provided by user as string value`() {
    configureByText(code)
    enterCommand("let g:highlightedyank_highlight_duration = \"-1\"")
    typeText("yy")

    assertEquals(null, VimPlugin.getMessage())
  }

  @Test
  fun `test custom colour used when provided by user`() {
    configureByText(code)
    enterCommand("let g:highlightedyank_highlight_color=\"rgba(100,10,20,30)\"")
    typeText("yy")

    val highlighter = getFirstHighlighter()
    assertEquals(Color(100, 10, 20, 30), highlighter.getTextAttributes(null)?.backgroundColor)
  }

  @Test
  fun `test indicating error when incorrect highlight color was provided by user`() {
    configureByText(code)

    listOf("rgba(1,2,3)", "rgba(1, 2, 3, 0.1)", "rgb(1,2,3)", "rgba(260, 2, 5, 6)").forEach { color ->
      enterCommand("let g:highlightedyank_highlight_color = \"$color\"")
      typeText("yy")

      assertTrue(
        VimPlugin.getMessage().contains("highlightedyank: Invalid value of g:highlightedyank_highlight_color"),
        color,
      )
    }
  }

  private val code = """
fun ${c}sum(x: Int, y: Int, z: Int): Int {
  return x + y + z
}
"""

  private val codeWithMultipleCursors = """
fun sum(x: ${c}Int, y: ${c}Int, z: ${c}Int): Int {
  return x + y + z
}
"""

  private fun assertHighlighterRange(start: Int, end: Int, highlighter: RangeHighlighter) {
    assertEquals(TextRange(start, end), highlighter.textRange)
  }

  private fun assertAllHighlightersCount(count: Int) {
    assertEquals(count, getAllHighlightersCount())
  }

  private fun getAllHighlightersCount() = fixture.editor.markupModel.allHighlighters.size
  private fun getFirstHighlighter(): RangeHighlighter = fixture.editor.markupModel.allHighlighters.first()
}
