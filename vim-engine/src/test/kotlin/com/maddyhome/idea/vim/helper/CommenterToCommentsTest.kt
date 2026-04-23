/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CommenterToCommentsTest {

  private fun markers(line: String? = null, block: String? = null, suffix: String? = null) =
    CommenterMarkers(linePrefix = line, blockPrefix = block, blockSuffix = suffix)

  @Test
  fun `returns null when no markers are present`() {
    assertNull(CommenterToComments.derive(markers()))
  }

  @Test
  fun `line-only commenter maps to colon prefix entry`() {
    assertEquals(":#", CommenterToComments.derive(markers(line = "#")))
  }

  @Test
  fun `symmetric block maps last char of prefix as middle`() {
    assertEquals("s1:/*,mb:*,ex:*/", CommenterToComments.derive(markers(block = "/*", suffix = "*/")))
  }

  @Test
  fun `combined line and block produces both entries with block first`() {
    assertEquals(
      "s1:/*,mb:*,ex:*/,://",
      CommenterToComments.derive(markers(line = "//", block = "/*", suffix = "*/")),
    )
  }

  @Test
  fun `block with only prefix but no suffix is skipped`() {
    assertEquals(":#", CommenterToComments.derive(markers(line = "#", block = "(*")))
  }

  @Test
  fun `block with only suffix but no prefix is skipped`() {
    assertEquals(":#", CommenterToComments.derive(markers(line = "#", suffix = "-}")))
  }

  @Test
  fun `asymmetric block like HTML uses space as middle marker`() {
    assertEquals(
      "s1:<!--,mb: ,ex:-->",
      CommenterToComments.derive(markers(block = "<!--", suffix = "-->")),
    )
  }

  @Test
  fun `asymmetric block like Lua degrades safely to space middle`() {
    assertEquals(
      "s1:--[[,mb: ,ex:]]",
      CommenterToComments.derive(markers(block = "--[[", suffix = "]]")),
    )
  }

  @Test
  fun `empty-string markers are treated as absent`() {
    assertNull(CommenterToComments.derive(markers(line = "", block = "", suffix = "")))
  }

  @Test
  fun `trailing whitespace on line prefix is trimmed`() {
    assertEquals(":#", CommenterToComments.derive(markers(line = "# ")))
  }
}
