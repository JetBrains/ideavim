/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.extension.highlightedyank

import com.intellij.openapi.editor.markup.RangeHighlighter
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.extension.highlightedyank.DEFAULT_HIGHLIGHT_DURATION
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.assertHappened

class VimHighlightedYankTest : VimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("highlightedyank")
  }

  fun `test highlighting whole line when whole line is yanked`() {
    doTest("yy", code, code, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)

    assertAllHighlightersCount(1)
    assertHighlighterRange(1, 40, getFirstHighlighter())
  }

  fun `test highlighting single word when single word is yanked`() {
    doTest("yiw", code, code, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)

    assertAllHighlightersCount(1)
    assertHighlighterRange(5, 8, getFirstHighlighter())
  }

  fun `test removing previous highlight when new range is yanked`() {
    configureByJavaText(code)
    typeText(injector.parser.parseKeys("yyjyy"))

    assertAllHighlightersCount(1)
    assertHighlighterRange(40, 59, getFirstHighlighter())
  }

  fun `test removing previous highlight when entering insert mode`() {
    doTest("yyi", code, code, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)

    assertAllHighlightersCount(0)
  }

  fun `test indicating error when incorrect highlight duration was provided by user`() {
    configureByJavaText(code)
    typeText(injector.parser.parseKeys(":let g:highlightedyank_highlight_duration = \"500.15\"<CR>"))
    typeText(injector.parser.parseKeys("yy"))

    assertEquals(
      "highlightedyank: Invalid value of g:highlightedyank_highlight_duration -- For input string: \"500.15\"",
      VimPlugin.getMessage()
    )
  }

  fun `test not indicating error when correct highlight duration was provided by user`() {

    configureByJavaText(code)
    typeText(injector.parser.parseKeys(":let g:highlightedyank_highlight_duration = \"-1\"<CR>"))
    typeText(injector.parser.parseKeys("yy"))

    assertEquals(VimPlugin.getMessage(), "")
  }

  fun `test indicating error when incorrect highlight color was provided by user`() {
    configureByJavaText(code)

    listOf("rgba(1,2,3)", "rgba(1, 2, 3, 0.1)", "rgb(1,2,3)", "rgba(260, 2, 5, 6)").forEach { color ->
      typeText(injector.parser.parseKeys(":let g:highlightedyank_highlight_color = \"$color\"<CR>"))
      typeText(injector.parser.parseKeys("yy"))

      assertTrue(
        color,
        VimPlugin.getMessage().contains("highlightedyank: Invalid value of g:highlightedyank_highlight_color")
      )
    }
  }

  fun `test indicating error when correct highlight color was provided by user`() {
    configureByJavaText(code)

    listOf("rgba(1,2,3,5)", "rgba1, 2, 3, 1", "rgba(1, 2, 3, 4").forEach { color ->
      typeText(injector.parser.parseKeys(":let g:highlightedyank_highlight_color = \"$color\"<CR>"))
      typeText(injector.parser.parseKeys("yy"))

      assertEquals("", VimPlugin.getMessage())
    }
  }

  fun `test highlighting with multiple cursors`() {
    doTest("yiw", codeWithMultipleCurors, codeWithMultipleCurors, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)

    val highlighters = myFixture.editor.markupModel.allHighlighters
    assertAllHighlightersCount(3)
    assertHighlighterRange(12, 15, highlighters[1])
    assertHighlighterRange(20, 23, highlighters[0])
    assertHighlighterRange(28, 31, highlighters[2])
  }

  fun `test clearing all highlighters with multiple cursors`() {
    doTest("yiwi", codeWithMultipleCurors, codeWithMultipleCurors, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)

    assertAllHighlightersCount(0)
  }

  fun `test highlighting for a correct default amount of time`() {
    doTest("yiw", code, code, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)

    assertHappened(DEFAULT_HIGHLIGHT_DURATION.toInt(), 200) {
      getAllHighlightersCount() == 0
    }
  }

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
    assertEquals(start, highlighter.startOffset)
    assertEquals(end, highlighter.endOffset)
  }

  private fun assertAllHighlightersCount(count: Int) {
    assertEquals(count, getAllHighlightersCount())
  }

  private fun getAllHighlightersCount() = myFixture.editor.markupModel.allHighlighters.size

  private fun getFirstHighlighter(): RangeHighlighter {
    return myFixture.editor.markupModel.allHighlighters.first()
  }
}
