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
import kotlin.test.assertIs
import kotlin.test.assertNull

class CommandLineParserTest {

  @Test
  fun `test parse simple command with argument`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("edit foo.txt"))
    assertEquals("edit", result.commandName)
    assertEquals("foo.txt", result.argumentPrefix)
    assertEquals(5, result.completionStart)
  }

  @Test
  fun `test parse abbreviated command`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("e foo.txt"))
    assertEquals("e", result.commandName)
    assertEquals("foo.txt", result.argumentPrefix)
    assertEquals(2, result.completionStart)
  }

  @Test
  fun `test parse command with path argument`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("edit src/main/"))
    assertEquals("edit", result.commandName)
    assertEquals("src/main/", result.argumentPrefix)
    assertEquals(5, result.completionStart)
  }

  @Test
  fun `test parse command with home path`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("e ~/.vim"))
    assertEquals("e", result.commandName)
    assertEquals("~/.vim", result.argumentPrefix)
    assertEquals(2, result.completionStart)
  }

  @Test
  fun `test parse command with empty argument`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("edit "))
    assertEquals("edit", result.commandName)
    assertEquals("", result.argumentPrefix)
    assertEquals(5, result.completionStart)
  }

  @Test
  fun `test parse command with multiple spaces before argument`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("edit   foo"))
    assertEquals("edit", result.commandName)
    assertEquals("foo", result.argumentPrefix)
    assertEquals(7, result.completionStart)
  }

  @Test
  fun `test parse with leading whitespace`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("  edit foo"))
    assertEquals("edit", result.commandName)
    assertEquals("foo", result.argumentPrefix)
    assertEquals(7, result.completionStart)
  }

  @Test
  fun `test parse bang command`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("edit! foo.txt"))
    assertEquals("edit!", result.commandName)
    assertEquals("foo.txt", result.argumentPrefix)
    assertEquals(6, result.completionStart)
  }

  @Test
  fun `test parse write command`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("w output.txt"))
    assertEquals("w", result.commandName)
    assertEquals("output.txt", result.argumentPrefix)
    assertEquals(2, result.completionStart)
  }

  @Test
  fun `test parse source command with path`() {
    val result = assertIs<ArgumentCompletionContext>(parseCommandLineForCompletion("source ~/.ideavimrc"))
    assertEquals("source", result.commandName)
    assertEquals("~/.ideavimrc", result.argumentPrefix)
    assertEquals(7, result.completionStart)
  }

  @Test
  fun `test parse command name only returns command-name context`() {
    val result = assertIs<CommandNameCompletionContext>(parseCommandLineForCompletion("edit"))
    assertEquals("edit", result.prefix)
    assertEquals(0, result.completionStart)
  }

  @Test
  fun `test parse single letter abbreviation returns command-name context`() {
    val result = assertIs<CommandNameCompletionContext>(parseCommandLineForCompletion("e"))
    assertEquals("e", result.prefix)
    assertEquals(0, result.completionStart)
  }

  @Test
  fun `test parse partial abbreviation returns command-name context`() {
    val result = assertIs<CommandNameCompletionContext>(parseCommandLineForCompletion("vs"))
    assertEquals("vs", result.prefix)
    assertEquals(0, result.completionStart)
  }

  @Test
  fun `test parse command name with leading whitespace`() {
    val result = assertIs<CommandNameCompletionContext>(parseCommandLineForCompletion("  vs"))
    assertEquals("vs", result.prefix)
    assertEquals(2, result.completionStart)
  }

  @Test
  fun `test parse mixed case command name preserved`() {
    val result = assertIs<CommandNameCompletionContext>(parseCommandLineForCompletion("tabN"))
    assertEquals("tabN", result.prefix)
    assertEquals(0, result.completionStart)
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
  fun `test parse returns null for non-letter start`() {
    assertNull(parseCommandLineForCompletion("123"))
  }

  @Test
  fun `test parse returns null for bang form without space`() {
    assertNull(parseCommandLineForCompletion("vs!"))
    assertNull(parseCommandLineForCompletion("edit!"))
  }

  @Test
  fun `test parse returns null for command followed by non-space chars`() {
    assertNull(parseCommandLineForCompletion("foo123"))
  }
}
