/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import org.jetbrains.plugins.ideavim.VimTestCase

class SublistExpressionTest : VimTestCase() {

  fun `test strung sublist`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[0:1]"))
    assertExOutput("ab\n")
  }

  fun `test negative index with sting`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[-1]"))
    assertExOutput("\n")
  }

  fun `test index greater than size with string`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[1000]"))
    assertExOutput("\n")
  }

  fun `test negative index with list`() {
    configureByText("\n")
    typeText(commandToKeys("echo [1, 2][-1]"))
    assertPluginErrorMessageContains("E684: list index out of range: -1")
  }

  fun `test index greater than size with list`() {
    configureByText("\n")
    typeText(commandToKeys("echo [1, 2][1000]"))
    assertPluginErrorMessageContains("E684: list index out of range: 1000")
  }

  fun `test list with correct index`() {
    configureByText("\n")
    typeText(commandToKeys("echo [1, 2][1]"))
    assertExOutput("2\n")
  }

  fun `test negative first index`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[-1:]"))
    assertExOutput("c\n")
  }

  fun `test negative last index`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[0:-2]"))
    assertExOutput("ab\n")
  }

  fun `test negative last index2`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[0:-1]"))
    assertExOutput("abc\n")
  }

  fun `test last index bigger sting size`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[1:10000]"))
    assertExOutput("bc\n")
  }

  fun `test both indexes bigger sting size`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[100:10000]"))
    assertExOutput("\n")
  }

  fun `test first index is bigger than second`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[100:10]"))
    assertExOutput("\n")
  }
}
