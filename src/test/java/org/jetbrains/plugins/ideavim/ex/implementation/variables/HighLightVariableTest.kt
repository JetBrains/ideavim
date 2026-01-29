/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.variables

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class HighLightVariableTest : VimTestCase() {

  @Test
  fun `test v hlsearch is initially false`() {
    configureByText(
      """
    |one two one
    |${c}three four three
    |five six five
    """.trimMargin()
    )
    enterCommand("set hlsearch")
    enterCommand("echo v:hlsearch")
    assertOutput("0")
  }

  @Test
  fun `test v hlsearch is true after successful search`() {
    configureByText(
      """
    |one two one
    |${c}three four three
    |five six five
    """.trimMargin()
    )
    enterCommand("set hlsearch")
    enterSearch("one")
    enterCommand("echo v:hlsearch")
    assertOutput("1")
  }

  @Test
  fun `test v hlsearch is false after search with no matches`() {
    configureByText(
      """
    |one two one
    |${c}three four three
    |five six five
    """.trimMargin()
    )
    enterCommand("set hlsearch")
    enterSearch("xyz")
    enterCommand("echo v:hlsearch")
    assertOutput("0")
  }

  @Test
  fun `test v hlsearch is false after nohlsearch command`() {
    configureByText(
      """
    |one two one
    |${c}three four three
    |five six five
    """.trimMargin()
    )
    enterCommand("set hlsearch")
    enterSearch("one")
    enterCommand("nohlsearch")
    enterCommand("echo v:hlsearch")
    assertOutput("0")
  }

  @Test
  fun `test v hlsearch is true after n command reuses previous search`() {
    configureByText(
      """
    |one two one
    |${c}three four three
    |five six five
    """.trimMargin()
    )
    enterCommand("set hlsearch")
    enterSearch("one")
    enterCommand("nohlsearch")
    typeText("n")
    enterCommand("echo v:hlsearch")
    assertOutput("1")
  }


}
