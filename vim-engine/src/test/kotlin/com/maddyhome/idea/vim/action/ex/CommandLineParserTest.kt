/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CommandLineParserTest {

  @Test
  fun `test parse simple command with argument`() {
    val result = parseCommandLineForCompletion("edit foo.txt")
    assertNotNull(result)
    assertEquals("edit", result.commandName)
    assertEquals("foo.txt", result.argumentPrefix)
    assertEquals(5, result.completionStart)
  }

  @Test
  fun `test parse abbreviated command`() {
    val result = parseCommandLineForCompletion("e foo.txt")
    assertNotNull(result)
    assertEquals("e", result.commandName)
    assertEquals("foo.txt", result.argumentPrefix)
    assertEquals(2, result.completionStart)
  }

  @Test
  fun `test parse command with path argument`() {
    val result = parseCommandLineForCompletion("edit src/main/")
    assertNotNull(result)
    assertEquals("edit", result.commandName)
    assertEquals("src/main/", result.argumentPrefix)
    assertEquals(5, result.completionStart)
  }

  @Test
  fun `test parse command with home path`() {
    val result = parseCommandLineForCompletion("e ~/.vim")
    assertNotNull(result)
    assertEquals("e", result.commandName)
    assertEquals("~/.vim", result.argumentPrefix)
    assertEquals(2, result.completionStart)
  }

  @Test
  fun `test parse command with empty argument`() {
    val result = parseCommandLineForCompletion("edit ")
    assertNotNull(result)
    assertEquals("edit", result.commandName)
    assertEquals("", result.argumentPrefix)
    assertEquals(5, result.completionStart)
  }

  @Test
  fun `test parse command with multiple spaces before argument`() {
    val result = parseCommandLineForCompletion("edit   foo")
    assertNotNull(result)
    assertEquals("edit", result.commandName)
    assertEquals("foo", result.argumentPrefix)
    assertEquals(7, result.completionStart)
  }

  @Test
  fun `test parse returns null for empty text`() {
    assertNull(parseCommandLineForCompletion(""))
  }

  @Test
  fun `test parse returns null for whitespace only`() {
    assertNull(parseCommandLineForCompletion("   "))
  }

  @Test
  fun `test parse returns null for command without space`() {
    assertNull(parseCommandLineForCompletion("edit"))
  }

  @Test
  fun `test parse returns null for command abbreviation without space`() {
    assertNull(parseCommandLineForCompletion("e"))
  }

  @Test
  fun `test parse returns null for non-letter start`() {
    assertNull(parseCommandLineForCompletion("123"))
  }

  @Test
  fun `test parse with leading whitespace`() {
    val result = parseCommandLineForCompletion("  edit foo")
    assertNotNull(result)
    assertEquals("edit", result.commandName)
    assertEquals("foo", result.argumentPrefix)
    assertEquals(7, result.completionStart)
  }

  @Test
  fun `test parse bang command`() {
    val result = parseCommandLineForCompletion("edit! foo.txt")
    assertNotNull(result)
    assertEquals("edit!", result.commandName)
    assertEquals("foo.txt", result.argumentPrefix)
    assertEquals(6, result.completionStart)
  }

  @Test
  fun `test parse write command`() {
    val result = parseCommandLineForCompletion("w output.txt")
    assertNotNull(result)
    assertEquals("w", result.commandName)
    assertEquals("output.txt", result.argumentPrefix)
    assertEquals(2, result.completionStart)
  }

  @Test
  fun `test parse source command with path`() {
    val result = parseCommandLineForCompletion("source ~/.ideavimrc")
    assertNotNull(result)
    assertEquals("source", result.commandName)
    assertEquals("~/.ideavimrc", result.argumentPrefix)
    assertEquals(7, result.completionStart)
  }
}
