/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertNewLineBelowActionTest : VimTestCase() {
  fun `test insert new line below`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest("o", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test insert new line below with caret in middle of line`() {
    val before = """I found it in a legendary land
        |all rocks and ${c}lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest("o", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test insert new line below matches indent for plain text`() {
    val before = """    I found it in a legendary land
        |    ${c}all rocks and lavender and tufted grass,
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.""".trimMargin()
    val after = """    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.""".trimMargin()
    doTest("o", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test insert new line below matches indent for plain text 1`() {
    val before = """    I found it in a legendary land
        | $c   all rocks and lavender and tufted grass,
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.""".trimMargin()
    val after = """    I found it in a legendary land
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    hard by the torrent of a mountain pass.""".trimMargin()
    doTest("o", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN) // Java support would be a neovim plugin
  fun `test insert new line below matches indent for java`() {
    val before = """public class C {
      |  ${c}Integer a;
      |  Integer b;
      |}
    """.trimMargin()
    val after = """public class C {
      |  Integer a;
      |  $c
      |  Integer b;
      |}
    """.trimMargin()
    configureByJavaText(before)
    typeText(injector.parser.parseKeys("o"))
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN) // Java support would be a neovim plugin
  fun `test insert new line below matches indent for java 1`() {
    val before = """public class C {
      |$c  Integer a;
      |  Integer b;
      |}
    """.trimMargin()
    val after = """public class C {
      |  Integer a;
      |  $c
      |  Integer b;
      |}
    """.trimMargin()
    configureByJavaText(before)
    typeText(injector.parser.parseKeys("o"))
    assertState(after)
  }

  fun `test insert new line below with multiple carets`() {
    val before = """    I fou${c}nd it in a legendary land
        |    all rocks and laven${c}der and tufted grass,
        |    where it was sett${c}led on some sodden sand
        |    hard by the tor${c}rent of a mountain pass.""".trimMargin()
    val after = """    I found it in a legendary land
        |    $c
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    $c
        |    hard by the torrent of a mountain pass.
        |    $c""".trimMargin()
    doTest("o", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert new line below at bottom of screen does not scroll bottom of screen`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByLines(50, "I found it in a legendary land")
    setPositionAndScroll(5, 29)
    typeText(injector.parser.parseKeys("o"))
    assertPosition(30, 0)
    assertVisibleArea(6, 40)
  }

  fun `test insert new line below with count`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest("5o", before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test insert new line below with count and escape`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |123
        |123
        |123
        |123
        |12${c}3
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest("5o123<esc>", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test insert new line below with folds`() {
    val before = """I found it in a legendary land
        |${c}all rocks [and lavender] and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()

    configureAndFold(before, "")

    performTest("o", after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.FOLDING, "Neovim doesn't support arbitrary folds")
  fun `test insert new line below with folds 2`() {
    val before = """I found it in a legendary land
        |${c}all rocks [and lavender and tufted grass,
        |where it was settled] on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |$c
        |hard by the torrent of a mountain pass.""".trimMargin()

    configureAndFold(before, "")

    performTest("o", after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test pycharm notebook folders`() {
    val before = """[I found it in a legendary land
        |]${c}all rocks and lavender and tufted grass,
        |[where it was settled on some sodden sand
        |]hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()

    configureAndFold(before, "")

    performTest("o", after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }
}
