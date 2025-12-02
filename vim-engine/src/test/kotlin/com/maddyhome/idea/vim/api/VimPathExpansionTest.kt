/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VimPathExpansionTest {
  private val pathExpansion = VimPathExpansionImpl()
  private val home = System.getProperty("user.home")

  @Test
  fun `test tilde expansion to home directory`() {
    assertEquals(home, pathExpansion.expandPath("~"))
  }

  @Test
  fun `test tilde slash expansion`() {
    assertEquals("$home/test", pathExpansion.expandPath("~/test"))
  }

  @Test
  fun `test tilde backslash expansion on Windows`() {
    assertEquals("$home\\test", pathExpansion.expandPath("~\\test"))
  }

  @Test
  fun `test tilde in middle of path not expanded`() {
    assertEquals("/path/~test", pathExpansion.expandPath("/path/~test"))
  }

  @Test
  fun `test tilde with non-separator char not expanded`() {
    // ~.vimrc should stay as-is (Vim behavior)
    assertEquals("~.vimrc", pathExpansion.expandPath("~.vimrc"))
    assertEquals("~test", pathExpansion.expandPath("~test"))
  }

  @Test
  fun `test tilde at end of path not expanded`() {
    assertEquals("/path/~", pathExpansion.expandPath("/path/~"))
  }

  @Test
  fun `test multiple tildes - only first expanded`() {
    assertEquals("$home/~", pathExpansion.expandPath("~/~"))
  }

  @Test
  fun `test environment variable expansion with dollar sign`() {
    // Set up a test environment variable
    val testValue = System.getenv("PATH") // Use PATH as it's always available
    val expanded = pathExpansion.expandPath("${'$'}PATH/test")
    assertEquals("$testValue/test", expanded)
  }

  @Test
  fun `test environment variable expansion with braces`() {
    val testValue = System.getenv("PATH")
    val expanded = pathExpansion.expandPath("${'$'}{PATH}/test")
    assertEquals("$testValue/test", expanded)
  }

  @Test
  fun `test non-existent environment variable expands to empty string`() {
    val expanded = pathExpansion.expandPath("${'$'}NONEXISTENT_VAR_12345/test")
    assertEquals("/test", expanded)
  }

  @Test
  fun `test non-existent environment variable with braces expands to empty string`() {
    val expanded = pathExpansion.expandPath("${'$'}{NONEXISTENT_VAR_12345}/test")
    assertEquals("/test", expanded)
  }

  @Test
  fun `test escaped dollar sign becomes literal`() {
    val expanded = pathExpansion.expandPath("\\${'$'}PATH/test")
    assertEquals("${'$'}PATH/test", expanded)
  }

  @Test
  fun `test escaped dollar sign with braces becomes literal`() {
    val expanded = pathExpansion.expandPath("\\${'$'}{PATH}/test")
    assertEquals("${'$'}{PATH}/test", expanded)
  }

  @Test
  fun `test multiple environment variables in path`() {
    val path = System.getenv("PATH")
    val home = System.getenv("HOME") ?: System.getProperty("user.home")
    val expanded = pathExpansion.expandPath("${'$'}PATH:${'$'}HOME")
    assertEquals("$path:$home", expanded)
  }

  @Test
  fun `test mixed tilde and environment variable expansion`() {
    val path = System.getenv("PATH")
    val expanded = pathExpansion.expandPath("~/${'$'}PATH/test")
    assertEquals("$home/$path/test", expanded)
  }

  @Test
  fun `test no expansion needed`() {
    val path = "/absolute/path/to/file"
    assertEquals(path, pathExpansion.expandPath(path))
  }

  @Test
  fun `test relative path no expansion`() {
    val path = "relative/path/to/file"
    assertEquals(path, pathExpansion.expandPath(path))
  }

  @Test
  fun `test environment variable at end of path`() {
    val path = System.getenv("PATH")
    val expanded = pathExpansion.expandPath("/prefix/${'$'}PATH")
    assertEquals("/prefix/$path", expanded)
  }

  @Test
  fun `test environment variable names with underscores`() {
    // Many systems have USER or _USER variables
    val testVar = System.getenv().keys.find { it.contains("_") }
    if (testVar != null) {
      val value = System.getenv(testVar)
      val expanded = pathExpansion.expandPath("${'$'}$testVar")
      assertEquals(value, expanded)
    }
  }

  @Test
  fun `test empty path returns empty`() {
    assertEquals("", pathExpansion.expandPath(""))
  }

  @Test
  fun `test only dollar sign left unchanged`() {
    assertEquals("$", pathExpansion.expandPath("$"))
  }

  @Test
  fun `test dollar sign without valid variable name left unchanged`() {
    // $ followed by a digit or special char is not a valid variable name
    assertEquals("${'$'}123", pathExpansion.expandPath("${'$'}123"))
    assertEquals("${'$'}@test", pathExpansion.expandPath("${'$'}@test"))
  }

  // Tests for expandForOption (option expansion mode)

  @Test
  fun `test option expansion - non-existent variable left as-is`() {
    val value = "/usr/${'$'}NONEXISTENT_VAR_12345/test"
    assertEquals(value, pathExpansion.expandForOption(value))
  }

  @Test
  fun `test option expansion - existing variable expanded`() {
    val pathValue = System.getenv("PATH")
    val expanded = pathExpansion.expandForOption("/usr/${'$'}PATH/test")
    assertEquals("/usr/$pathValue/test", expanded)
  }

  @Test
  fun `test option expansion - mixed existing and non-existent`() {
    val homeValue = System.getenv("HOME") ?: System.getProperty("user.home")
    val expanded = pathExpansion.expandForOption("/usr/${'$'}INCLUDE,${'$'}HOME/include")
    assertEquals("/usr/${'$'}INCLUDE,$homeValue/include", expanded)
  }

  @Test
  fun `test option expansion - tilde expansion still works`() {
    val expanded = pathExpansion.expandForOption("~/${'$'}NONEXISTENT/test")
    assertEquals("$home/${'$'}NONEXISTENT/test", expanded)
  }
}
