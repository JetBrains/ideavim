/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.text

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection", "RemoveCurlyBracesFromTemplate")
class MotionBigWordEndLeftActionTest : VimTestCase() {
  @Test
  fun `test move to previous WORD end`() {
    doTest(
      "gE",
      "Lorem ip${c}sum dolor sit amet, consectetur adipiscing elit",
      "Lore${c}m ipsum dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous WORD end from end of word`() {
    doTest(
      "gE",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
      "Lore${c}m ipsum dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous WORD end from whitespace`() {
    doTest(
      "gE",
      "Lorem   ${c}   ipsum dolor sit amet, consectetur adipiscing elit",
      "Lore${c}m      ipsum dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous WORD end stops at non-word characters`() {
    doTest(
      "gE",
      "Lorem ipsum dolor sit amet, con${c}sectetur adipiscing elit",
      "Lorem ipsum dolor sit amet${c}, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous WORD end moves to end at non-word characters`() {
    doTest(
      "gE",
      "Lorem ipsum dolor sit amet,,, con${c}sectetur adipiscing elit",
      "Lorem ipsum dolor sit amet,,${c}, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous WORD end with count`() {
    doTest(
      "3gE",
      "Lorem ipsum dolor sit a${c}met, consectetur adipiscing elit",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous WORD end with count over non-word characters`() {
    doTest(
      "3gE",
      "Lorem ipsum dolor sit amet, conse${c}ctetur adipiscing elit",
      "Lorem ipsum dolo${c}r sit amet, consectetur adipiscing elit",
    )
  }

  @Test
  fun `test move to previous WORD end wraps to previous line`() {
    doTest(
      "gE",
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
  fun `test move to previous WORD end wraps to previous line skipping trailing whitespace`() {
    doTest(
      "gE",
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

  @Test
  fun `test move to previous word end on first word moves to start of file`() {
    doTest("gE", "Lore${c}m ipsum", "${c}Lorem ipsum")
  }

  @Test
  fun `test move to previous word end on first word with leading whitespace moves to start of file`() {
    doTest("gE", "    Lore${c}m ipsum", "${c}    Lorem ipsum")
  }

  @Test
  fun `test move to previous word end at start of file reports error`() {
    doTest("gE", "${c}Lorem ipsum", "${c}Lorem ipsum")
    assertPluginError(true)
  }

  @Test
  fun `test move to previous word end with large count moves to start of file without reporting error`() {
    doTest("100gE", "Lorem ip${c}sum", "${c}Lorem ipsum")
    assertPluginError(false)
  }
}
