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

class InsertNewLineAboveActionTest : VimTestCase() {
  @Test
  fun `test insert new line above`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |$c
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line above with caret in middle of line`() {
    val before = """I found it in a legendary land
        |all rocks and ${c}lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |$c
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line above matches indent for plain text`() {
    val before = """    Lorem ipsum dolor sit amet,
        |    consectetur adipiscing elit
        |    ${c}Sed in orci mauris.
        |    Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """    Lorem ipsum dolor sit amet,
        |    consectetur adipiscing elit
        |    $c
        |    Sed in orci mauris.
        |    Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line above matches indent for first line of plain text`() {
    val before = """    ${c}Lorem ipsum dolor sit amet,
        |    consectetur adipiscing elit
        |    Sed in orci mauris.
        |    Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """    $c
        |    Lorem ipsum dolor sit amet,
        |    consectetur adipiscing elit
        |    Sed in orci mauris.
        |    Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line above with multiple carets`() {
    val before = """    I fou${c}nd it in a legendary land
        |    all rocks and laven${c}der and tufted grass,
        |    where it was sett${c}led on some sodden sand
        |    hard by the tor${c}rent of a mountain pass.
    """.trimMargin()
    val after = """    $c
        |    I found it in a legendary land
        |    $c
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    $c
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test insert new line above at top of screen does not scroll top of screen`() {
    configureByLines(50, "Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(5, 15)
    typeText("O")
    assertPosition(15, 0)
    assertVisibleArea(5, 39)
  }

  @Test
  fun `test insert new line above first line`() {
    val before = """${c}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """
        |$c
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }
}
