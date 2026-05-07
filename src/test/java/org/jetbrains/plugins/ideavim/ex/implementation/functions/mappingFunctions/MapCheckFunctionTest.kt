/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.mappingFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class MapCheckFunctionTest : VimTestCase("\n") {
  @Test
  fun `test mapcheck returns empty string for unknown mapping`() {
    assertCommandOutput("echo string(mapcheck('x'))", "''")
  }

  @Test
  fun `test mapcheck returns rhs of exact nmap match`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(mapcheck('x'))", "'yy'")
  }

  @Test
  fun `test mapcheck returns rhs of exact match with explicit normal mode`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(mapcheck('x', 'n'))", "'yy'")
  }

  @Test
  fun `test mapcheck returns rhs of nnoremap`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(mapcheck('x', 'n'))", "'yy'")
  }

  @Test
  fun `test mapcheck returns empty string when mode does not match`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(mapcheck('x', 'i'))", "''")
  }

  @Test
  fun `test mapcheck returns rhs of imap`() {
    enterCommand("imap jj <Esc>")
    assertCommandOutput("echo string(mapcheck('jj', 'i'))", "'<Esc>'")
  }

  @Test
  fun `test mapcheck with empty mode string searches nvo modes`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(mapcheck('x', ''))", "'yy'")
  }

  @Test
  fun `test mapcheck with empty mode string searches nvo modes - omap`() {
    enterCommand("omap x yy")
    assertCommandOutput("echo string(mapcheck('x', ''))", "'yy'")
  }

  @Test
  fun `test mapcheck with empty mode string searches nvo modes - negative test for imap`() {
    enterCommand("imap x yy")
    assertCommandOutput("echo string(mapcheck('x', ''))", "''")
  }

  @Test
  fun `test mapcheck returns empty string for abbreviations`() {
    // TODO: IdeaVim should support abbreviations
    assertCommandOutput("echo string(mapcheck('ab', '', 1))", "''")
  }

  // mapcheck differs from maparg in that it uses bidirectional prefix matching

  @Test
  fun `test mapcheck returns rhs when name is a prefix of an existing mapping`() {
    // 'xy' is mapped; checking 'x' should find it because 'x' is a prefix of 'xy'
    enterCommand("nmap xy yy")
    assertCommandOutput("echo string(mapcheck('x', 'n'))", "'yy'")
  }

  @Test
  fun `test mapcheck returns rhs when existing mapping is a prefix of name`() {
    // 'x' is mapped; checking 'xy' should find it because 'x' is a prefix of 'xy'
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(mapcheck('xy', 'n'))", "'yy'")
  }

  @Test
  fun `test mapcheck returns empty string when no prefix conflict exists`() {
    enterCommand("nmap ab yy")
    assertCommandOutput("echo string(mapcheck('x', 'n'))", "''")
  }

  @Test
  fun `test mapcheck with longer name that starts with mapped key`() {
    // Like the Vim docs example: map _v exists, so mapcheck('_vv') is non-empty
    enterCommand("nmap _v dd")
    assertCommandOutput("echo string(mapcheck('_vv', 'n'))", "'dd'")
  }

  @Test
  fun `test mapcheck with shorter name that is prefix of mapped key`() {
    // Like the Vim docs example: map _vvv exists, so mapcheck('_vv') is non-empty
    enterCommand("nmap _vvv dd")
    assertCommandOutput("echo string(mapcheck('_vv', 'n'))", "'dd'")
  }

  @Test
  fun `test mapcheck with exact match from docs example`() {
    // Like the Vim docs example: map _vv exists, so mapcheck('_vv') is non-empty
    enterCommand("nmap _vv dd")
    assertCommandOutput("echo string(mapcheck('_vv', 'n'))", "'dd'")
  }

  @Test
  fun `test mapcheck with special key in mapping`() {
    enterCommand("nmap <C-a> gg")
    assertCommandOutput("echo string(mapcheck('<C-a>', 'n'))", "'gg'")
  }

  @Test
  fun `test mapcheck returns empty string after mapping is unmapped`() {
    enterCommand("nmap x yy")
    enterCommand("nunmap x")
    assertCommandOutput("echo string(mapcheck('x', 'n'))", "''")
  }

  @Test
  fun `test mapcheck finds vmap in visual mode`() {
    enterCommand("vmap x gq")
    assertCommandOutput("echo string(mapcheck('x', 'v'))", "'gq'")
  }

  @Test
  fun `test mapcheck finds xmap in visual mode`() {
    enterCommand("xmap x gq")
    assertCommandOutput("echo string(mapcheck('x', 'x'))", "'gq'")
  }

  @Test
  fun `test mapcheck finds cmap in command mode`() {
    // Note that we can't look for `jj` here because we can't type it!
    enterCommand("cmap jjj <Esc>")
    assertCommandOutput("echo string(mapcheck('jj', 'c'))", "'<Esc>'")
  }

  @Test
  fun `test mapcheck finds omap in operator-pending mode`() {
    enterCommand("omap w b")
    assertCommandOutput("echo string(mapcheck('w', 'o'))", "'b'")
  }

  @Test
  fun `test mapcheck with no args returns empty for unknown mapping`() {
    assertCommandOutput("echo string(mapcheck('nonexistent'))", "''")
  }

  @Test
  fun `test mapcheck finds map in default nvo modes - nmap`() {
    enterCommand("map x dd")
    assertCommandOutput("echo string(mapcheck('x', 'n'))", "'dd'")
  }

  @Test
  fun `test mapcheck finds map in default nvo modes - omap`() {
    enterCommand("map x dd")
    assertCommandOutput("echo string(mapcheck('x', 'o'))", "'dd'")
  }
}
