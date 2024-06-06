/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class GetFunctionTest : VimTestCase() {
  @Test
  fun `test get with dictionary`() {
    configureByText("\n")
    typeText(commandToKeys("echo get({1: 'one', 2: 'two', 3: 'three'}, '2')"))
    assertExOutput("two")
  }

  @Test
  fun `test get by nonexistent key in dictionary`() {
    configureByText("\n")
    typeText(commandToKeys("echo get({1: 'one', 2: 'two', 3: 'three'}, '10')"))
    assertExOutput("0")
  }

  @Test
  fun `test get by nonexistent key in dictionary with default value`() {
    configureByText("\n")
    typeText(commandToKeys("echo get({1: 'one', 2: 'two', 3: 'three'}, '10', 'null')"))
    assertExOutput("null")
  }

  @Test
  fun `test get with list`() {
    configureByText("\n")
    typeText(commandToKeys("echo get(['one', 'two', 'three'], 1)"))
    assertExOutput("two")
  }

  @Test
  fun `test get by nonexistent index in list`() {
    configureByText("\n")
    typeText(commandToKeys("echo get(['one', 'two', 'three'], 10)"))
    assertExOutput("-1")
  }

  @Test
  fun `test get by nonexistent index in list with default value`() {
    configureByText("\n")
    typeText(commandToKeys("echo get(['one', 'two', 'three'], 10, 'null')"))
    assertExOutput("null")
  }
}
