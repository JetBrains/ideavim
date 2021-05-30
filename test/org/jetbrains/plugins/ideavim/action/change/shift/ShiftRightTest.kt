/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action.change.shift

import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class ShiftRightTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun `test shift till new line`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile(StringHelper.parseKeys(">W"), file)
    assertState("""
            A Discovery

                  ${c}I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
      """.trimIndent())
  }

  // VIM-407
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftShiftsOneCharacterSingleLine() {
    configureByText("<caret>w\n")
    typeText(StringHelper.parseKeys(">>"))
    assertState("    w\n")
  }

  // VIM-407
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftShiftsOneCharacterMultiLine() {
    configureByText("Hello\n<caret>w\nWorld")
    typeText(StringHelper.parseKeys(">>"))
    assertState("Hello\n    w\nWorld")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftShiftsMultipleCharactersOneLine() {
    configureByText("<caret>Hello, world!\n")
    typeText(StringHelper.parseKeys(">>"))
    assertState("    Hello, world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftShiftsMultipleCharactersMultipleLines() {
    configureByText("<caret>Hello,\nworld!\n")
    typeText(StringHelper.parseKeys("j>>"))
    assertState("Hello,\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsSingleLineSelection() {
    configureByText("<caret>Hello,\nworld!\n")
    typeText(StringHelper.parseKeys("jv$>>"))
    assertState("Hello,\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelection() {
    configureByText("<caret>Hello,\nworld!\n")
    typeText(StringHelper.parseKeys("vj$>>"))
    assertState("    Hello,\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelectionSkipsNewline() {
    configureByText("<caret>Hello,\nworld!\n\n")
    typeText(StringHelper.parseKeys("vG$>>"))
    assertState("    Hello,\n    world!\n\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelectionSkipsNewlineWhenCursorNotInFirstColumn() {
    configureByText("<caret>Hello,\n\nworld!\n")
    typeText(StringHelper.parseKeys("lVG>"))
    assertState("    Hello,\n\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelectionAddsTrailingWhitespaceIfTherePreviouslyWas() {
    configureByText("<caret>Hello,\n    \nworld!\n")
    typeText(StringHelper.parseKeys("lVG>"))
    assertState("    Hello,\n        \n    world!\n")
  }

  // VIM-705 repeating a multiline indent would only affect last line
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelectionRepeat() {
    configureByText("<caret>a\nb\n")
    typeText(StringHelper.parseKeys("Vj>."))
    assertState("        a\n        b\n")
  }

  fun testShiftsDontCrashKeyHandler() {
    configureByText("\n")
    typeText(StringHelper.parseKeys("<I<>", "<I<>"))
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsVisualBlockMode() {
    configureByText("foo<caret>foo\nfoobar\nfoobaz\n")
    typeText(StringHelper.parseKeys("<C-V>jjl>"))
    assertState("foo    foo\nfoo    bar\nfoo    baz\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun `test shift right positions caret at first non-blank char`() {
    val file = """
      |A Discovery
      |
      |       I found it in a legendary l${c}and
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
    """.trimMargin()
    typeTextInFile(StringHelper.parseKeys(">>"), file)
    assertState("""
      |A Discovery

      |           ${c}I found it in a legendary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin())
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun `test shift right does not move caret with nostartofline`() {
    OptionsManager.startofline.reset()
    val file = """
      |A Discovery
      |
      |       I found it in a ${c}legendary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
    """.trimMargin()
    typeTextInFile(StringHelper.parseKeys(">>"), file)
    assertState("""
      |A Discovery

      |           I found it i${c}n a legendary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin())
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun `test shift ctrl-t`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile(StringHelper.parseKeys("i<C-T>"), file)
    assertState("""
            A Discovery

                  I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
      """.trimIndent())
  }
}
