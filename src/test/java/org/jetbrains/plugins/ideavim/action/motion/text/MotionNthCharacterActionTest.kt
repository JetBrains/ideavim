/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.text

import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionNthCharacterActionTest : VimTestCase() {
  fun `test goto without count`() {
    doTest(
      "go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test goto with 0`() {
    doTest(
      "0go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test goto with 1`() {
    doTest(
      "1go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test goto with 2`() {
    doTest(
      "2go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A$c Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test goto with 10`() {
    doTest(
      "10go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discove${c}ry

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test goto with 1000`() {
    doTest(
      "1000go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @VimBehaviorDiffers(
    """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.$c
  """
  )
  fun `test goto with 1000 in visual mode`() {
    doTest(
      "v1000go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it in a legendary land
            all rocks ${s}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass$c.$se
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER
    )
  }

  @VimBehaviorDiffers(
    """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            $c
  """
  )
  fun `test goto with 1000 and new line`() {
    doTest(
      "1000go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            
      """.trimIndent(),
      """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass$c.
            
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }
}
