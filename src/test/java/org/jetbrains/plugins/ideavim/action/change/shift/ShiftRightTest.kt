/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.shift

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
    typeTextInFile(">W", file)
    assertState(
      """
            A Discovery

                  ${c}I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
  }

  // VIM-407
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftShiftsOneCharacterSingleLine() {
    configureByText("<caret>w\n")
    typeText(">>")
    assertState("    w\n")
  }

  // VIM-407
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftShiftsOneCharacterMultiLine() {
    configureByText("Hello\n<caret>w\nWorld")
    typeText(">>")
    assertState("Hello\n    w\nWorld")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftShiftsMultipleCharactersOneLine() {
    configureByText("<caret>Hello, world!\n")
    typeText(">>")
    assertState("    Hello, world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftShiftsMultipleCharactersMultipleLines() {
    configureByText("<caret>Hello,\nworld!\n")
    typeText("j>>")
    assertState("Hello,\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsSingleLineSelection() {
    configureByText("<caret>Hello,\nworld!\n")
    typeText("jv$>>")
    assertState("Hello,\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelection() {
    configureByText("<caret>Hello,\nworld!\n")
    typeText("vj$>>")
    assertState("    Hello,\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelectionSkipsNewline() {
    configureByText("<caret>Hello,\nworld!\n\n")
    typeText("vG$>>")
    assertState("    Hello,\n    world!\n\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelectionSkipsNewlineWhenCursorNotInFirstColumn() {
    configureByText("<caret>Hello,\n\nworld!\n")
    typeText("lVG>")
    assertState("    Hello,\n\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelectionAddsTrailingWhitespaceIfTherePreviouslyWas() {
    configureByText("<caret>Hello,\n    \nworld!\n")
    typeText("lVG>")
    assertState("    Hello,\n        \n    world!\n")
  }

  // VIM-705 repeating a multiline indent would only affect last line
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsMultiLineSelectionRepeat() {
    configureByText("<caret>a\nb\n")
    typeText("Vj>.")
    assertState("        a\n        b\n")
  }

  fun testShiftsDontCrashKeyHandler() {
    configureByText("\n")
    typeText("<I<>" + "<I<>")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun testShiftsVisualBlockMode() {
    configureByText("foo<caret>foo\nfoobar\nfoobaz\n")
    typeText("<C-V>jjl>")
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
    typeTextInFile(">>", file)
    assertState(
      """
      |A Discovery

      |           ${c}I found it in a legendary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  fun `test shift right does not move caret with nostartofline`() {
    val file = """
      |A Discovery
      |
      |       I found it in a ${c}legendary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
    """.trimMargin()
    configureByText(file)
    enterCommand("set nostartofline")
    typeText(">>")
    assertState(
      """
      |A Discovery

      |           I found it i${c}n a legendary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
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
    typeTextInFile("i<C-T>", file)
    assertState(
      """
            A Discovery

                  I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
  }
}
