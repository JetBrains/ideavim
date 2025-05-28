/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestIjOptionConstants
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption
import org.junit.jupiter.api.Test

@TraceOptions
class DeleteJoinLinesSpacesActionTest : VimTestCase() {
  @OptionTest(VimOption(TestIjOptionConstants.ideajoin))
  fun `test join with idea`() {
    doTest(
      "J",
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @OptionTest(VimOption(TestIjOptionConstants.ideajoin))
  fun `test join with idea with count`() {
    doTest(
      "3J",
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet, consectetur adipiscing elit$c Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestIjOptionConstants.ideajoin))
  fun `test join with idea with large count`() {
    doTest(
      "10J",
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
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test undo after join lines`() {
    configureByText("""
      First line
      ${c}Second line
      Third line
    """.trimIndent())
    typeText("J")
    assertState("""
      First line
      Second line${c} Third line
    """.trimIndent())
    typeText("u")
    assertState("""
      First line
      ${c}Second line
      Third line
    """.trimIndent())
  }

  @Test
  fun `test undo after join multiple lines`() {
    configureByText("""
      ${c}Line 1
      Line 2
      Line 3
      Line 4
    """.trimIndent())
    typeText("3J")
    assertState("""
      Line 1 Line 2 Line 3$c Line 4
    """.trimIndent())
    typeText("u")
    assertState("""
      ${c}Line 1
      Line 2
      Line 3
      Line 4
    """.trimIndent())
  }

  @Test
  fun `test undo after multiple sequential joins`() {
    configureByText("""
      ${c}One
      Two
      Three
      Four
    """.trimIndent())
    typeText("J")
    assertState("""
      One${c} Two
      Three
      Four
    """.trimIndent())
    typeText("J")
    assertState("""
      One Two${c} Three
      Four
    """.trimIndent())
    
    // Undo second join
    typeText("u")
    assertState("""
      One${c} Two
      Three
      Four
    """.trimIndent())
    
    // Undo first join
    typeText("u")
    assertState("""
      ${c}One
      Two
      Three
      Four
    """.trimIndent())
  }

  @Test
  fun `test undo join with special whitespace handling`() {
    configureByText("""
      ${c}foo {
          bar
      }
    """.trimIndent())
    typeText("J")
    assertState("""
      foo {${c} bar
      }
    """.trimIndent())
    typeText("u")
    assertState("""
      ${c}foo {
          bar
      }
    """.trimIndent())
  }
}
