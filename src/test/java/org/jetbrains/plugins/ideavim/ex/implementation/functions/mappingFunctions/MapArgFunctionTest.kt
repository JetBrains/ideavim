/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.mappingFunctions

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class MapArgFunctionTest : VimTestCase("\n") {
  @Test
  fun `test maparg returns empty string for unknown mapping`() {
    assertCommandOutput("echo string(maparg('x'))", "''")
  }

  @Test
  fun `test maparg returns rhs of nmap`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(maparg('x'))", "'yy'")
  }

  @Test
  fun `test maparg returns rhs with explicit normal mode`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(maparg('x', 'n'))", "'yy'")
  }

  @Test
  fun `test maparg returns rhs of nnoremap`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n'))", "'yy'")
  }

  @Test
  fun `test maparg returns empty string when mode does not match`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(maparg('x', 'i'))", "''")
  }

  @Test
  fun `test maparg returns rhs of imap`() {
    enterCommand("imap jj <Esc>")
    assertCommandOutput("echo string(maparg('jj', 'i'))", "'<Esc>'")
  }

  @Test
  fun `test maparg with empty mode string searches nvo modes`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(maparg('x', ''))", "'yy'")
  }

  @Test
  fun `test maparg with empty mode string searches nvo modes 2`() {
    enterCommand("omap x yy")
    assertCommandOutput("echo string(maparg('x', ''))", "'yy'")
  }

  @Test
  fun `test maparg with empty mode string searches nvo modes - negative test`() {
    enterCommand("imap x yy")
    assertCommandOutput("echo string(maparg('x', ''))", "''")
  }

  @Test
  fun `test maparg returns empty string for abbreviations`() {
    // TODO: IdeaVim should support abbreviations
    assertCommandOutput("echo string(maparg('ab', '', 1))", "''")
  }

  @Test
  fun `test maparg returns empty dict for abbreviations`() {
    // TODO: IdeaVim should support abbreviations
    assertCommandOutput("echo string(maparg('ab', '', 1, 1))", "{}")
  }

  @Test
  fun `test maparg with dict flag returns empty dict for unknown mapping`() {
    assertCommandOutput("echo maparg('x', 'n', 0, 1)", "{}")
  }

  @Test
  fun `test maparg dict has correct lhs`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['lhs'])", "'x'")
  }

  @Test
  fun `test maparg dict has correct rhs`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['rhs'])", "'yy'")
  }

  @Test
  fun `test maparg dict has noremap 0 for nmap`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['noremap'])", "0")
  }

  @Test
  fun `test maparg dict has noremap 1 for nnoremap`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['noremap'])", "1")
  }

  @Test
  fun `test maparg dict has mode n for nmap`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['mode'])", "'n'")
  }

  @Test
  fun `test maparg dict has mode space for map`() {
    enterCommand("map x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['mode'])", "' '")
  }

  @Test
  fun `test maparg returns current mode after unmapping in one mode`() {
    enterCommand("map x yy")  // NVO
    enterCommand("nunmap x")  // Now the mapping is OV
    assertCommandOutput("echo string(maparg('x', '', 0, 1)['mode'])", "'ov'")
  }

  // TODO: More options for map?

  @Test
  fun `test maparg dict has expr 0 for regular mapping`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['expr'])", "0")
  }

  @Test
  fun `test maparg dict has expr 1 for expr mapping`() {
    enterCommand("nnoremap <expr> x '2j'")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['expr'])", "1")
  }

  @Test
  fun `test maparg dict has abbr 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['abbr'])", "0")
  }

  @Test
  fun `test maparg dict has silent 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['silent'])", "0")
  }

  @VimBehaviorDiffers(originalVimAfter = "1")
  @Test
  fun `test maparg dict has silent 0 even when specified in map command`() {
    enterCommand("nnoremap <silent> x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['silent'])", "0")
  }

  @Test
  fun `test maparg dict has buffer 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['buffer'])", "0")
  }

  @VimBehaviorDiffers(originalVimAfter = "1")
  @Test
  fun `test maparg dict has buffer 0 for buffer local mappings`() {
    enterCommand("nnoremap <buffer> x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['buffer'])", "0")
  }

  @Test
  fun `test maparg dict has nowait 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['nowait'])", "0")
  }

  @VimBehaviorDiffers(originalVimAfter = "1")
  @Test
  fun `test maparg dict has nowait 0 even when specified in map command`() {
    enterCommand("nnoremap <nowait> x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['nowait'])", "0")
  }

  @Test
  fun `test maparg dict has script 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['script'])", "0")
  }

  @Test
  fun `test maparg dict lhsraw matches lhs for simple key`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maparg('x', 'n', 0, 1)['lhsraw'])", "'x'")
  }

  // TODO: lhsraw for special keys

  @Test
  fun `test maparg full dict for nnoremap`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput(
      "echo maparg('x', 'n', 0, 1)",
      "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 1, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}",
    )
  }

  @Test
  fun `test maparg full dict for nmap`() {
    enterCommand("nmap x yy")
    assertCommandOutput(
      "echo maparg('x', 'n', 0, 1)",
      "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}",
    )
  }
}
