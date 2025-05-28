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

class ShiftLeftTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test shift till new line`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile("<W", file)
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

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test shift left positions caret at first non-blank char`() {
    val file = """
      |A Discovery
      |
      |       I found it in a legendary l${c}and
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
    """.trimMargin()
    typeTextInFile("<<", file)
    assertState(
      """
      |A Discovery

      |   ${c}I found it in a legendary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test shift left does not move caret with nostartofline`() {
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
    typeText("<<")
    assertState(
      """
      |A Discovery

      |   I found it in a lege${c}ndary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test shift left positions caret at end of line with nostartofline`() {
    val file = """
      |A Discovery
      |
      |       I found it in a legendary la${c}nd
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
    """.trimMargin()
    configureByText(file)
    enterCommand("set nostartofline")
    typeText("<<")
    assertState(
      """
      |A Discovery

      |   I found it in a legendary lan${c}d
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test shift ctrl-D`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile("i<C-D>", file)
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
  fun `test undo after shift left single line`() {
    configureByText("""
      func main() {
          ${c}println("Hello")
      }
    """.trimIndent())
    typeText("<<")
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
  fun `test undo after shift left with motion`() {
    configureByText("""
      func main() {
          ${c}line1()
          line2()
          line3()
      }
    """.trimIndent())
    typeText("<2j")  // Shift left 3 lines
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
  fun `test undo after shift left visual mode`() {
    configureByText("""
      func main() {
          ${c}line1()
          line2()
          line3()
      }
    """.trimIndent())
    typeText("Vj<")  // Visual select 2 lines and shift left
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
      ${c}    line1()
          line2()
          line3()
      }
    """.trimIndent())
  }

  @Test
  fun `test undo shift left in insert mode`() {
    configureByText("""
      func main() {
          ${c}println("Hello")
      }
    """.trimIndent())
    typeText("i<C-D>")
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
}
