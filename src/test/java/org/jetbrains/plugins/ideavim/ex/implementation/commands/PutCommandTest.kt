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

/**
 * @author Alex Plate
 */
class PutCommandTest : VimTestCase() {
  // VIM-550 |:put|
  @Test
  fun `test put creates new line`() {
    configureByText("Test\n" + "Hello <caret>World!\n")
    typeText(injector.parser.parseKeys("\"ayw"))
    typeText(commandToKeys("put a"))
    assertState(
      "Test\n" +
        "Hello World!\n" +
        "<caret>World\n",
    )
  }

  // VIM-551 |:put|
  @Test
  fun `test put default`() {
    configureByText("<caret>Hello World!\n")
    typeText(injector.parser.parseKeys("yw"))
    typeText(commandToKeys("put"))
    assertState("Hello World!\n" + "<caret>Hello \n")
  }

  @Test
  fun `test put and undo`() {
    configureByText(
      """
      Line 1
      Line ${c}2
      Line 3
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("yy"))
    enterCommand("put")
    assertState(
      """
      Line 1
      Line 2
      ${c}Line 2
      Line 3
      """.trimIndent()
    )

    typeText("u")
    assertState(
      """
      Line 1
      Line ${c}2
      Line 3
      """.trimIndent()
    )
  }

  @Test
  fun `test put from register and undo`() {
    configureByText(
      """
      First line
      Second ${c}line
      Third line
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("\"ayy"))
    enterCommand("put a")
    assertState(
      """
      First line
      Second line
      ${c}Second line
      Third line
      """.trimIndent()
    )

    typeText("u")
    assertState(
      """
      First line
      Second ${c}line
      Third line
      """.trimIndent()
    )
  }

  @Test
  fun `test put with line number and undo`() {
    configureByText(
      """
      Line 1
      Line ${c}2
      Line 3
      Line 4
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("yy"))
    enterCommand("1put")
    assertState(
      """
      Line 1
      ${c}Line 2
      Line 2
      Line 3
      Line 4
      """.trimIndent()
    )

    typeText("u")
    assertState(
      """
      Line 1
      Line ${c}2
      Line 3
      Line 4
      """.trimIndent()
    )
  }

  @Test
  fun `test put and undo with oldundo`() {
    configureByText(
      """
      Line 1
      Line ${c}2
      Line 3
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("yy"))
    try {
      enterCommand("set oldundo")
      enterCommand("put")
      assertState(
        """
      Line 1
      Line 2
      ${c}Line 2
      Line 3
      """.trimIndent()
      )

      typeText("u")
      assertState(
        """
      Line 1
      Line ${c}2
      Line 3
      """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test put from register and undo with oldundo`() {
    configureByText(
      """
      First line
      Second ${c}line
      Third line
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("\"ayy"))
    try {
      enterCommand("set oldundo")
      enterCommand("put a")
      assertState(
        """
      First line
      Second line
      ${c}Second line
      Third line
      """.trimIndent()
      )

      typeText("u")
      assertState(
        """
      First line
      Second ${c}line
      Third line
      """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test put with line number and undo with oldundo`() {
    configureByText(
      """
      Line 1
      Line ${c}2
      Line 3
      Line 4
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("yy"))
    try {
      enterCommand("set oldundo")
      enterCommand("1put")
      assertState(
        """
      Line 1
      ${c}Line 2
      Line 2
      Line 3
      Line 4
      """.trimIndent()
      )

      typeText("u")
      assertState(
        """
      Line 1
      Line ${c}2
      Line 3
      Line 4
      """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }
}
