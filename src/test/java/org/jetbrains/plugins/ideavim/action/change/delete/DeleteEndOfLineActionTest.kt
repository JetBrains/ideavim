/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class DeleteEndOfLineActionTest : VimTestCase() {
  @Test
  fun `test delete on empty line`() {
    doTest(
      "D",
      """
                Lorem Ipsum
                $c
                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
      """.trimIndent(),
      """
                Lorem Ipsum
                $c
                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test undo after delete to end of line`() {
    configureByText("Hello ${c}world and more text")
    typeText("D")
    assertState("Hello$c ")
    typeText("u")
    assertState("Hello ${c}world and more text")
  }

  @Test
  fun `test undo after delete to end of line with count`() {
    configureByText(
      """
      First ${c}line with text
      Second line
      Third line
      Fourth line
    """.trimIndent()
    )
    typeText("2D")
    assertState(
      """
      First$c 
      Third line
      Fourth line
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      First ${c}line with text
      Second line
      Third line
      Fourth line
    """.trimIndent()
    )
  }

  @Test
  fun `test undo after delete to end of line at different positions`() {
    configureByText("abc${c}defghijk")
    typeText("D")
    assertState("ab${c}c")
    typeText("u")
    assertState("abc${c}defghijk")

    // Move to different position and delete again
    typeText("0")
    assertState("${c}abcdefghijk")
    typeText("D")
    assertState("$c")
    typeText("u")
    assertState("${c}abcdefghijk")
  }

  @Test
  fun `test multiple undo after sequential delete to end of line`() {
    configureByText(
      """
      ${c}First line
      Second line
      Third line
    """.trimIndent()
    )
    typeText("D")
    assertState(
      """
      $c
      Second line
      Third line
    """.trimIndent()
    )
    typeText("j")
    typeText("D")
    assertState(
      """
      
      $c
      Third line
    """.trimIndent()
    )

    // Undo second delete
    typeText("u")
    assertState(
      """
      
      ${c}Second line
      Third line
    """.trimIndent()
    )

    // Undo first delete
    typeText("u")
    assertState(
      """
      ${c}First line
      Second line
      Third line
    """.trimIndent()
    )
  }

  @Test
  fun `test undo after delete to end of line with oldundo`() {
    configureByText("Hello ${c}world and more text")
    try {
      enterCommand("set oldundo")
      typeText("D")
      assertState("Hello$c ")
      typeText("u")
      assertState("Hello ${c}world and more text")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after delete to end of line with count with oldundo`() {
    configureByText(
      """
      First ${c}line with text
      Second line
      Third line
      Fourth line
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("2D")
      assertState(
        """
      First$c 
      Third line
      Fourth line
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      First ${c}line with text
      Second line
      Third line
      Fourth line
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after delete to end of line at different positions with oldundo`() {
    configureByText("abc${c}defghijk")
    try {
      enterCommand("set oldundo")
      typeText("D")
      assertState("ab${c}c")
      typeText("u")
      assertState("abc${c}defghijk")

      // Move to different position and delete again
      typeText("0")
      assertState("${c}abcdefghijk")
      typeText("D")
      assertState("$c")
      typeText("u")
      assertState("${c}abcdefghijk")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test multiple undo after sequential delete to end of line with oldundo`() {
    configureByText(
      """
      ${c}First line
      Second line
      Third line
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("D")
      assertState(
        """
      $c
      Second line
      Third line
    """.trimIndent()
      )
      typeText("j")
      typeText("D")
      assertState(
        """
      
      $c
      Third line
    """.trimIndent()
      )

      // Undo second delete
      typeText("u")
      assertState(
        """
      
      ${c}Second line
      Third line
    """.trimIndent()
      )

      // Undo first delete
      typeText("u")
      assertState(
        """
      ${c}
      Second line
      Third line
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }
}
