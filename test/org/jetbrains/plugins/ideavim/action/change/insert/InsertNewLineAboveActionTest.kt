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

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertNewLineAboveActionTest : VimTestCase() {
  fun `test insert new line above`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |$c
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest("O", before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test insert new line above with caret in middle of line`() {
    val before = """I found it in a legendary land
        |all rocks and ${c}lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |$c
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest("O", before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test insert new line above matches indent for plain text`() {
    val before = """    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    ${c}where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.""".trimMargin()
    val after = """    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.""".trimMargin()
    doTest("O", before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test insert new line above matches indent for first line of plain text`() {
    val before = """    ${c}I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.""".trimMargin()
    val after = """    $c
        |    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.""".trimMargin()
    doTest("O", before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
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
      typeText(parseKeys("O"))
      assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.MULTICARET)
  fun `test insert new line above with multiple carets`() {
    val before = """    I fou${c}nd it in a legendary land
        |    all rocks and laven${c}der and tufted grass,
        |    where it was sett${c}led on some sodden sand
        |    hard by the tor${c}rent of a mountain pass.""".trimMargin()
    val after = """    $c
        |    I found it in a legendary land
        |    $c
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    $c
        |    hard by the torrent of a mountain pass.""".trimMargin()
    doTest("O", before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert new line above at top of screen does not scroll top of screen`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(50, "I found it in a legendary land")
    setPositionAndScroll(5, 15)
    typeText(parseKeys("O"))
    assertPosition(15, 0)
    assertVisibleArea(5, 39)
  }
}
