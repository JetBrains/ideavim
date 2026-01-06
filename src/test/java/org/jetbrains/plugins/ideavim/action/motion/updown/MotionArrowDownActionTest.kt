/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionArrowDownActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual default options`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${s}rocks and lavender and tufted grass,
                wher${c}e${se} it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual stopsel`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wher${c}e it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=stopsel")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual stopselect`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${s}rocks and lavender and tufted grass,
                wher${c}e${se} it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    ) {
      enterCommand("set keymodel=stopselect")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual stopvisual`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wher${c}e it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=stopvisual")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual stopvisual multicaret`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wher${c}e it was settled on some sodden sand
                hard by the t${c}orrent of a mountain pass.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=stopvisual")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test char select stopsel`() {
    doTest(
      listOf("gh", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${s}rocks and lavender and tufted grass,
                where${c}${se} it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test virtual edit down to shorter line`() {
    doTest(
      listOf("<Down>"),
      """
            class MyClass ${c}{
            }
      """.trimIndent(),
      """
            class MyClass {
            }${c}
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test virtual edit down to shorter line after dollar`() {
    doTest(
      listOf("$", "<Down>"),
      """
            class ${c}MyClass {
            }
      """.trimIndent(),
      """
            class MyClass {
            ${c}}
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  // Once you press '$', then any up or down actions stay on the end of the current line.
  // Any non up/down action breaks this.
  private val start = """
            what ${c}a long line I am
            yet I am short
            Lo and behold, I am the longest yet
            nope.
  """.trimIndent()

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test up and down after dollar`() {
    // Arrow keys
    doTest(
      listOf("$", "<Down>"),
      start,
      """
            what a long line I am
            yet I am shor${c}t
            Lo and behold, I am the longest yet
            nope.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test up and down after dollar1`() {
    doTest(
      listOf("$", "<Down>", "<Down>"),
      start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest ye${c}t
            nope.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test up and down after dollar2`() {
    doTest(
      listOf("$", "<Down>", "<Down>", "<Down>"),
      start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest yet
            nope${c}.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test up and down after dollar3`() {
    doTest(
      listOf("$", "<Down>", "<Down>", "<Down>", "<Up>"),
      start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest ye${c}t
            nope.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test up and down after dollar4`() {
    // j k keys

    doTest(
      listOf("$", "j"),
      start,
      """
            what a long line I am
            yet I am shor${c}t
            Lo and behold, I am the longest yet
            nope.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test up and down after dollar5`() {
    doTest(
      listOf("$", "j", "j"),
      start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest ye${c}t
            nope.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test up and down after dollar6`() {
    doTest(
      listOf("$", "j", "j", "j"),
      start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest yet
            nope${c}.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test up and down after dollar7`() {
    doTest(
      listOf("$", "j", "j", "j", "k"),
      start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest ye${c}t
            nope.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test char select simple move`() {
    doTest(
      listOf("gH", "<Down>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=stopselect")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test select multiple carets`() {
    doTest(
      listOf("gH", "<Down>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.
      """.trimIndent(),
    ) {
      enterCommand("set keymodel=stopselect")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test arrow down in insert mode scrolls caret at scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(0, 29)

    typeText("i", "<Down>")
    assertPosition(30, 0)
    assertVisibleArea(10, 44)
  }
}
