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
class MotionBigWordRightActionTest : VimTestCase() {
  @Test
  fun `test simple WORD right`() {
    doTest("W", "${c}Lorem Ipsum", "Lorem ${c}Ipsum")
  }

  @Test
  fun `test WORD right ignoring non-alpha characters`() {
    doTest("W", "Lo${c}rem,ipsum;dolor.sit amet", "Lorem,ipsum;dolor.sit ${c}amet")
  }

  @Test
  fun `test WORD right with CTRL-Right`() {
    doTest("<C-Right>", "Lo${c}rem,ipsum;dolor.sit amet", "Lorem,ipsum;dolor.sit ${c}amet")
  }

  @Test
  fun `test WORD right from leading whitespace`() {
    doTest("W", "${c}    Lorem Ipsum", "    ${c}Lorem Ipsum")
  }

  @Test
  fun `test WORD right from leading whitespace 2`() {
    doTest("W", "Lorem  ${c}  Ipsum", "Lorem    ${c}Ipsum")
  }

  @Test
  fun `test WORD right with count`() {
    doTest(
      "3W",
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
  fun `test WORD right with count across non-word characters`() {
    doTest(
      "3W",
      """
        |Lorem ipsum dolor s${c}it amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur ${c}adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test WORD right at end of line wraps to next line`() {
    doTest(
      "W",
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
  fun `test WORD right at end of line with trailing whitespace wraps to next line`() {
    doTest(
      "W",
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
  fun `test WORD right at end of line with count wraps to next line`() {
    doTest(
      "3W",
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
  fun `test WORD right on last word moves caret to end of word`() {
    doTest("W", "Lorem I${c}psum", "Lorem Ipsu${c}m")
  }

  @Test
  fun `test WORD right on last word with trailing whitespace moves to end of whitespace`() {
    doTest("W", "Lorem Ipsu${c}m    ", "Lorem Ipsum   ${c} ")
  }

  @Test
  fun `test WORD right on last character of file reports error`() {
    doTest("W", "Lorem Ipsu${c}m", "Lorem Ipsu${c}m")
    assertPluginError(true)
  }

  @Test
  fun `test WORD right at end of file with large count does not report error`() {
    doTest("100W", "L${c}orem Ipsum", "Lorem Ipsu${c}m")
    assertPluginError(false)
  }

  @Test
  fun `test empty line is treated as WORD`() {
    doTest(
      "W",
      """
        |Lorem Ip${c}sum
        |
        |Lorem ipsum dolor sit amet,
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
  fun `test empty line is treated as WORD 2`() {
    doTest(
      "<C-Right>",
      """
        |Lorem Ipsum
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
        |
        |${c}
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test empty line is treated as WORD 3`() {
    doTest(
      "3<C-Right>",
      """
        |Lorem Ip${c}sum
        |
        |
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
        |
        |${c}
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test blank line is not treated as WORD`() {
    doTest(
      "W",
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
      """.trimMargin().dotToSpace(),
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
      """.trimMargin().dotToSpace()
    )
  }

  @Test
  fun `test blank line is not treated as WORD 2`() {
    doTest(
      "<C-Right>",
      """
        |Lorem Ipsum
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
        |...
        |${c}
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace()
    )
  }
}
