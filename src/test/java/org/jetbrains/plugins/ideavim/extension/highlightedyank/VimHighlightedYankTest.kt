/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.highlightedyank

import com.intellij.openapi.editor.markup.RangeHighlighter
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.extension.highlightedyank.DEFAULT_HIGHLIGHT_DURATION
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.assertHappened
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class VimHighlightedYankTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("highlightedyank")
  }

  @Test
  fun `test highlighting whole line when whole line is yanked`() {
    doTest("yy", code, code, Mode.NORMAL())

    assertAllHighlightersCount(1)
    assertHighlighterRange(1, 40, getFirstHighlighter())
  }

  @Test
  fun `test highlighting single word when single word is yanked`() {
    doTest("yiw", code, code, Mode.NORMAL())

    assertAllHighlightersCount(1)
    assertHighlighterRange(5, 8, getFirstHighlighter())
  }

  @Test
  fun `test removing previous highlight when new range is yanked`() {
    configureByJavaText(code)
    typeText(injector.parser.parseKeys("yyjyy"))

    assertAllHighlightersCount(1)
    assertHighlighterRange(40, 59, getFirstHighlighter())
  }

  @Test
  fun `test removing previous highlight when entering insert mode`() {
    doTest("yyi", code, code, Mode.INSERT)

    assertAllHighlightersCount(0)
  }

  @Test
  fun `test indicating error when incorrect highlight duration was provided by user`() {
    configureByJavaText(code)
    typeText(injector.parser.parseKeys(":let g:highlightedyank_highlight_duration = \"500.15\"<CR>"))
    typeText(injector.parser.parseKeys("yy"))

    kotlin.test.assertEquals(
      "highlightedyank: Invalid value of g:highlightedyank_highlight_duration -- For input string: \"500.15\"",
      VimPlugin.getMessage(),
    )
  }

  @Test
  fun `test not indicating error when correct highlight duration was provided by user`() {
    configureByJavaText(code)
    typeText(injector.parser.parseKeys(":let g:highlightedyank_highlight_duration = \"-1\"<CR>"))
    typeText(injector.parser.parseKeys("yy"))

    kotlin.test.assertEquals(VimPlugin.getMessage(), "")
  }

  @Test
  fun `test indicating error when incorrect highlight color was provided by user`() {
    configureByJavaText(code)

    listOf("rgba(1,2,3)", "rgba(1, 2, 3, 0.1)", "rgb(1,2,3)", "rgba(260, 2, 5, 6)").forEach { color ->
      typeText(injector.parser.parseKeys(":let g:highlightedyank_highlight_color = \"$color\"<CR>"))
      typeText(injector.parser.parseKeys("yy"))

      kotlin.test.assertTrue(
        VimPlugin.getMessage().contains("highlightedyank: Invalid value of g:highlightedyank_highlight_color"),
        color,
      )
    }
  }

  @Test
  fun `test indicating error when correct highlight color was provided by user`() {
    configureByJavaText(code)

    listOf("rgba(1,2,3,5)", "rgba1, 2, 3, 1", "rgba(1, 2, 3, 4").forEach { color ->
      typeText(injector.parser.parseKeys(":let g:highlightedyank_highlight_color = \"$color\"<CR>"))
      typeText(injector.parser.parseKeys("yy"))

      kotlin.test.assertEquals("", VimPlugin.getMessage())
    }
  }

  @Test
  fun `test highlighting with multiple cursors`() {
    doTest(
      "yiw",
      codeWithMultipleCurors,
      codeWithMultipleCurors,
      Mode.NORMAL(),
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
      codeWithMultipleCurors,
      codeWithMultipleCurors,
      Mode.INSERT,
    )

    assertAllHighlightersCount(0)
  }

  @Test
  fun `test highlighting for a correct default amount of time`() {
    doTest("yiw", code, code, Mode.NORMAL())

    assertHappened(DEFAULT_HIGHLIGHT_DURATION.toInt(), 200) {
      getAllHighlightersCount() == 0
    }
  }

  @Test
  fun `test highlighting for a correct user provided amount of time`() {
    configureByJavaText(code)
    typeText(injector.parser.parseKeys(":let g:highlightedyank_highlight_duration = \"1000\"<CR>"))
    typeText(injector.parser.parseKeys("yiw"))

    assertHappened(1000, 200) {
      getAllHighlightersCount() == 0
    }
  }

  private val code = """
fun ${c}sum(x: Int, y: Int, z: Int): Int {
  return x + y + z
}
"""

  private val codeWithMultipleCurors = """
fun sum(x: ${c}Int, y: ${c}Int, z: ${c}Int): Int {
  return x + y + z
}
"""

  private fun assertHighlighterRange(start: Int, end: Int, highlighter: RangeHighlighter) {
    kotlin.test.assertEquals(start, highlighter.startOffset)
    kotlin.test.assertEquals(end, highlighter.endOffset)
  }

  private fun assertAllHighlightersCount(count: Int) {
    kotlin.test.assertEquals(count, getAllHighlightersCount())
  }

  private fun getAllHighlightersCount() = fixture.editor.markupModel.allHighlighters.size

  private fun getFirstHighlighter(): RangeHighlighter {
    return fixture.editor.markupModel.allHighlighters.first()
  }
}
