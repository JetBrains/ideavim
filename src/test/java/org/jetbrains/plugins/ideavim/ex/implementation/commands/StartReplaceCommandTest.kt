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
 * Tests for `:startreplace`, which works like `R`, or like `$R` when a bang is given.
 *
 * See `:help :startreplace`.
 */
class StartReplaceCommandTest : VimTestCase() {

  @Test
  fun `test startreplace enters replace mode`() {
    configureByText("abc${c}def")
    enterCommand("startreplace")

    assertPluginError(false)
    assertMode(Mode.REPLACE)
  }

  @Test
  fun `test startr abbreviation is accepted`() {
    configureByText("abc${c}def")
    enterCommand("startr")

    assertPluginError(false)
    assertMode(Mode.REPLACE)
  }

  @Test
  fun `test startreplace overwrites from the caret like R`() {
    configureByText("abc${c}def")
    enterCommand("startreplace")
    typeText("XY")

    assertState("abcXY${c}f")
  }

  @Test
  fun `test startreplace leaves the caret where it was`() {
    configureByText("abc${c}def")
    enterCommand("startreplace")

    assertState("abc${c}def")
  }

  @Test
  fun `test backspace in startreplace restores the overwritten character`() {
    configureByText("abc${c}def")
    enterCommand("startreplace")
    typeText("XY<BS><BS>")

    assertState("abc${c}def")
  }

  @Test
  fun `test startreplace past the end of the line appends`() {
    configureByText("ab${c}c")
    enterCommand("startreplace")
    typeText("XYZ")

    assertState("abXYZ${c}")
  }

  @Test
  fun `test startreplace with bang overwrites the last character like dollar R`() {
    configureByText("ab${c}cdef")
    enterCommand("startreplace!")
    typeText("X")

    assertPluginError(false)
    assertMode(Mode.REPLACE)
    assertState("abcdeX${c}")
  }

  @Test
  fun `test startreplace with bang stops at the end of the current line only`() {
    configureByText(
      """
        ${c}abc
        def
      """.trimIndent(),
    )
    enterCommand("startreplace!")
    typeText("X")

    assertState(
      """
        abX${c}
        def
      """.trimIndent(),
    )
  }

  @Test
  fun `test startreplace with bang keeps typing past the end of the line`() {
    configureByText("${c}abc")
    enterCommand("startreplace!")
    typeText("XY")

    assertState("abXY${c}")
  }

  @Test
  fun `test startreplace with bang on empty line`() {
    configureByText(
      """
        ${c}
        next
      """.trimIndent(),
    )
    enterCommand("startreplace!")
    typeText("X")

    assertState(
      """
        X${c}
        next
      """.trimIndent(),
    )
  }

  @Test
  fun `test startreplace with bang appends when virtualedit is onemore`() {
    configureByText("ab${c}cdef")
    enterCommand("set virtualedit=onemore")
    enterCommand("startreplace!")
    typeText("X")

    assertState("abcdefX${c}")
  }

  @Test
  fun `test startreplace with multiple carets`() {
    configureByText(
      """
        ab${c}cd
        ef${c}gh
      """.trimIndent(),
    )
    enterCommand("startreplace")
    typeText("X")

    assertState(
      """
        abX${c}d
        efX${c}h
      """.trimIndent(),
    )
  }

  @Test
  fun `test startreplace with bang moves every caret to its own line end`() {
    configureByText(
      """
        ${c}abc
        ${c}defgh
      """.trimIndent(),
    )
    enterCommand("startreplace!")
    typeText("X")

    assertState(
      """
        abX${c}
        defgX${c}
      """.trimIndent(),
    )
  }

  @Test
  fun `test trailing characters raises an error`() {
    configureByText("abc${c}def")
    enterCommand("startreplace foo")

    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: foo")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test range raises an error`() {
    configureByText("abc${c}def")
    enterCommand("1startreplace")

    assertPluginError(true)
    assertPluginErrorMessage("E481: No range allowed")
    assertMode(Mode.NORMAL())
  }
}
