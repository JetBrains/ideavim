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

@Suppress("RemoveCurlyBracesFromTemplate", "SpellCheckingInspection")
class MotionWordLeftActionTest : VimTestCase() {
  @Test
  fun `test word left from within word`() {
    doTest("b", "Lorem ip${c}sum", "Lorem ${c}ipsum")
  }

  @Test
  fun `test word left from start of word`() {
    doTest("b", "Lorem ${c}ipsum", "${c}Lorem ipsum")
  }

  @Test
  fun `test word left stops at non-word characters`() {
    doTest("b", "Lorem ipsum;dolor.sit,a${c}met", "Lorem ipsum;dolor.sit,${c}amet")
  }

  @Test
  fun `test word left with Shift-Left`() {
    doTest("<S-Left>", "Lorem ipsum;dolor.sit,a${c}met", "Lorem ipsum;dolor.sit,${c}amet")
  }

  @Test
  fun `test word left from trailing whitespace`() {
    doTest("b", "Lorem Ipsum   ${c}   ", "Lorem ${c}Ipsum      ")
  }

  @Test
  fun `test word left from trailing whitespace 2`() {
    doTest("b", "Lorem  ${c}  Ipsum", "${c}Lorem    Ipsum")
  }

  @Test
  fun `test word left with count from within word`() {
    doTest(
      "3b",
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
  fun `test word left with count from start of word`() {
    doTest(
      "3b",
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
  fun `test word left with count across non-word characters`() {
    doTest(
      "3b",
      """
        |Lorem ipsum dolor sit amet, cons${c}ectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit ${c}amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test word left at start of line wraps to previous line`() {
    doTest(
      "b",
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
  fun `test word left at start of line wraps to previous line with trailing whitespace`() {
    doTest(
      "b",
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
  fun `test word left at start of line with count wraps to previous line`() {
    doTest(
      "3b",
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
  fun `test word left at start of line across non-word characters`() {
    doTest(
      "b",
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
        |Lorem ipsum dolor sit amet${c},
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test word left on first word moves caret to start of word`() {
    doTest("b", "Lo${c}rem Ipsum", "${c}Lorem Ipsum")
  }

  @Test
  fun `test word left on leading whitespace at start of file moves caret to start of file`() {
    doTest("b", "    ${c}  Lorem Ipsum", "${c}      Lorem Ipsum")
  }

  @Test
  fun `test word left on first character of file reports error`() {
    doTest("b", "${c}Lorem Ipsum", "${c}Lorem Ipsum")
    assertPluginError(true)
  }

  @Test
  fun `test empty line is treated as word`() {
    doTest(
      "<S-Left>",
      """
        |Lorem Ipsum
        |
        |${c}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |${c}
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test empty line is treated as word 2`() {
    doTest(
      "b",
      """
        |Lorem Ipsum
        |
        |
        |${c}
        |
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |${c}
        |
        |
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test empty line is treated as word 3`() {
    doTest(
      "3<S-Left>",
      """
        |Lorem Ipsum
        |
        |
        |
        |
        |
        |${c}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |
        |${c}
        |
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test blank line is not treated as word`() {
    doTest(
      "b",
      """
        |Lorem Ipsum
        |
        |...
        |${c}
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem Ipsum
        |${c}
        |...
        |
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace()
    )
  }

  @Test
  fun `test blank line is not treated as word 2`() {
    doTest(
      "<S-Left>",
      """
        |Lorem Ipsum
        |
        |
        |.${c}..
        |
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem Ipsum
        |
        |${c}
        |...
        |
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace()
    )
  }
}
