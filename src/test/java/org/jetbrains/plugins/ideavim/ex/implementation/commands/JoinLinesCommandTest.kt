/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class JoinLinesCommandTest : VimTestCase() {
  @Test
  fun `test simple join`() {
    doTest(
      exCommand("j"),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }

  @Test
  fun `test simple join full command`() {
    doTest(
      exCommand("join"),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }

  @Test
  fun `test join on last line deletes nothing and reports error`() {
    doTest(
      exCommand("j"),
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                ${c}Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                ${c}Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertPluginError(true)
  }

  @Test
  fun `test join with range`() {
    doTest(
      exCommand("4,6j"),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,
                ${c}consectetur adipiscing elit Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }

  @Test
  fun `test join with visual range`() {
    doTest(
      listOf("vj", exCommand("'<,'>j")),
      """
                Lorem Ipsum

                Lorem ${c}ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }

  @Test
  fun `test join with count`() {
    doTest(
      exCommand("j 3"),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }

  @Test
  fun `test join with too large count`() {
    doTest(
      exCommand("j 300"),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }

  @Test
  fun `test join with invalid count`() {
    doTest(
      exCommand("j 3,4"),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: ,4")
  }

  @Test
  fun `test join multicaret`() {
    configureByText(
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
//    typeText(injector.parser.parseKeys("Vjj"))
    typeText(commandToKeys("join"))
    assertState(
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }

  @Test
  fun `test join visual multicaret`() {
    configureByText(
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("Vjj"))
    typeText(commandToKeys("join"))
    assertState(
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }
}
