/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class LmapCommandTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmap command is accepted`() {
    configureByText("\n")
    enterCommand("lmap foo bar")
    assertPluginError(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lnoremap command is accepted`() {
    configureByText("\n")
    enterCommand("lnoremap foo bar")
    assertPluginError(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test abbreviated lm and ln commands are accepted`() {
    configureByText("\n")
    enterCommand("lm foo bar")
    assertPluginError(false)
    enterCommand("ln baz qux")
    assertPluginError(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmap is listed under lmap in language mode`() {
    configureByText("\n")
    enterCommand("lmap foo bar")
    assertCommandOutput(
      "lmap",
      """
        |l  foo           bar
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmap does not leak into other mapping modes`() {
    configureByText("\n")
    enterCommand("lmap foo bar")

    // Language mappings are their own mode -- they must not appear in Normal, Insert or `:map`.
    assertCommandOutput("nmap", "No mapping found")
    assertCommandOutput("imap", "No mapping found")
    assertCommandOutput("map", "No mapping found")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lunmap removes a language mapping`() {
    configureByText("\n")
    enterCommand("lmap foo bar")
    enterCommand("lunmap foo")
    assertPluginError(false)
    assertCommandOutput("lmap", "No mapping found")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmapclear removes all language mappings`() {
    configureByText("\n")
    enterCommand("lmap foo bar")
    enterCommand("lmap baz qux")
    enterCommand("lmapclear")
    assertPluginError(false)
    assertCommandOutput("lmap", "No mapping found")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmapclear does not clear other mapping modes`() {
    configureByText("\n")
    enterCommand("nmap foo bar")
    enterCommand("lmap foo baz")
    enterCommand("lmapclear")

    assertCommandOutput("lmap", "No mapping found")
    assertCommandOutput(
      "nmap",
      """
        |n  foo           bar
      """.trimMargin(),
    )
  }
}
