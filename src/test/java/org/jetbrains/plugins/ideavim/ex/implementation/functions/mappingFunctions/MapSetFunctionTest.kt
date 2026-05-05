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
class MapSetFunctionTest : VimTestCase("\n") {
  @Test
  fun `test mapset creates nmap with three-argument form`() {
    val dict = "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset('n', 0, $dict)")
    assertCommandOutput("map x", "n  x             yy")
  }

  @Test
  fun `test mapset creates nnoremap with noremap 1 in dict`() {
    val dict = "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 1, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset('n', 0, $dict)")
    assertCommandOutput("map x", "n  x           * yy")
  }

  @Test
  fun `test mapset creates nmap with noremap 0 in dict`() {
    val dict = "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset('n', 0, $dict)")
    assertCommandOutput("map x", "n  x             yy")
  }

  @Test
  fun `test mapset creates imap with mode i in three-argument form`() {
    val dict = "{'lhs': 'jj', 'lhsraw': 'jj', 'rhs': '<Esc>', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'i', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset('i', 0, $dict)")
    assertCommandOutput("map! jj", "i  jj            <Esc>")
  }

  @Test
  fun `test mapset with three-argument form mode overrides dict mode`() {
    // The mode argument (first arg) determines the mapping mode, not dict's mode field
    val dict = "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'i', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset('n', 0, $dict)")
    assertCommandOutput("map x", "n  x             yy")
  }

  @Test
  fun `test mapset creates mapping with one-argument form`() {
    val dict = "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset($dict)")
    assertCommandOutput("map x", "n  x             yy")
  }

  @Test
  fun `test mapset one-argument form uses mode from dict`() {
    val dict = "{'lhs': 'jj', 'lhsraw': 'jj', 'rhs': '<Esc>', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'i', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset($dict)")
    assertCommandOutput("map! jj", "i  jj            <Esc>")
  }

  @Test
  fun `test mapset round-trip with maparg restores nmap`() {
    enterCommand("nmap x yy")
    enterCommand("let d = maparg('x', 'n', 0, 1)")
    enterCommand("nunmap x")
    assertCommandOutput("nmap x", "No mapping found")
    enterCommand("call mapset('n', 0, d)")
    assertCommandOutput("map x", "n  x             yy")
  }

  @Test
  fun `test mapset round-trip with maparg restores nnoremap`() {
    enterCommand("nnoremap x yy")
    enterCommand("let d = maparg('x', 'n', 0, 1)")
    enterCommand("nunmap x")
    enterCommand("call mapset('n', 0, d)")
    assertCommandOutput("map x", "n  x           * yy")
  }

  @Test
  fun `test mapset round-trip restores imap`() {
    enterCommand("imap jj <Esc>")
    enterCommand("let d = maparg('jj', 'i', 0, 1)")
    enterCommand("iunmap jj")
    enterCommand("call mapset('i', 0, d)")
    assertCommandOutput("map! jj", "i  jj            <Esc>")
  }

  @Test
  fun `test mapset round-trip preserves expr mapping`() {
    enterCommand("nnoremap <expr> x '2j'")
    enterCommand("let d = maparg('x', 'n', 0, 1)")
    enterCommand("nunmap x")
    enterCommand("call mapset('n', 0, d)")
    assertCommandOutput("nmap x", "n  x           * '2j'")
    assertCommandOutput("echo maparg('x', 'n', 0, 1).expr", "1")
  }

  @Test
  fun `test mapset with NVO mode (space) in one-argument form`() {
    val dict = "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': ' ', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset($dict)")
    assertCommandOutput("map x", "   x             yy")
  }

  @Test
  fun `test mapset with IC mode (!) in one-argument form`() {
    val dict = "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': '!', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset($dict)")
    assertCommandOutput("map!", "!  x             yy")
  }

  @Test
  fun `test mapset round-trip for NVO mapping`() {
    enterCommand("map x yy")
    enterCommand("let d = maparg('x', 'n', 0, 1)")
    enterCommand("unmap x")
    enterCommand("call mapset(d)")
    assertCommandOutput("map x", "   x             yy")
  }

  @Test
  fun `test mapset returns zero for abbreviations`() {
    // TODO: IdeaVim should support abbreviations
    val dict = "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}"
    assertCommandOutput("echo mapset('n', 1, $dict)", "0")
  }

  @Test
  fun `test mapset does not create mapping for abbreviations`() {
    // TODO: IdeaVim should support abbreviations
    val dict = "{'lhs': 'x', 'lhsraw': 'x', 'rhs': 'yy', 'silent': 0, 'noremap': 0, 'script': 0, 'expr': 0, 'buffer': 0, 'mode': 'n', 'nowait': 0, 'abbr': 0}"
    enterCommand("call mapset('n', 1, $dict)")
    assertCommandOutput("nmap x", "No mapping found")
  }

  @Test
  fun `test mapset throws E1206 when only 2 arguments`() {
    enterCommand("call mapset('n', 0)")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 3")
  }

  @Test
  fun `test mapset throws E1206 when third argument is not a dict`() {
    enterCommand("call mapset('n', 0, 'notadict')")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 3")
  }

  @Test
  fun `test mapset throws E1206 when single argument is not a dict`() {
    enterCommand("call mapset('notadict')")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test mapset throws E460 when dict is missing lhs`() {
    enterCommand("call mapset('n', 0, {'rhs': 'yy', 'expr': 0, 'noremap': 0})")
    assertPluginError(true)
    assertPluginErrorMessage("E460: Entries missing in mapset() dict argument")
  }

  @Test
  fun `test mapset throws E460 when dict is missing rhs`() {
    enterCommand("call mapset('n', 0, {'lhs': 'x', 'expr': 0, 'noremap': 0})")
    assertPluginError(true)
    assertPluginErrorMessage("E460: Entries missing in mapset() dict argument")
  }

  @Test
  fun `test mapset throws E460 when dict is missing expr`() {
    enterCommand("call mapset('n', 0, {'lhs': 'x', 'rhs': 'yy', 'noremap': 0})")
    assertPluginError(true)
    assertPluginErrorMessage("E460: Entries missing in mapset() dict argument")
  }

  @Test
  fun `test mapset throws E460 when dict is missing noremap`() {
    enterCommand("call mapset('n', 0, {'lhs': 'x', 'rhs': 'yy', 'expr': 0})")
    assertPluginError(true)
    assertPluginErrorMessage("E460: Entries missing in mapset() dict argument")
  }

  @Test
  fun `test mapset throws E460 when one-argument dict is missing mode`() {
    enterCommand("call mapset({'lhs': 'x', 'rhs': 'yy', 'expr': 0, 'noremap': 0})")
    assertPluginError(true)
    assertPluginErrorMessage("E460: Entries missing in mapset() dict argument")
  }
}
