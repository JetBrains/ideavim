/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
