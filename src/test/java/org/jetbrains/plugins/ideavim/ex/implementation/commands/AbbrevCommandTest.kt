/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.action.ex.VimExTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class AbbrevCommandTest : VimExTestCase() {

  @AfterEach
  fun clearAbbreviations() {
    enterCommand("abclear")
  }

  @Test
  fun `iabbrev with full-id lhs expands on whitespace trigger`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    typeText("i", "foo ")
    assertState("bar \n")
  }

  @Test
  fun `iabbrev with end-id lhs expands on whitespace trigger`() {
    configureByText("${c}\n")
    enterCommand("iabbrev #i include")
    typeText("i", "#i ")
    assertState("include \n")
  }

  @Test
  fun `iabbrev with non-id lhs expands on whitespace trigger`() {
    configureByText("${c}\n")
    enterCommand("iabbrev def# define")
    typeText("i", "def# ")
    assertState("define \n")
  }

  @Test
  fun `iabbrev rejects mixed lhs that ends in keyword with non-keyword char inside`() {
    configureByText("\n")
    enterCommand("iabbrev f#i value")
    assertPluginError(true)
  }

  @Test
  fun `iunabbrev makes a previously defined abbreviation stop expanding`() {
    configureByText("${c}\n")
    enterCommand("iabbrev teh the")
    enterCommand("iunabbrev teh")
    typeText("i", "teh ")
    assertState("teh \n")
  }

  @Test
  fun `iabclear makes all previously defined insert abbreviations stop expanding`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    enterCommand("iabbrev teh the")
    enterCommand("iabclear")
    typeText("i", "foo teh ")
    assertState("foo teh \n")
  }

  @Test
  fun `cabbrev expands its lhs in cmdline mode on whitespace trigger`() {
    enterCommand("cabbrev myabbrev myexpansion")
    typeText(":myabbrev ")
    assertExText("myexpansion ")
  }

  @Test
  fun `iabbrev does not expand after cursor moved with arrows in same insert session`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    typeText("i", "foo<Left><Right> ")
    assertState("foo \n")
  }

  @Test
  fun `cabbrev does not expand after cursor moved with arrows in cmdline`() {
    enterCommand("cabbrev myabbrev myexpansion")
    typeText(":myabbrev<Left><Right> ")
    assertExText("myabbrev ")
  }

  @Test
  fun `iabbrev still does not expand after typing more chars when cursor was previously moved`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    typeText("i", "foo<Left><Right>x ")
    assertState("foox \n")
  }

  @Test
  fun `iabbrev still expands after backspace correction within insert session`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    typeText("i", "fox<BS>o ")
    assertState("bar \n")
  }

  @Test
  fun `iabbrev expansion is re-enabled by re-entering insert mode`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    typeText("i", "foo<Left><Right><Esc>")
    typeText("o", "foo ")
    assertState("foo\nbar \n")
  }

  @Test
  fun `cabbrev expansion is re-enabled when cmdline is re-opened`() {
    enterCommand("cabbrev myabbrev myexpansion")
    typeText(":myabbrev<Left><Right><Esc>")
    typeText(":myabbrev ")
    assertExText("myexpansion ")
  }
}
