/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SublistExpressionTest : VimTestCase() {

  @Test
  fun `test strung sublist`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[0:1]"))
    assertExOutput("ab")
  }

  @Test
  fun `test negative index with sting`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[-1]"))
    assertExOutput("")
  }

  @Test
  fun `test index greater than size with string`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[1000]"))
    assertExOutput("")
  }

  // TODO: This (and above) are indexed expressions, not sublist expressions
  @Test
  fun `test negative index with list`() {
    configureByText("\n")
    assertCommandOutput("echo [1, 2][-1]", "2")
  }

  @Test
  fun `test index greater than size with list`() {
    configureByText("\n")
    typeText(commandToKeys("echo [1, 2][1000]"))
    assertPluginErrorMessage("E684: List index out of range: 1000")
  }

  @Test
  fun `test list with correct index`() {
    configureByText("\n")
    typeText(commandToKeys("echo [1, 2][1]"))
    assertExOutput("2")
  }

  @Test
  fun `test negative first index`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[-1:]"))
    assertExOutput("c")
  }

  @Test
  fun `test negative last index`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[0:-2]"))
    assertExOutput("ab")
  }

  @Test
  fun `test negative last index2`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[0:-1]"))
    assertExOutput("abc")
  }

  @Test
  fun `test last index bigger sting size`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[1:10000]"))
    assertExOutput("bc")
  }

  @Test
  fun `test both indexes bigger sting size`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[100:10000]"))
    assertExOutput("")
  }

  @Test
  fun `test first index is bigger than second`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[100:10]"))
    assertExOutput("")
  }
}
