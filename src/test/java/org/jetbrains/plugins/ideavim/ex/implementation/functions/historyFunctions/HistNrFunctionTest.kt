/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.historyFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class HistNrFunctionTest : VimTestCase("\n") {
  @Test
  fun `test histnr returns -1 for unknown history type`() {
    assertCommandOutput("echo histnr('unknown')", "-1")
  }

  @Test
  fun `test histnr returns -1 for empty search history`() {
    assertCommandOutput("echo histnr('search')", "-1")
  }

  @Test
  fun `test histnr returns -1 for empty expr history`() {
    assertCommandOutput("echo histnr('expr')", "-1")
  }

  @Test
  fun `test histnr returns -1 for empty input history`() {
    assertCommandOutput("echo histnr('input')", "-1")
  }

  @Test
  fun `test histnr returns 1 after first search history entry`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histnr('search')", "1")
  }

  @Test
  fun `test histnr increments with each new search history entry`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'bar')")
    assertCommandOutput("echo histnr('search')", "2")
  }

  @Test
  fun `test histnr increments even when duplicate is added to search history`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'foo')")
    // Duplicate is removed and re-added with a higher counter
    assertCommandOutput("echo histnr('search')", "2")
  }

  @Test
  fun `test histnr returns entry number from search history using slash shortcut`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histnr('/')", "1")
  }

  @Test
  fun `test histnr returns entry number from search history using abbreviated name`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histnr('s')", "1")
  }

  @Test
  fun `test histnr returns 1 after first expr history entry`() {
    enterCommand("call histadd('expr', '1 + 1')")
    assertCommandOutput("echo histnr('expr')", "1")
  }

  @Test
  fun `test histnr increments with each new expr history entry`() {
    enterCommand("call histadd('expr', '1 + 1')")
    enterCommand("call histadd('expr', '2 + 2')")
    assertCommandOutput("echo histnr('expr')", "2")
  }

  @Test
  fun `test histnr returns entry number from expr history using equals shortcut`() {
    enterCommand("call histadd('expr', '1 + 1')")
    assertCommandOutput("echo histnr('=')", "1")
  }

  @Test
  fun `test histnr returns entry number from expr history using abbreviated name`() {
    enterCommand("call histadd('expr', '1 + 1')")
    assertCommandOutput("echo histnr('e')", "1")
  }

  @Test
  fun `test histnr returns 1 after first input history entry`() {
    enterCommand("call histadd('input', 'my input')")
    assertCommandOutput("echo histnr('input')", "1")
  }

  @Test
  fun `test histnr increments with each new input history entry`() {
    enterCommand("call histadd('input', 'first')")
    enterCommand("call histadd('input', 'second')")
    assertCommandOutput("echo histnr('input')", "2")
  }

  @Test
  fun `test histnr returns entry number from input history using at shortcut`() {
    enterCommand("call histadd('input', 'my input')")
    assertCommandOutput("echo histnr('@')", "1")
  }

  @Test
  fun `test histnr returns entry number from input history using abbreviated name`() {
    enterCommand("call histadd('input', 'my input')")
    assertCommandOutput("echo histnr('i')", "1")
  }

  // Note: `enterCommand` auto-adds each executed command to cmd history.
  // After `enterCommand("call histadd('cmd', 'echo hello')")`, cmd history has:
  //   #1: call histadd('cmd', 'echo hello')  (auto-added on execution)
  //   #2: echo hello                          (added by histadd)
  // When `assertCommandOutput("echo histnr('cmd')")` runs, it adds `echo histnr('cmd')` as #3.
  @Test
  fun `test histnr returns entry number from cmd history using full name`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    // cmd history: [#1: call histadd..., #2: echo hello]
    // assertCommandOutput adds "echo histnr('cmd')" as #3 before evaluation
    assertCommandOutput("echo histnr('cmd')", "3")
  }

  @Test
  fun `test histnr returns entry number from cmd history using colon shortcut`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput("echo histnr(':')", "3")
  }

  @Test
  fun `test histnr returns entry number from cmd history using abbreviated name`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput("echo histnr('c')", "3")
  }
}
