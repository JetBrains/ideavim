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
class MotionWordEndLeftActionTest : VimTestCase() {
  @Test
  fun `test move to previous word end`() {
    doTest(
      "ge",
      "Lorem ip${c}sum dolor sit amet, consectetur adipiscing elit",
      "Lore${c}m ipsum dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous word end from end of word`() {
    doTest(
      "ge",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
      "Lore${c}m ipsum dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous word end from whitespace`() {
    doTest(
      "ge",
      "Lorem   ${c}   ipsum dolor sit amet, consectetur adipiscing elit",
      "Lore${c}m      ipsum dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous word end stops at non-word characters`() {
    doTest(
      "ge",
      "Lorem ipsum dolor sit amet, con${c}sectetur adipiscing elit",
      "Lorem ipsum dolor sit amet${c}, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous word end moves to end at non-word characters`() {
    doTest(
      "ge",
      "Lorem ipsum dolor sit amet,,, con${c}sectetur adipiscing elit",
      "Lorem ipsum dolor sit amet,,${c}, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous word end with count`() {
    doTest(
      "3ge",
      "Lorem ipsum dolor sit a${c}met, consectetur adipiscing elit",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous word end with count over non-word characters`() {
    doTest(
      "3ge",
      "Lorem ipsum dolor sit amet, conse${c}ctetur adipiscing elit",
      "Lorem ipsum dolor si${c}t amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous word end wraps to previous line`() {
    doTest(
      "ge",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit
        |Se${c}d in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing eli${c}t
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
  }

  @Test
  fun `test move to previous word end wraps to previous line skipping trailing whitespace`() {
    doTest(
      "ge",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit....
        |Se${c}d in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing eli${c}t....
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
    )
  }

  // TODO: Fix this bug
  @VimBehaviorDiffers(originalVimAfter = "${c}Lorem ipsum")
  @Test
  fun `test move to previous word end on first word moves to start of file`() {
    doTest("ge", "Lore${c}m ipsum", "Lore${c}m ipsum")
  }

  @Test
  fun `test move to previous word end on first word with leading whitespace moves to start of file`() {
    doTest("ge", "    Lore${c}m ipsum", "${c}    Lorem ipsum")
  }

  @Test
  fun `test move to previous word end at start of file reports error`() {
    doTest("ge", "${c}Lorem ipsum", "${c}Lorem ipsum")
    assertPluginError(true)
  }

  // TODO: Fix this bug
  @VimBehaviorDiffers(originalVimAfter = "${c}Lorem ipsum")
  @Test
  fun `test move to previous word end with large count moves to start of file without reporting error`() {
    doTest("100ge", "Lorem ip${c}sum", "Lore${c}m ipsum")
    assertPluginError(false)
  }
}
