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
import org.junit.jupiter.api.Test

class ShiftRightTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
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
  @Test
  fun testShiftShiftsOneCharacterSingleLine() {
    configureByText("<caret>w\n")
    typeText(">>")
    assertState("    w\n")
  }

  // VIM-407
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftShiftsOneCharacterMultiLine() {
    configureByText("Hello\n<caret>w\nWorld")
    typeText(">>")
    assertState("Hello\n    w\nWorld")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftShiftsMultipleCharactersOneLine() {
    configureByText("<caret>Hello, world!\n")
    typeText(">>")
    assertState("    Hello, world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftShiftsMultipleCharactersMultipleLines() {
    configureByText("<caret>Hello,\nworld!\n")
    typeText("j>>")
    assertState("Hello,\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftsSingleLineSelection() {
    configureByText("<caret>Hello,\nworld!\n")
    typeText("jv$>>")
    assertState("Hello,\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftsMultiLineSelection() {
    configureByText("<caret>Hello,\nworld!\n")
    typeText("vj$>>")
    assertState("    Hello,\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftsMultiLineSelectionSkipsNewline() {
    configureByText("<caret>Hello,\nworld!\n\n")
    typeText("vG$>>")
    assertState("    Hello,\n    world!\n\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftsMultiLineSelectionSkipsNewlineWhenCursorNotInFirstColumn() {
    configureByText("<caret>Hello,\n\nworld!\n")
    typeText("lVG>")
    assertState("    Hello,\n\n    world!\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftsMultiLineSelectionAddsTrailingWhitespaceIfTherePreviouslyWas() {
    configureByText("<caret>Hello,\n    \nworld!\n")
    typeText("lVG>")
    assertState("    Hello,\n        \n    world!\n")
  }

  // VIM-705 repeating a multiline indent would only affect last line
  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftsMultiLineSelectionRepeat() {
    configureByText("<caret>a\nb\n")
    typeText("Vj>.")
    assertState("        a\n        b\n")
  }

  @Test
  fun testShiftsDontCrashKeyHandler() {
    configureByText("\n")
    typeText("<I<>" + "<I<>")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun testShiftsVisualBlockMode() {
    configureByText("foo<caret>foo\nfoobar\nfoobaz\n")
    typeText("<C-V>jjl>")
    assertState("foo    foo\nfoo    bar\nfoo    baz\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
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
  @Test
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
  @Test
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

  @Test
  fun `test undo after shift right single line`() {
    configureByText("""
      func main() {
      ${c}println("Hello")
      }
    """.trimIndent())
    typeText(">>")
    assertState("""
      func main() {
          ${c}println("Hello")
      }
    """.trimIndent())
    typeText("u")
    assertState("""
      func main() {
      ${c}println("Hello")
      }
    """.trimIndent())
  }

  @Test
  fun `test undo after shift right with motion`() {
    configureByText("""
      func main() {
      ${c}line1()
      line2()
      line3()
      }
    """.trimIndent())
    typeText(">2j")  // Shift right 3 lines
    assertState("""
      func main() {
          ${c}line1()
          line2()
          line3()
      }
    """.trimIndent())
    typeText("u")
    assertState("""
      func main() {
      ${c}line1()
      line2()
      line3()
      }
    """.trimIndent())
  }

  @Test
  fun `test undo after shift right visual mode`() {
    configureByText("""
      func main() {
      ${c}line1()
      line2()
          line3()
      }
    """.trimIndent())
    typeText("Vj>")  // Visual select 2 lines and shift right
    assertState("""
      func main() {
          ${c}line1()
          line2()
          line3()
      }
    """.trimIndent())
    typeText("u")
    assertState("""
      func main() {
      ${c}line1()
      line2()
          line3()
      }
    """.trimIndent())
  }

  @Test
  fun `test multiple undo after sequential shifts right`() {
    configureByText("${c}unindented line")
    typeText(">>")
    assertState("    ${c}unindented line")
    typeText(">>")
    assertState("        ${c}unindented line")
    typeText(">>")
    assertState("            ${c}unindented line")
    
    // Undo third shift
    typeText("u")
    assertState("        ${c}unindented line")
    
    // Undo second shift
    typeText("u")
    assertState("    ${c}unindented line")
    
    // Undo first shift
    typeText("u")
    assertState("${c}unindented line")
  }

  @Test
  fun `test undo shift right in insert mode`() {
    configureByText("""
      func main() {
      ${c}println("Hello")
      }
    """.trimIndent())
    typeText("i<C-T>")
    assertState("""
      func main() {
          ${c}println("Hello")
      }
    """.trimIndent())
    typeText("<Esc>")
    typeText("u")
    assertState("""
      func main() {
      ${c}println("Hello")
      }
    """.trimIndent())
  }

  @Test
  fun `test undo shift right and left combination`() {
    configureByText("""
      func main() {
          ${c}println("Hello")
      }
    """.trimIndent())
    typeText(">>")  // Shift right
    assertState("""
      func main() {
              ${c}println("Hello")
      }
    """.trimIndent())
    typeText("<<")  // Shift left
    assertState("""
      func main() {
          ${c}println("Hello")
      }
    """.trimIndent())
    
    // Undo shift left
    typeText("u")
    assertState("""
      func main() {
              ${c}println("Hello")
      }
    """.trimIndent())
    
    // Undo shift right
    typeText("u")
    assertState("""
      func main() {
          ${c}println("Hello")
      }
    """.trimIndent())
  }
}
