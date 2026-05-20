/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.abolish

import com.maddyhome.idea.vim.extension.abolish.splitIntoAtoms
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WordAtomsTest {

  @Test
  fun `single lowercase word is one atom`() {
    assertEquals(listOf("hello"), splitIntoAtoms("hello"))
  }

  @Test
  fun `snake_case splits on underscores`() {
    assertEquals(listOf("hello", "world"), splitIntoAtoms("hello_world"))
  }

  @Test
  fun `kebab-case splits on dashes`() {
    assertEquals(listOf("hello", "world"), splitIntoAtoms("hello-world"))
  }

  @Test
  fun `dot-case splits on dots`() {
    assertEquals(listOf("hello", "world"), splitIntoAtoms("hello.world"))
  }

  @Test
  fun `space case splits on spaces`() {
    assertEquals(listOf("hello", "world"), splitIntoAtoms("hello world"))
  }

  @Test
  fun `camelCase splits on case boundaries`() {
    assertEquals(listOf("hello", "World"), splitIntoAtoms("helloWorld"))
  }

  @Test
  fun `PascalCase splits on case boundaries`() {
    assertEquals(listOf("Hello", "World"), splitIntoAtoms("HelloWorld"))
  }

  @Test
  fun `acronym is kept together`() {
    assertEquals(listOf("HTTP", "Request"), splitIntoAtoms("HTTPRequest"))
  }

  @Test
  fun `UPPER_SNAKE splits on underscores`() {
    assertEquals(listOf("HELLO", "WORLD"), splitIntoAtoms("HELLO_WORLD"))
  }

  @Test
  fun `empty string is no atoms`() {
    assertEquals(emptyList(), splitIntoAtoms(""))
  }
}
