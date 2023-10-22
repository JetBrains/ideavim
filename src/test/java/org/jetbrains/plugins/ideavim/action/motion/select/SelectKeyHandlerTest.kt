/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */
class SelectKeyHandlerTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test type in select mode`() {
    val typed = "Hello"
    this.doTest(
      listOf("gh", "<S-Right>", typed),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                ${typed}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test char mode on empty line`() {
    val typed = "Hello"
    this.doTest(
      listOf("gh", typed),
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery
                $typed
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test char mode backspace`() {
    this.doTest(
      listOf("gh", "<BS>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test char mode delete`() {
    this.doTest(
      listOf("gh", "<DEL>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test char mode multicaret`() {
    val typed = "Hello"
    this.doTest(
      listOf("gh", "<S-Right>", typed),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${typed}und it in a legendary land
                all rocks and ${typed}vender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test line mode`() {
    val typed = "Hello"
    this.doTest(
      listOf("gH", typed),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                $typed
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test line mode empty line`() {
    val typed = "Hello"
    this.doTest(
      listOf("gH", typed),
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery
                $typed
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test line mode multicaret`() {
    val typed = "Hello"
    this.doTest(
      listOf("gH", typed),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                Hello
                Hello
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test type in select block mode`() {
    val typed = "Hello"
    this.doTest(
      listOf("g<C-H>", "<S-Down>", "<S-Right>", typed),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                ${typed}found it in a legendary land
                ${typed}l rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
                A Discovery
                Hello
                Hellofound it in a legendary land
                Hellol rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test block mode empty line`() {
    val typed = "Hello"
    this.doTest(
      listOf("g<C-H>", "<S-Down>".repeat(2), "<S-Right>", typed),
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                $typed found it in a legendary land
                ${typed}ll rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test block mode longer line`() {
    val typed = "Hello"
    this.doTest(
      listOf("g<C-H>", "<S-Down>", "<S-Right>".repeat(2), typed),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary lan$typed
                all rocks and lavender and tu${typed}d grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test block mode longer line with esc`() {
    val typed = "Hello"
    this.doTest(
      listOf("g<C-H>", "<S-Down>", "<S-Right>".repeat(2), typed, "<esc>"),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary lanHell${c}o
                all rocks and lavender and tuHell${c}od grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
    assertCaretsVisualAttributes()
    assertMode(Mode.NORMAL())
  }
}
