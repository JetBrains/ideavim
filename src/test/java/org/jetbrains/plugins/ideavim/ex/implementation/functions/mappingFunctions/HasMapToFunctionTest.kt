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
class HasMapToFunctionTest : VimTestCase("\n") {
  @Test
  fun `test hasmapto returns 0 for unknown mapping target`() {
    assertCommandOutput("echo hasmapto('yy')", "0")
  }

  @Test
  fun `test hasmapto returns 1 when nmap rhs matches`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo hasmapto('yy')", "1")
  }

  @Test
  fun `test hasmapto returns 1 with explicit normal mode`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo hasmapto('yy', 'n')", "1")
  }

  @Test
  fun `test hasmapto returns 1 for nnoremap`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo hasmapto('yy', 'n')", "1")
  }

  @Test
  fun `test hasmapto returns 0 when mode does not match`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo hasmapto('yy', 'i')", "0")
  }

  @Test
  fun `test hasmapto returns 1 for imap`() {
    enterCommand("imap jj <Esc>")
    assertCommandOutput("echo hasmapto('<Esc>', 'i')", "1")
  }

  @Test
  fun `test hasmapto with empty mode string searches nvo modes - nmap`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo hasmapto('yy', '')", "1")
  }

  @Test
  fun `test hasmapto with empty mode string searches nvo modes - omap`() {
    enterCommand("omap x yy")
    assertCommandOutput("echo hasmapto('yy', '')", "1")
  }

  @Test
  fun `test hasmapto with empty mode string does not find imap`() {
    enterCommand("imap x yy")
    assertCommandOutput("echo hasmapto('yy', '')", "0")
  }

  @Test
  fun `test hasmapto returns 1 for partial rhs match`() {
    // hasmapto checks if what is contained in the rhs
    enterCommand("nmap x ddyy")
    assertCommandOutput("echo hasmapto('yy', 'n')", "1")
  }

  @Test
  fun `test hasmapto returns 0 after mapping is removed`() {
    enterCommand("nmap x yy")
    enterCommand("nunmap x")
    assertCommandOutput("echo hasmapto('yy', 'n')", "0")
  }

  @Test
  fun `test hasmapto returns 0 for abbreviations`() {
    // TODO: IdeaVim should support abbreviations
    assertCommandOutput("echo hasmapto('yy', '', 1)", "0")
  }

  @Test
  fun `test hasmapto finds vmap in visual mode`() {
    enterCommand("vmap x gq")
    assertCommandOutput("echo hasmapto('gq', 'v')", "1")
  }

  @Test
  fun `test hasmapto finds xmap in visual mode`() {
    enterCommand("xmap x gq")
    assertCommandOutput("echo hasmapto('gq', 'x')", "1")
  }

  @Test
  fun `test hasmapto finds cmap in command mode`() {
    enterCommand("cmap jj <Esc>")
    assertCommandOutput("echo hasmapto('<Esc>', 'c')", "1")
  }

  @Test
  fun `test hasmapto finds omap in operator-pending mode`() {
    enterCommand("omap w b")
    assertCommandOutput("echo hasmapto('b', 'o')", "1")
  }

  @Test
  fun `test hasmapto finds map in nvo modes`() {
    enterCommand("map x dd")
    assertCommandOutput("echo hasmapto('dd', 'n')", "1")
  }

  @Test
  fun `test hasmapto with special key in rhs`() {
    enterCommand("nmap x <C-a>")
    assertCommandOutput("echo hasmapto('<C-a>', 'n')", "1")
//    enterCommand("nmap x <Esc>")
//    assertCommandOutput("echo hasmapto('<Esc>', 'n')", "1")
  }

  @Test
  fun `test hasmapto returns 0 when rhs does not contain what`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo hasmapto('dd', 'n')", "0")
  }

  @Test
  fun `test hasmapto finds mapping in default mode when no mode specified`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo hasmapto('yy')", "1")
  }
}
