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

class DictionaryElementByKeyTest : VimTestCase() {

  @Test
  fun `test get element by key`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'a': 42} | echo dict.a"))
    assertExOutput("42")
  }

  @Test
  fun `test get element from inner dictionary`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'a': 42, 'b' : {'c': 'oh, hi Mark'}} | echo dict.b.c"))
    assertExOutput("oh, hi Mark")
  }

  @Test
  fun `test get element by key with minus`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'first-key': 42, 'second-key' : {'third-key': 'oh, hi Mark'}} | echo dict.first-key"))
    assertExOutput("42")
  }

  @Test
  fun `test get element from inner dictionary by keys with minuses`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'first-key': 42, 'second-key' : {'third-key': 'oh, hi Mark'}} | echo dict.second-key.third-key"))
    assertExOutput("oh, hi Mark")
  }

  @Test
  fun `test get element from inner list by index`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'list': [42]}"))
    typeText(commandToKeys("echo dict.list[0]"))
    assertExOutput("42")
  }

  @Test
  fun `test get element from inner list by index multiple times`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'list': [{'key': [42]}]}"))
    typeText(commandToKeys("echo dict.list[0].key[0]"))
    assertExOutput("42")
  }
}
