/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertNewLineAboveActionTest : VimTestCase() {
  fun `test insert new line above`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |$c
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("O", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

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
    doTest("O", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test insert new line above matches indent for plain text`() {
    val before = """    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    ${c}where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("O", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test insert new line above matches indent for first line of plain text`() {
    val before = """    ${c}I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """    $c
        |    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("O", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN) // Java support would be a neovim plugin
  fun `test insert new line above matches indent for java`() {
    val before = """public class C {
      |  Integer a;
      |  ${c}Integer b;
      |}
    """.trimMargin()
    val after = """public class C {
      |  Integer a;
      |  $c
      |  Integer b;
      |}
    """.trimMargin()
    configureByJavaText(before)
    typeText("O")
    assertState(after)
  }

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
    doTest("O", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert new line above at top of screen does not scroll top of screen`() {
    configureByLines(50, "I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(5, 15)
    typeText("O")
    assertPosition(15, 0)
    assertVisibleArea(5, 39)
  }

  fun `test insert new line above first line`() {
    val before = """${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """
        |$c
        |I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("O", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }
}
