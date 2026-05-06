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

class HistGetFunctionTest : VimTestCase("\n") {
  @Test
  fun `test histget returns empty string for unknown history type`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput("echo histget('unknown', 1)", "")
  }

  // Note: `enterCommand` auto-adds each executed command to cmd history.
  // After `enterCommand("call histadd('cmd', 'echo hello')")`, cmd history has:
  //   #1: call histadd('cmd', 'echo hello')  (auto-added on execution)
  //   #2: echo hello                          (added by histadd)
  // Similarly, when `assertCommandOutput("echo histget(...)")` runs its command,
  // the command itself is added to history first, so -1 resolves to that command.

  @Test
  fun `test histget returns entry from cmd history using full name`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    // Entry #1 = the enterCommand call itself; #2 = "echo hello" added by histadd
    assertCommandOutput("echo histget('cmd', 2)", "echo hello")
  }

  @Test
  fun `test histget returns entry from cmd history using colon shortcut`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput("echo histget(':', 2)", "echo hello")
  }

  @Test
  fun `test histget returns entry from cmd history using abbreviated name`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput("echo histget('c', 2)", "echo hello")
  }

  @Test
  fun `test histget returns specific cmd history entry using negative index`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    enterCommand("call histadd('cmd', 'echo world')")
    // History: [#1: call histadd hello, #2: echo hello, #3: call histadd world, #4: echo world]
    // When `echo histget('cmd', -2)` runs, it is added as #5, so -1 = #5 and -2 = #4 = "echo world"
    assertCommandOutput("echo histget('cmd', -2)", "echo world")
  }

  @Test
  fun `test histget returns earlier cmd history entry by positive index`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    enterCommand("call histadd('cmd', 'echo world')")
    // "echo hello" is at index 2; "echo world" is at index 4
    assertCommandOutput("echo histget('cmd', 2)", "echo hello")
  }

  @Test
  fun `test histget returns empty string for deleted cmd history entry`() {
    enterCommand("call histadd('cmd', 'echo hello')") // histadd #1 + echo hello #2
    enterCommand("call histadd('cmd', 'echo world')") // histadd #3 + echo world #4
    enterCommand("call histadd('cmd', 'echo hello')") // histadd #5 (replaces #1) + echo hello #6 (replaces #2)
    assertCommandOutput("echo string(histget('cmd', 2))", "''")
    assertCommandOutput("echo string(histget('cmd', 6))", "'echo hello'")
  }

  @Test
  fun `test histget returns empty string for out-of-range cmd history index`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput("echo string(histget('cmd', 99))", "''")
  }

  @Test
  fun `test histget returns entry from search history using full name`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histget('search', 1)", "foo")
  }

  @Test
  fun `test histget returns entry from search history using slash shortcut`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histget('/', 1)", "foo")
  }

  @Test
  fun `test histget returns entry from search history using abbreviated name`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histget('s', 1)", "foo")
  }

  @Test
  fun `test histget returns most recent search history entry using negative index`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'bar')")
    assertCommandOutput("echo histget('search', -1)", "bar")
  }

  @Test
  fun `test histget returns empty string for empty search history`() {
    assertCommandOutput("echo string(histget('search'))", "''")
  }

  @Test
  fun `test histget returns entry from expr history using full name`() {
    enterCommand("call histadd('expr', '1 + 1')")
    assertCommandOutput("echo histget('expr', 1)", "1 + 1")
  }

  @Test
  fun `test histget returns entry from expr history using equals shortcut`() {
    enterCommand("call histadd('expr', '1 + 1')")
    assertCommandOutput("echo histget('=', 1)", "1 + 1")
  }

  @Test
  fun `test histget returns entry from expr history using abbreviated name`() {
    enterCommand("call histadd('expr', '1 + 1')")
    assertCommandOutput("echo histget('e', 1)", "1 + 1")
  }

  @Test
  fun `test histget returns most recent expr history entry using negative index`() {
    enterCommand("call histadd('expr', '1 + 1')")
    enterCommand("call histadd('expr', '2 + 2')")
    assertCommandOutput("echo histget('expr', -1)", "2 + 2")
  }

  @Test
  fun `test histget returns empty string for empty expr history`() {
    assertCommandOutput("echo string(histget('expr'))", "''")
  }

  @Test
  fun `test histget returns entry from input history using full name`() {
    enterCommand("call histadd('input', 'my input')")
    assertCommandOutput("echo histget('input', 1)", "my input")
  }

  @Test
  fun `test histget returns entry from input history using at shortcut`() {
    enterCommand("call histadd('input', 'my input')")
    assertCommandOutput("echo histget('@', 1)", "my input")
  }

  @Test
  fun `test histget returns entry from input history using abbreviated name`() {
    enterCommand("call histadd('input', 'my input')")
    assertCommandOutput("echo histget('i', 1)", "my input")
  }

  @Test
  fun `test histget returns most recent input history entry using negative index`() {
    enterCommand("call histadd('input', 'first')")
    enterCommand("call histadd('input', 'second')")
    assertCommandOutput("echo histget('input', -1)", "second")
  }

  @Test
  fun `test histget returns empty string for empty input history`() {
    assertCommandOutput("echo string(histget('input'))", "''")
  }

  @Test
  fun `test histget with no index returns most recent entry`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histget('search')", "foo")
  }

  @Test
  fun `test histget with no index returns empty string when history is empty`() {
    assertCommandOutput("echo histget('search')", "")
  }
}
