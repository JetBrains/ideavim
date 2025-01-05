/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.text

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection", "RemoveCurlyBracesFromTemplate")
class MotionBigWordEndRightActionTest : VimTestCase() {
  @Test
  fun `test move to WORD end`() {
    doTest(
      "E",
      "${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "Lore${c}m ipsum dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to WORD end from end of word`() {
    doTest(
      "E",
      "Lore${c}m ipsum dolor sit amet, consectetur adipiscing elit",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to WORD end from leading whitespace`() {
    doTest(
      "E",
      "  ${c}  Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "    Lore${c}m ipsum dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to WORD end ignores non-word characters`() {
    doTest(
      "E",
      "Lorem ipsum dolor sit ${c}amet, consectetur adipiscing elit",
      "Lorem ipsum dolor sit amet${c}, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to WORD end moves to end of non-word characters`() {
    doTest(
      "E",
      "Lorem ipsum dolor sit a${c}met,,,, consectetur adipiscing elit",
      "Lorem ipsum dolor sit amet,,,${c}, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to WORD end with count`() {
    doTest(
      "3E",
      "${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "Lorem ipsum dolo${c}r sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to WORD end with count over non-word characters`() {
    doTest(
      "3E",
      "Lorem ipsum dolor si${c}t amet, consectetur adipiscing elit",
      "Lorem ipsum dolor sit amet, consectetur adipiscin${c}g elit",
    )
  }

  @Test
  fun `test move to WORD end wraps to next line`() {
    doTest(
      "E",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing eli${c}t
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit
        |Se${c}d in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
  }

  @Test
  fun `test move to WORD end wraps to next line skipping trailing whitespace`() {
    doTest(
      "E",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing eli${c}t....
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit....
        |Se${c}d in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
    )
  }

  @Test
  fun `test move to WORD end at end of file reports error`() {
    doTest("E", "Lorem ipsu${c}m", "Lorem ipsu${c}m")
    assertPluginError(true)
  }

  @VimBehaviorDiffers(description = "IdeaVim cannot move and have an error at the same time")
  @Test
  fun `test move to WORD end with large count moves to end of file and reports error`() {
    doTest("300E", "${c}Lorem ipsum", "Lorem ipsu${c}m")
    assertPluginError(false)
  }
}
