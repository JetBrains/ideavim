/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for `:startinsert`, which works like `i`, or like `A` when a bang is given.
 *
 * See `:help :startinsert`.
 */
class StartInsertCommandTest : VimTestCase() {

  @Test
  fun `test startinsert enters insert mode`() {
    configureByText("abc${c}def")
    enterCommand("startinsert")

    assertPluginError(false)
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test star abbreviation is accepted`() {
    configureByText("abc${c}def")
    enterCommand("star")

    assertPluginError(false)
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test startinsert inserts before the caret like i`() {
    configureByText("abc${c}def")
    enterCommand("startinsert")
    typeText("X")

    assertState("abcX${c}def")
  }

  @Test
  fun `test startinsert leaves the caret where it was`() {
    configureByText("abc${c}def")
    enterCommand("startinsert")

    assertState("abc${c}def")
  }

  @Test
  fun `test startinsert on empty line`() {
    configureByText("")
    enterCommand("startinsert")
    typeText("X")

    assertState("X${c}")
  }

  @Test
  fun `test startinsert with bang appends at the end of the line like A`() {
    configureByText("abc${c}def")
    enterCommand("startinsert!")
    typeText("X")

    assertPluginError(false)
    assertMode(Mode.INSERT)
    assertState("abcdefX${c}")
  }

  @Test
  fun `test startinsert with bang from the start of the line`() {
    configureByText("${c}hello world")
    enterCommand("startinsert!")
    typeText("X")

    assertState("hello worldX${c}")
  }

  @Test
  fun `test startinsert with bang stops at the end of the current line only`() {
    configureByText(
      """
        ab${c}c
        def
      """.trimIndent(),
    )
    enterCommand("startinsert!")
    typeText("X")

    assertState(
      """
        abcX${c}
        def
      """.trimIndent(),
    )
  }

  @Test
  fun `test startinsert with bang on empty line`() {
    configureByText(
      """
        ${c}
        next
      """.trimIndent(),
    )
    enterCommand("startinsert!")
    typeText("X")

    assertState(
      """
        X${c}
        next
      """.trimIndent(),
    )
  }

  @Test
  fun `test startinsert with multiple carets`() {
    configureByText(
      """
        ab${c}c
        de${c}f
      """.trimIndent(),
    )
    enterCommand("startinsert")
    typeText("X")

    assertState(
      """
        abX${c}c
        deX${c}f
      """.trimIndent(),
    )
  }

  @Test
  fun `test startinsert with bang moves every caret to its own line end`() {
    configureByText(
      """
        ${c}abc
        ${c}defgh
      """.trimIndent(),
    )
    enterCommand("startinsert!")
    typeText("X")

    assertState(
      """
        abcX${c}
        defghX${c}
      """.trimIndent(),
    )
  }

  @Test
  fun `test trailing characters raises an error`() {
    configureByText("abc${c}def")
    enterCommand("startinsert foo")

    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: foo")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test range raises an error`() {
    configureByText("abc${c}def")
    enterCommand("1startinsert")

    assertPluginError(true)
    assertPluginErrorMessage("E481: No range allowed")
    assertMode(Mode.NORMAL())
  }
}
