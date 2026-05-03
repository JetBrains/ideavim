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
class MapListFunctionTest : VimTestCase("\n") {
  @Test
  fun `test maplist returns empty list for abbreviations`() {
    // TODO: IdeaVim should support abbreviations
    assertCommandOutput("echo maplist(1)", "[]")
  }

  @Test
  fun `test maplist with explicit zero flag returns mappings`() {
    enterCommand("nmap x yy")
    assertCommandOutput(
      "echo maplist(0)",
      "[{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}]",
    )
  }

  @Test
  fun `test maplist contains nmap entry`() {
    enterCommand("nmap x yy")
    assertCommandOutput(
      "echo maplist()",
      "[{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}]",
    )
  }

  @Test
  fun `test maplist contains nnoremap entry`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput(
      "echo maplist()",
      "[{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 1, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}]",
    )
  }

  @Test
  fun `test maplist contains imap entry`() {
    enterCommand("imap jj <Esc>")
    assertCommandOutput(
      "echo maplist()",
      "[{'lhs': 'jj', 'lhsraw': 'jj', 'rhs': '<Esc>', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'i', 'nowait': 0, 'abbr': 0}]",
    )
  }

  @Test
  fun `test maplist contains entries from multiple modes`() {
    enterCommand("nmap x yy")
    enterCommand("imap x <Esc>")
    assertCommandOutput(
      "echo maplist()",
      "[{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}," +
        " {'lhs': 'x', 'lhsraw': 'x', 'rhs': '<Esc>', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'i', 'nowait': 0, 'abbr': 0}]",
    )
  }

  @Test
  fun `test maplist contains single map entry for NVO mode with mode value set to ' '`() {
    enterCommand("map x yy")
    assertCommandOutput(
      "echo maplist()",
      "[{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': ' ', 'nowait': 0, 'abbr': 0}]",
    )
  }

  @Test
  fun `test maplist contains single map entry NVO mapping after unmapping N with mode value set to 'ov'`() {
    // Vim stores the `x` mapping as a single mapping for NVO. When we unmap N, it's still a single mapping, but now for
    // OV only
    enterCommand("map x yy")
    enterCommand("nunmap x")
    assertCommandOutput(
      "echo maplist()",
      "[{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'ov', 'nowait': 0, 'abbr': 0}]",
    )
  }

  @Test
  fun `test maplist contains single map entry for IC mode with mode value set to '!'`() {
    enterCommand("map! x yy")
    assertCommandOutput(
      "echo maplist()",
      "[{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': '!', 'nowait': 0, 'abbr': 0}]",
    )
  }

  @Test
  fun `test maplist entry has correct rhs`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maplist()[0]['rhs'])", "'yy'")
  }

  @Test
  fun `test maplist entry has noremap 0 for nmap`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(maplist()[0]['noremap'])", "0")
  }

  @Test
  fun `test maplist entry has noremap 1 for nnoremap`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maplist()[0]['noremap'])", "1")
  }

  @Test
  fun `test maplist entry has mode n for nmap`() {
    enterCommand("nmap x yy")
    assertCommandOutput("echo string(maplist()[0]['mode'])", "'n'")
  }

  @Test
  fun `test maplist entry has mode i for imap`() {
    enterCommand("imap x yy")
    assertCommandOutput("echo string(maplist()[0]['mode'])", "'i'")
  }

  @Test
  fun `test maplist entry has mode space for map`() {
    enterCommand("map x yy")
    assertCommandOutput("echo string(maplist()[0]['mode'])", "' '")
  }

  @Test
  fun `test maplist entry uses current mode after unmapping one mode`() {
    enterCommand("map x yy")  // NVO
    enterCommand("nunmap x")  // OV
    assertCommandOutput("echo string(maplist()[0]['mode'])", "'ov'")
  }

  @Test
  fun `test maplist entry has expr 0 for regular mapping`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maplist()[0]['expr'])", "0")
  }

  @Test
  fun `test maplist entry has expr 1 for expr mapping`() {
    enterCommand("nnoremap <expr> x '2j'")
    assertCommandOutput("echo string(maplist()[0]['expr'])", "1")
  }

  @Test
  fun `test maplist entry has abbr 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maplist()[0]['abbr'])", "0")
  }

  @Test
  fun `test maplist entry has silent 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maplist()[0]['silent'])", "0")
  }

  @VimBehaviorDiffers(originalVimAfter = "1")
  @Test
  fun `test maplist entry has silent 0 even when specified in map command`() {
    enterCommand("nnoremap <silent> x yy")
    assertCommandOutput("echo string(maplist()[0]['silent'])", "0")
  }

  @Test
  fun `test maplist entry has buffer 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maplist()[0]['buffer'])", "0")
  }

  @VimBehaviorDiffers(originalVimAfter = "1")
  @Test
  fun `test maplist entry has buffer 0 for buffer local mappings`() {
    enterCommand("nnoremap <buffer> x yy")
    assertCommandOutput("echo string(maplist()[0]['buffer'])", "0")
  }

  @Test
  fun `test maplist entry has nowait 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maplist()[0]['nowait'])", "0")
  }

  @VimBehaviorDiffers(originalVimAfter = "1")
  @Test
  fun `test maplist entry has nowait 0 even when specified in map command`() {
    enterCommand("nnoremap <nowait> x yy")
    assertCommandOutput("echo string(maplist()[0]['nowait'])", "0")
  }

  @Test
  fun `test maplist entry has script 0`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maplist()[0]['script'])", "0")
  }

  @Test
  fun `test maplist entry lhsraw matches lhs for simple key`() {
    enterCommand("nnoremap x yy")
    assertCommandOutput("echo string(maplist()[0]['lhsraw'])", "'x'")
  }

  @Test
  fun `test maplist full dict for nmap`() {
    enterCommand("nmap x yy")
    assertCommandOutput(
      "echo maplist()",
      "[{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}]",
    )
  }
}
