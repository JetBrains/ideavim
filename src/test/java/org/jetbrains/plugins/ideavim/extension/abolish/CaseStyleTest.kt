/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.abolish

import com.maddyhome.idea.vim.extension.abolish.CaseStyle
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CaseStyleTest {

  @Test
  fun `snake_case joins atoms with underscores in lowercase`() {
    assertEquals("hello_world_again", CaseStyle.SNAKE.join(listOf("Hello", "world", "AGAIN")))
  }

  @Test
  fun `UPPER_SNAKE joins atoms with underscores in uppercase`() {
    assertEquals("HELLO_WORLD", CaseStyle.UPPER_SNAKE.join(listOf("hello", "world")))
  }

  @Test
  fun `kebab-case joins atoms with dashes in lowercase`() {
    assertEquals("hello-world", CaseStyle.KEBAB.join(listOf("Hello", "World")))
  }

  @Test
  fun `dot-case joins atoms with dots in lowercase`() {
    assertEquals("hello.world", CaseStyle.DOT.join(listOf("Hello", "World")))
  }

  @Test
  fun `space case joins atoms with spaces in lowercase`() {
    assertEquals("hello world", CaseStyle.SPACE.join(listOf("Hello", "World")))
  }

  @Test
  fun `title case capitalises every atom and joins with spaces`() {
    assertEquals("Hello World", CaseStyle.TITLE.join(listOf("hello", "WORLD")))
  }

  @Test
  fun `camelCase lowercases the first atom and capitalises the rest`() {
    assertEquals("helloWorldAgain", CaseStyle.CAMEL.join(listOf("hello", "world", "again")))
  }

  @Test
  fun `PascalCase capitalises every atom and concatenates`() {
    assertEquals("HelloWorld", CaseStyle.PASCAL.join(listOf("hello", "world")))
  }

  @Test
  fun `joining no atoms yields empty string`() {
    assertEquals("", CaseStyle.SNAKE.join(emptyList()))
  }

  @Test
  fun `recase converts camelCase to snake_case`() {
    assertEquals("hello_world", CaseStyle.SNAKE.recase("helloWorld"))
  }

  @Test
  fun `recase converts snake_case to PascalCase`() {
    assertEquals("HelloWorld", CaseStyle.PASCAL.recase("hello_world"))
  }

  @Test
  fun `recase converts kebab to UPPER_SNAKE`() {
    assertEquals("HELLO_WORLD", CaseStyle.UPPER_SNAKE.recase("hello-world"))
  }
}
