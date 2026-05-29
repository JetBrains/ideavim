/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.plugins.ideavim.action.ex.VimExTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AbbrevCommandTest : VimExTestCase() {

  // Both @BeforeEach and @AfterEach run a full cmdline reset: leftover ex-entry state from a
  // preceding test can suppress the @AfterEach `abclear`, so each test must defensively isolate
  // itself on entry as well as clean up on exit.
  @BeforeEach
  fun clearAbbreviationsBeforeTest() = resetCmdlineAndClearAbbreviations()

  @AfterEach
  fun clearAbbreviationsAfterTest() = resetCmdlineAndClearAbbreviations()

  private fun resetCmdlineAndClearAbbreviations() {
    deactivateExEntry()
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

  @Test
  fun `iabbrev with buffer modifier expands in the current buffer`() {
    configureByText("${c}\n")
    enterCommand("iabbrev <buffer> foo bar")
    typeText("i", "foo ")
    assertState("bar \n")
  }

  @Test
  fun `iabbrev with buffer modifier does not expand in a different buffer`() {
    configureByText("${c}\n")
    enterCommand("iabbrev <buffer> foo bar")
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile("other.txt", "\n"))
    }
    typeText("0")
    typeText("i", "foo ")
    assertState("foo \n")
  }

  @Test
  fun `iabbrev with buffer modifier takes precedence over global abbreviation with same lhs`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo global")
    enterCommand("iabbrev <buffer> foo local")
    typeText("i", "foo ")
    assertState("local \n")
  }

  @Test
  fun `iunabbrev with buffer modifier removes only the buffer-local abbreviation`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo global")
    enterCommand("iabbrev <buffer> foo local")
    enterCommand("iunabbrev <buffer> foo")
    typeText("i", "foo ")
    assertState("global \n")
  }

  @Test
  fun `iabclear with buffer modifier clears only the buffer-local abbreviations`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo global")
    enterCommand("iabbrev <buffer> foo local")
    enterCommand("iabclear <buffer>")
    typeText("i", "foo ")
    assertState("global \n")
  }

  @Test
  fun `iabbrev with no arguments lists all defined insert-mode abbreviations`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    enterCommand("iabbrev teh the")
    assertCommandOutput("iabbrev", "i  foo            bar\ni  teh            the")
  }

  @Test
  fun `iabbrev with buffer marks buffer-local entries with at sign`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo global")
    enterCommand("iabbrev <buffer> baz local")
    assertCommandOutput("iabbrev", "i @baz            local\ni  foo            global")
  }

  @Test
  fun `iabbrev with buffer argument lists only buffer-local entries`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo global")
    enterCommand("iabbrev <buffer> baz local")
    assertCommandOutput("iabbrev <buffer>", "i @baz            local")
  }

  @Test
  fun `cabbrev with no arguments lists only cmdline-mode abbreviations`() {
    enterCommand("iabbrev foo bar")
    enterCommand("cabbrev myabbrev myexpansion")
    assertCommandOutput("cabbrev", "c  myabbrev       myexpansion")
  }

  @Test
  fun `iabbrev listing marks expr entries with asterisk`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    enterCommand("iabbrev <expr> dt 1+2")
    assertCommandOutput("iabbrev", "i  dt           * 1+2\ni  foo            bar")
  }

  @Test
  fun `iabbrev with expr modifier evaluates rhs at expansion time`() {
    configureByText("${c}\n")
    enterCommand("iabbrev <expr> dt 1+2")
    typeText("i", "dt ")
    assertState("3 \n")
  }

  @Test
  fun `iabbrev with expr modifier supports string expressions`() {
    configureByText("${c}\n")
    enterCommand("""iabbrev <expr> shout toupper("hello")""")
    typeText("i", "shout ")
    assertState("HELLO \n")
  }

  @Test
  fun `iabbrev with expr re-evaluates on each expansion`() {
    configureByText("${c}\n")
    enterCommand("let g:n = 0")
    enterCommand("iabbrev <expr> n g:n")
    enterCommand("let g:n = 7")
    typeText("i", "n ")
    assertState("7 \n")
  }

  @Test
  fun `iabbrev with buffer modifier before expr modifier works`() {
    configureByText("${c}\n")
    enterCommand("iabbrev <buffer> <expr> add 1+1")
    typeText("i", "add ")
    assertState("2 \n")
  }

  @Test
  fun `iabbrev with expr modifier before buffer modifier works`() {
    configureByText("${c}\n")
    enterCommand("iabbrev <expr> <buffer> add 1+1")
    typeText("i", "add ")
    assertState("2 \n")
  }

  @Test
  fun `iabbrev with expr accepts an invalid expression at registration time`() {
    configureByText("\n")
    enterCommand("iabbrev <expr> bad 1+")
    assertPluginError(false)
  }

  @Test
  fun `cabbrev with expr evaluates rhs in cmdline`() {
    enterCommand("cabbrev <expr> myabbrev 1+1")
    typeText(":myabbrev ")
    assertExText("2 ")
  }

  @Test
  fun `should show error when trying to evaluate an invalid expression`() {
    configureByText("\n")
    enterCommand("iabbrev <expr> myabbrev bad")
    typeText("i myabbrev ")
    assertPluginErrorMessage("E121: Undefined variable: bad")
  }

  @Test
  fun `iabbrev accepts a lhs with dash when iskeyword includes dash`() {
    configureByText("${c}\n")
    enterCommand("set iskeyword+=-")
    enterCommand("iabbrev foo-bar baz")
    assertPluginError(false)
    typeText("i", "foo-bar ")
    assertState("baz \n")
  }

  @Test
  fun `iabbrev rejects a lhs with dash when iskeyword does not include dash`() {
    configureByText("\n")
    enterCommand("iabbrev foo-bar baz")
    assertPluginError(true)
  }

  @Test
  fun `iabbrev does not expand on dash trigger when iskeyword includes dash`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    enterCommand("set iskeyword+=-")
    assertPluginError(false)
    typeText("i", "foo-")
    assertState("foo-\n")
  }

  @Test
  fun `iabbrev still expands on space trigger after iskeyword change`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    enterCommand("set iskeyword+=-")
    assertPluginError(false)
    typeText("i", "foo ")
    assertState("bar \n")
  }

  @Test
  fun `iabbrev walk-back stops at whitespace when iskeyword includes dash`() {
    configureByText("${c}\n")
    enterCommand("set iskeyword+=-")
    enterCommand("iabbrev foo-bar baz")
    typeText("i", "xyz-foo-bar ")
    assertState("xyz-foo-bar \n")
  }

  @Test
  fun `abbrev with no mode prefix lists entry under both modes as a single bang row`() {
    enterCommand("abbrev foo bar")
    assertCommandOutput("abbrev", "!  foo            bar")
  }

  @Test
  fun `abbrev listing shows insert-only entry with i marker`() {
    enterCommand("iabbrev foo bar")
    assertCommandOutput("abbrev", "i  foo            bar")
  }

  @Test
  fun `abbrev listing shows cmdline-only entry with c marker`() {
    enterCommand("cabbrev foo bar")
    assertCommandOutput("abbrev", "c  foo            bar")
  }

  @Test
  fun `abbrev listing shows separate rows when insert and cmdline have different rhs for same lhs`() {
    enterCommand("iabbrev foo insertRhs")
    enterCommand("cabbrev foo cmdlineRhs")
    assertCommandOutput("abbrev", "c  foo            cmdlineRhs\ni  foo            insertRhs")
  }

  @Test
  fun `abbrev listing merges bang row even with expr modifier`() {
    enterCommand("abbrev <expr> foo 1+1")
    assertCommandOutput("abbrev", "!  foo          * 1+1")
  }
}
