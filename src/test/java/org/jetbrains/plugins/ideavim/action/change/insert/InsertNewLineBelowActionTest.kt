/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertNewLineBelowActionTest : VimTestCase() {
  @Test
  fun `test insert new line below`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("o", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line below with caret in middle of line`() {
    val before = """I found it in a legendary land
        |all rocks and ${c}lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("o", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line below matches indent for plain text`() {
    val before = """    I found it in a legendary land
        |    ${c}all rocks and lavender and tufted grass,
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("o", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line below matches indent for plain text 1`() {
    val before = """    I found it in a legendary land
        | $c   all rocks and lavender and tufted grass,
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("o", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line below with multiple carets`() {
    val before = """    I fou${c}nd it in a legendary land
        |    all rocks and laven${c}der and tufted grass,
        |    where it was sett${c}led on some sodden sand
        |    hard by the tor${c}rent of a mountain pass.
    """.trimMargin()
    val after = """    I found it in a legendary land
        |    $c
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    $c
        |    hard by the torrent of a mountain pass.
        |    $c
    """.trimMargin()
    doTest("o", before, after, Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test insert new line below at bottom of screen does not scroll bottom of screen`() {
    configureByLines(50, "I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(5, 29)
    typeText("o")
    assertPosition(30, 0)
    assertVisibleArea(6, 40)
  }

  @Test
  fun `test insert new line below with count`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("5o", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line below with count and escape`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |123
        |123
        |123
        |123
        |12${c}3
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("5o123<esc>", before, after)
  }

  @Test
  fun `test insert new line below with folds`() {
    val before = """I found it in a legendary land
        |${c}all rocks [and lavender] and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()

    configureAndFold(before, "")

    performTest("o", after, Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.FOLDING, "Neovim doesn't support arbitrary folds")
  @Test
  fun `test insert new line below with folds 2`() {
    val before = """I found it in a legendary land
        |${c}all rocks [and lavender and tufted grass,
        |where it was settled] on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |$c
        |hard by the torrent of a mountain pass.
    """.trimMargin()

    configureAndFold(before, "")

    performTest("o", after, Mode.INSERT)
  }

  @Test
  fun `test pycharm notebook folders`() {
    val before = """[I found it in a legendary land
        |]${c}all rocks and lavender and tufted grass,
        |[where it was settled on some sodden sand
        |]hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()

    configureAndFold(before, "")

    performTest("o", after, Mode.INSERT)
  }

  @Test
  fun `test insert new line above clears status line`() {
    configureByText("lorem ipsum")
    enterSearch("dolor")
    assertStatusLineMessageContains("Pattern not found: dolor")
    typeText("o")
    assertStatusLineCleared()
  }
}
