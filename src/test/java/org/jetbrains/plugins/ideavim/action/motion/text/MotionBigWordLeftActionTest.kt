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

@Suppress("RemoveCurlyBracesFromTemplate")
class MotionBigWordLeftActionTest : VimTestCase() {
  @Test
  fun `test WORD left from within word`() {
    doTest("B", "Lorem ip${c}sum", "Lorem ${c}ipsum")
  }

  @Test
  fun `test WORD left from start of word`() {
    doTest("B", "Lorem ${c}ipsum", "${c}Lorem ipsum")
  }

  @Test
  fun `test WORD left ignores non-word characters`() {
    doTest("B", "Lorem ipsum;dolor.sit,a${c}met", "Lorem ${c}ipsum;dolor.sit,amet")
  }

  @Test
  fun `test WORD left with CTRL-Left`() {
    doTest("<C-Left>", "Lorem ipsum;dolor.sit,a${c}met", "Lorem ${c}ipsum;dolor.sit,amet")
  }

  @Test
  fun `test WORD left from trailing whitespace`() {
    doTest("B", "Lorem Ipsum   ${c}   ", "Lorem ${c}Ipsum      ")
  }

  @Test
  fun `test WORD left from trailing whitespace 2`() {
    doTest("B", "Lorem  ${c}  Ipsum", "${c}Lorem    Ipsum")
  }

  @Test
  fun `test WORD left with count from within word`() {
    doTest(
      "3B",
      """
        |Lorem ipsum dolor s${c}it amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test WORD left with count from start of word`() {
    doTest(
      "3B",
      """
        |Lorem ipsum dolor ${c}sit amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test WORD left with count across non-word characters`() {
    doTest(
      "3B",
      """
        |Lorem ipsum dolor sit amet, cons${c}ectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor ${c}sit amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test WORD left at start of line wraps to previous line`() {
    doTest(
      "B",
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |${c}Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing ${c}elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test WORD left at start of line wraps to previous line with trailing whitespace`() {
    doTest(
      "B",
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit....
        |${c}Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing ${c}elit....
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace()
    )
  }

  @Test
  fun `test WORD left at start of line with count wraps to previous line`() {
    doTest(
      "3B",
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Se${c}d in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur ${c}adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test WORD left at start of line across non-word characters`() {
    doTest(
      "B",
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit ${c}amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test WORD left on first word moves caret to start of word`() {
    doTest("B", "Lo${c}rem Ipsum", "${c}Lorem Ipsum")
  }

  @Test
  fun `test WORD left on leading whitespace at start of file moves caret to start of file`() {
    doTest("B", "    ${c}  Lorem Ipsum", "${c}      Lorem Ipsum")
  }

  @Test
  fun `test WORD left on first character of file reports error`() {
    doTest("B", "${c}Lorem Ipsum", "${c}Lorem Ipsum")
    assertPluginError(true)
  }
}
