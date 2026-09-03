/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the deprecated (i.e. pre-`idea` prefix) names of IdeaVim's IDE specific options
 *
 * IdeaVim's own options are prefixed with "idea" to distinguish them from Vim's options. Some options were introduced
 * without the prefix, and have since been renamed. The old name is still recognised, so that existing `~/.ideavimrc`
 * files keep working, but it is only an alias - the canonical name is always used for storage and for output.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
@Suppress("SpellCheckingInspection")
class DeprecatedOptionNameTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test set toggle option with deprecated name`() {
    enterCommand("set windowjumps")
    assertCommandOutput("set ideawindowjumps?", "  ideawindowjumps")
  }

  @Test
  fun `test query option with deprecated name reports canonical name`() {
    enterCommand("set ideawindowjumps")
    assertCommandOutput("set windowjumps?", "  ideawindowjumps")
  }

  @Test
  fun `test unset toggle option with deprecated name`() {
    // 'ideaclosenotebooks' is on by default
    enterCommand("set noclosenotebooks")
    assertCommandOutput("set ideaclosenotebooks?", "noideaclosenotebooks")
  }

  @Test
  fun `test invert toggle option with deprecated name`() {
    enterCommand("set invclosenotebooks")
    assertCommandOutput("set ideaclosenotebooks?", "noideaclosenotebooks")
  }

  @Test
  fun `test toggle option with deprecated name and bang`() {
    enterCommand("set noideaclosenotebooks")
    enterCommand("set closenotebooks!")
    assertCommandOutput("set ideaclosenotebooks?", "  ideaclosenotebooks")
  }

  @Test
  fun `test set value with deprecated name`() {
    enterCommand("set visualdelay=200")
    assertCommandOutput("set ideavisualdelay?", "  ideavisualdelay=200")
  }

  @Test
  fun `test reset to default value with deprecated name`() {
    enterCommand("set ideavisualdelay=200")
    enterCommand("set visualdelay&")
    assertCommandOutput("set ideavisualdelay?", "  ideavisualdelay=100")
  }

  @Test
  fun `test deprecated name and canonical name share a value`() {
    enterCommand("set visualdelay=200")
    assertCommandOutput("set visualdelay?", "  ideavisualdelay=200")
    enterCommand("set ideavisualdelay=300")
    assertCommandOutput("set visualdelay?", "  ideavisualdelay=300")
  }

  @Test
  fun `test abbreviation still works after rename`() {
    enterCommand("set trackactionids")
    assertCommandOutput("set tai?", "  ideatrackactionids")
  }

  @Test
  fun `test deprecated name in option expression`() {
    enterCommand("set windowjumps")
    typeText(commandToKeys("if &windowjumps | echo 'on' | else | echo 'off' | endif"))
    assertExOutput("on")
  }

  @Test
  fun `test set option value with let and deprecated name`() {
    enterCommand("let &visualdelay = 300")
    assertCommandOutput("set ideavisualdelay?", "  ideavisualdelay=300")
  }

  @Test
  fun `test deprecated name is not listed by set`() {
    enterCommand("set visualdelay=200")

    val tokens = getShownOptions("set")
    assertTrue("ideavisualdelay=200" in tokens, "':set' should list the canonical name. Shown: $tokens")
    assertFalse("visualdelay=200" in tokens, "':set' should not list the deprecated name. Shown: $tokens")
  }

  @Test
  fun `test deprecated name is not listed by set all`() {
    val tokens = getShownOptions("set all")
    assertTrue(tokens.any { it.startsWith("ideavisualdelay=") }, "':set all' should list the canonical name")
    assertFalse(tokens.any { it.startsWith("visualdelay=") }, "':set all' should not list the deprecated name")
  }

  @Test
  fun `test unknown option is still an error`() {
    enterCommand("set ideadoesnotexist")
    assertPluginErrorMessage("E518: Unknown option: ideadoesnotexist")
  }

  /**
   * Run a `:set` command that lists options and return the shown options as a set of tokens
   *
   * Each option is shown as one of "  name", "noname", "--name" or "  name=value". Values never contain spaces, so
   * splitting the body of the output on whitespace gives one token per shown option.
   */
  private fun getShownOptions(command: String): Set<String> {
    clearOutputPanel()
    enterCommand(command)
    val output = readOutputPanel { it.text }
    assertNotNull(output, "No Ex output for ':$command'")
    clearOutputPanel()
    return output.lines().drop(1).filter { it.isNotBlank() }
      .flatMap { it.trim().split(Regex("\\s+")) }
      .toSet()
  }
}
