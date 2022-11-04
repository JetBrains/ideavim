/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class ChangeLineActionTest : VimTestCase() {
  fun `test on empty file`() {
    setupChecks {
      this.neoVim.ignoredRegisters = setOf('1', '"')
    }
    doTest("cc", "", "", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test on empty file with S`() {
    setupChecks {
      this.neoVim.ignoredRegisters = setOf('1', '"')
    }
    doTest("S", "", "", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test on last line with S`() {
    doTest(
      "S",
      """
            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
      """.trimIndent(),
      """
            I found it in a legendary land
            $c
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun `test on last line with new line with S`() {
    doTest(
      "S",
      """
            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
            
      """.trimIndent(),
      """
            I found it in a legendary land
            $c
            
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun `test on very last line with new line with S`() {
    doTest(
      "S",
      """
            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
      """.trimIndent(),
      """
            I found it in a legendary land
            $c
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun `test on very last line with new line with S2`() {
    doTest(
      "S",
      """
            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
            
      """.trimIndent(),
      """
            I found it in a legendary land
            $c
            
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun `test on first line with new line with S`() {
    doTest(
      "S",
      """
            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
      """.trimIndent(),
      """
            $c
            all rocks and lavender and tufted grass,
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun `test on last line with new line with cc`() {
    doTest(
      "cc",
      """
            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
            
      """.trimIndent(),
      """
            I found it in a legendary land
            $c
            
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun `test on last line`() {
    doTest(
      "cc",
      """
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            $c
      """.trimIndent(),
      """
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            $c
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun `test S with count`() {
    doTest(
      "3S",
      """
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.

            My needles have teased out its sculpted sex;
            corroded tissues could no longer hide
            that priceless mote now dimpling the convex
            and limpid teardrop on a lighted slide.
      """.trimIndent(),
      """
            $c
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.

            My needles have teased out its sculpted sex;
            corroded tissues could no longer hide
            that priceless mote now dimpling the convex
            and limpid teardrop on a lighted slide.
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }
}
