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

class HistDelFunctionTest : VimTestCase("\n") {
  @Test
  fun `test histdel returns 0 for unknown history type`() {
    assertCommandOutput("echo histdel('unknown', 1)", "0")
  }

  @Test
  fun `test histdel with no second argument clears entire search history`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'bar')")
    assertCommandOutput("echo histdel('search')", "1")
    assertCommandOutput("history search", "      #  search history")
  }

  @Test
  fun `test histdel with no second argument clears entire expr history`() {
    enterCommand("call histadd('expr', '1 + 1')")
    enterCommand("call histadd('expr', '2 + 2')")
    assertCommandOutput("echo histdel('expr')", "1")
    assertCommandOutput("history expr", "      #  expr history")
  }

  @Test
  fun `test histdel with no second argument clears entire input history`() {
    enterCommand("call histadd('input', 'first')")
    enterCommand("call histadd('input', 'second')")
    assertCommandOutput("echo histdel('input')", "1")
    assertCommandOutput("history input", "      #  input history")
  }

  @Test
  fun `test histdel with no second argument clears entire command history`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput("echo histdel('cmd')", "1")
    // Obviously, the command to output the history adds something to the history...
    assertCommandOutput("history cmd", """
      |      #  cmd history
      |>     1  history cmd
    """.trimMargin())
  }

  @Test
  fun `test histdel returns 1 when clearing non-empty history`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histdel('search')", "1")
  }

  @Test
  fun `test histdel returns 1 when clearing empty history`() {
    assertCommandOutput("echo histdel('search')", "1")
  }

  @Test
  fun `test histdel removes correct entry by positive index from search history`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'bar')")
    assertCommandOutput("echo histdel('search', 1)", "1")
    assertCommandOutput(
      "history search",
      """
      |      #  search history
      |>     2  bar
    """.trimMargin()
    )
  }

  @Test
  fun `test histdel removes no entries with invalid positive index from search history`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'bar')")
    enterCommand("call histadd('search', 'baz')")
    enterCommand("call histadd('search', 'bar')") // Moves 'bar' from #2 to #4
    assertCommandOutput(
      "history search",
      """
      |      #  search history
      |      1  foo
      |      3  baz
      |>     4  bar
    """.trimMargin()
    )
    assertCommandOutput("echo histdel('search', 2)", "0")
    assertCommandOutput(
      "history search",
      """
      |      #  search history
      |      1  foo
      |      3  baz
      |>     4  bar
    """.trimMargin()
    )
  }

  @Test
  fun `test histdel returns 1 when deleting existing entry by positive index`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histdel('search', 1)", "1")
  }

  @Test
  fun `test histdel returns 0 when deleting nonexistent entry by positive index`() {
    assertCommandOutput("echo histdel('search', 99)", "0")
  }

  @Test
  fun `test histdel removes most recent entry with index -1`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'bar')")
    assertCommandOutput("echo histdel('search', -1)", "1")
    assertCommandOutput(
      "history search", """
      |      #  search history
      |>     1  foo
    """.trimMargin()
    )
  }

  @Test
  fun `test histdel removes second-most-recent entry with index -2`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'bar')")
    assertCommandOutput("echo histdel('search', -2)", "1")
    assertCommandOutput(
      "history search", """
      |      #  search history
      |>     2  bar
    """.trimMargin()
    )
  }

  @Test
  fun `test histdel with index 0 incorrectly removes first entry`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'bar')")
    assertCommandOutput("echo histdel('search', 0)", "0")
    assertCommandOutput(
      "history search",
      """
      |      #  search history
      |      1  foo
      |>     2  bar
    """.trimMargin()
    )
  }

  @Test
  fun `test histdel returns 1 when deleting entry by negative index`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histdel('search', -1)", "1")
  }

  @Test
  fun `test histdel returns 0 when deleting by negative index from empty history`() {
    assertCommandOutput("echo histdel('search', -1)", "0")
  }

  @Test
  fun `test histdel with exact pattern removes matching entry`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histdel('search', 'foo')")
    assertCommandOutput("history search", "      #  search history")
  }

  @Test
  fun `test histdel with partial pattern removes matching entry`() {
    enterCommand("call histadd('search', 'foobar')")
    enterCommand("call histadd('search', 'baz')")
    // partial match: 'oo' should match 'foobar'
    assertCommandOutput("echo histdel('search', 'oo')", "1")
    assertCommandOutput(
      "history search", """
      |      #  search history
      |>     2  baz
    """.trimMargin()
    )
  }

  @Test
  fun `test histdel with pattern removes all matching entries`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'foobar')")
    enterCommand("call histadd('search', 'baz')")
    assertCommandOutput("echo histdel('search', 'foo')", "1")
    assertCommandOutput(
      "history search", """
      |      #  search history
      |>     3  baz
    """.trimMargin()
    )
  }

  @Test
  fun `test histdel with regex pattern removes matching entries`() {
    enterCommand("call histadd('search', 'abc')")
    enterCommand("call histadd('search', 'def')")
    enterCommand("call histadd('search', 'abx')")
    // 'ab.' matches 'abc' and 'abx'
    assertCommandOutput("echo histdel('search', 'ab.')", "1")
    assertCommandOutput(
      "history search", """
      |      #  search history
      |>     2  def
    """.trimMargin()
    )
  }

  @Test
  fun `test histdel with non-matching pattern leaves history unchanged`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histdel('search', 'xyz')", "0")
    assertCommandOutput(
      "history search", """
      |      #  search history
      |>     1  foo
    """.trimMargin()
    )
  }

  @Test
  fun `test histdel returns 1 when pattern matches an entry`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histdel('search', 'foo')", "1")
  }

  @Test
  fun `test histdel returns 0 when pattern matches no entries`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput("echo histdel('search', 'xyz')", "0")
  }

  @Test
  fun `test histdel works with search history using slash shortcut`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histdel('/', 1)")
    assertCommandOutput("history search", "      #  search history")
  }

  @Test
  fun `test histdel works with search history using abbreviated name`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histdel('s', 1)")
    assertCommandOutput("history search", "      #  search history")
  }

  @Test
  fun `test histdel works with expr history using full name`() {
    enterCommand("call histadd('expr', '1 + 1')")
    enterCommand("call histdel('expr', 1)")
    assertCommandOutput("history expr", "      #  expr history")
  }

  @Test
  fun `test histdel works with expr history using equals shortcut`() {
    enterCommand("call histadd('expr', '1 + 1')")
    enterCommand("call histdel('=', 1)")
    assertCommandOutput("history expr", "      #  expr history")
  }

  @Test
  fun `test histdel works with expr history using abbreviated name`() {
    enterCommand("call histadd('expr', '1 + 1')")
    enterCommand("call histdel('e', 1)")
    assertCommandOutput("history expr", "      #  expr history")
  }

  @Test
  fun `test histdel works with input history using full name`() {
    enterCommand("call histadd('input', 'my input')")
    enterCommand("call histdel('input', 1)")
    assertCommandOutput("history input", "      #  input history")
  }

  @Test
  fun `test histdel works with input history using at shortcut`() {
    enterCommand("call histadd('input', 'my input')")
    enterCommand("call histdel('@', 1)")
    assertCommandOutput("history input", "      #  input history")
  }

  @Test
  fun `test histdel works with input history using abbreviated name`() {
    enterCommand("call histadd('input', 'my input')")
    enterCommand("call histdel('i', 1)")
    assertCommandOutput("history input", "      #  input history")
  }

  @Test
  fun `test histdel works with cmd history using full name`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    // #1 = call histadd, #2 = echo hello, #3 = call histdel, #4 = history cmd
    assertCommandOutput("echo histdel('cmd', 2)", "1")
    assertCommandOutput("history cmd", """
      |      #  cmd history
      |      1  call histadd('cmd', 'echo hello')
      |      3  echo histdel('cmd', 2)
      |>     4  history cmd
    """.trimMargin())
  }

  @Test
  fun `test histdel works with cmd history using colon shortcut`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput("echo histdel(':', 2)", "1")
    assertCommandOutput("history cmd", """
      |      #  cmd history
      |      1  call histadd('cmd', 'echo hello')
      |      3  echo histdel(':', 2)
      |>     4  history cmd
    """.trimMargin())
  }

  @Test
  fun `test histdel works with cmd history using abbreviated name`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput("echo histdel('c', 2)", "1")
    assertCommandOutput("history cmd", """
      |      #  cmd history
      |      1  call histadd('cmd', 'echo hello')
      |      3  echo histdel('c', 2)
      |>     4  history cmd
    """.trimMargin())
  }
}
