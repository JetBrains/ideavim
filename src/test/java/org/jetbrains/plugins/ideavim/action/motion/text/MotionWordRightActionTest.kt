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
class MotionWordRightActionTest : VimTestCase() {
  @Test
  fun `test simple word right`() {
    doTest("w", "${c}Lorem Ipsum", "Lorem ${c}Ipsum")
  }

  @Test
  fun `test word right with non-alpha characters`() {
    doTest("w", "Lo${c}rem,ipsum;dolor.sit amet", "Lorem${c},ipsum;dolor.sit amet")
  }

  @Test
  fun `test word right with Shift-Right`() {
    doTest("<S-Right>", "Lo${c}rem,ipsum;dolor.sit amet", "Lorem${c},ipsum;dolor.sit amet")
  }

  @Test
  fun `test word right from leading whitespace`() {
    doTest("w", "${c}    Lorem Ipsum", "    ${c}Lorem Ipsum")
  }

  @Test
  fun `test word right from leading whitespace 2`() {
    doTest("w", "Lorem  ${c}  Ipsum", "Lorem    ${c}Ipsum")
  }

  @Test
  fun `test word right with count`() {
    doTest(
      "3w",
      """
        |Lo${c}rem ipsum dolor sit amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor ${c}sit amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test word right with count across non-word characters`() {
    doTest(
      "3w",
      """
        |Lorem ipsum dolor s${c}it amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, ${c}consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test word right at end of line wraps to next line`() {
    doTest(
      "w",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing e${c}lit
        |  Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit
        |  ${c}Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test word right at end of line with trailing whitespace wraps to next line`() {
    doTest(
      "w",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing e${c}lit....
        |  Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit....
        |  ${c}Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace()
    )
  }

  @Test
  fun `test word right at end of line with count wraps to next line`() {
    doTest(
      "3w",
      """
        |Lorem ipsum dolor sit amet, consectetur a${c}dipiscing elit
        |  Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit
        |  Sed ${c}in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test word right on last word moves caret to end of word`() {
    doTest("w", "Lorem I${c}psum", "Lorem Ipsu${c}m")
  }

  @Test
  fun `test word right on last word with trailing whitespace moves to end of whitespace`() {
    doTest("w", "Lorem Ipsu${c}m    ", "Lorem Ipsum   ${c} ")
  }

  @Test
  fun `test word right on last character of file reports error`() {
    doTest("w", "Lorem Ipsu${c}m", "Lorem Ipsu${c}m")
    assertPluginError(true)
  }

  @Test
  fun `test word right at end of file with large count does not report error`() {
    doTest("100w", "L${c}orem Ipsum", "Lorem Ipsu${c}m")
    assertPluginError(false)
  }
}
