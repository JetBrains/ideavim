/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.highlightedyank

import com.intellij.openapi.editor.markup.RangeHighlighter
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.jetbrains.plugins.ideavim.assertHappened
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class VimHighlightedYankJavaTest : VimJavaTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("highlightedyank")
  }

  @Test
  fun `test removing previous highlight when new range is yanked`() {
    configureByJavaText(code)
    typeText(injector.parser.parseKeys("yyjyy"))

    assertAllHighlightersCount(1)
    assertHighlighterRange(40, 59, getFirstHighlighter())
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

  private fun assertAllHighlightersCount(count: Int) {
    kotlin.test.assertEquals(count, getAllHighlightersCount())
  }

  private fun getAllHighlightersCount() = fixture.editor.markupModel.allHighlighters.size

  private fun assertHighlighterRange(start: Int, end: Int, highlighter: RangeHighlighter) {
    kotlin.test.assertEquals(start, highlighter.startOffset)
    kotlin.test.assertEquals(end, highlighter.endOffset)
  }

  private fun getFirstHighlighter(): RangeHighlighter {
    return fixture.editor.markupModel.allHighlighters.first()
  }
}