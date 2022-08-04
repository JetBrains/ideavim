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

package org.jetbrains.plugins.ideavim.extension.paragraphmotion

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class ParagraphMotionTest : VimTestCase() {

  override fun setUp() {
    super.setUp()
    enableExtensions("vim-paragraph-motion")
  }

  fun `test paragraph next without whitespace`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest("}", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test paragraph next with whitespace`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |....
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c....
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    doTest("}", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test paragraph next with whitespace visual`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |....
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    val after = """I found it in a legendary land
        |${s}all rocks and lavender and tufted grass,
        |$c.$se...
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    doTest("v}", before, after, VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }

  fun `test paragraph next with whitespace delete`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |....
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    val after = """I found it in a legendary land
        |$c
        |....
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    doTest("d}", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test paragraph prev without whitespace`() {
    val before = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |
        |${c}where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest("{", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test paragraph prev with whitespace`() {
    val before = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |....
        |${c}where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c....
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    doTest("{", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test paragraph prev with whitespace visual`() {
    val before = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |....
        |${c}where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |${s}$c....
        |w${se}here it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    doTest("v{", before, after, VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }

  fun `test paragraph prev with whitespace delete`() {
    val before = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |....
        |${c}where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |$c
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin().dotToSpace()
    doTest("d{", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }
}
