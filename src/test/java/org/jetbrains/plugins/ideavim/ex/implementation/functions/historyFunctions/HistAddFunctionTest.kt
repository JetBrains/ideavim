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

class HistAddFunctionTest : VimTestCase("\n") {
  @Test
  fun `test histadd returns 1 on success`() {
    assertCommandOutput("echo histadd('cmd', 'echo hello')", "1")
  }

  @Test
  fun `test histadd returns 0 for unknown history type`() {
    assertCommandOutput("echo histadd('unknown', 'echo hello')", "0")
  }

  @Test
  fun `test histadd adds entry to cmd history using full name`() {
    enterCommand("call histadd('cmd', 'echo hello')")
    assertCommandOutput(
      "history cmd",
      """
        |      #  cmd history
        |      1  call histadd('cmd', 'echo hello')
        |      2  echo hello
        |>     3  history cmd
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to cmd history using colon shortcut`() {
    enterCommand("call histadd(':', 'echo hello')")
    assertCommandOutput(
      "history cmd",
      """
        |      #  cmd history
        |      1  call histadd(':', 'echo hello')
        |      2  echo hello
        |>     3  history cmd
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to cmd history using abbreviated name`() {
    enterCommand("call histadd('c', 'echo hello')")
    assertCommandOutput(
      "history cmd",
      """
        |      #  cmd history
        |      1  call histadd('c', 'echo hello')
        |      2  echo hello
        |>     3  history cmd
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to search history using full name`() {
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput(
      "history search",
      """
        |      #  search history
        |>     1  foo
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to search history using slash shortcut`() {
    enterCommand("call histadd('/', 'foo')")
    assertCommandOutput(
      "history search",
      """
        |      #  search history
        |>     1  foo
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to search history using abbreviated name`() {
    enterCommand("call histadd('s', 'foo')")
    assertCommandOutput(
      "history search",
      """
        |      #  search history
        |>     1  foo
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to expr history using full name`() {
    enterCommand("call histadd('expr', '1 + 1')")
    assertCommandOutput(
      "history =",
      """
        |      #  expr history
        |>     1  1 + 1
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to expr history using equals shortcut`() {
    enterCommand("call histadd('=', '1 + 1')")
    assertCommandOutput(
      "history =",
      """
        |      #  expr history
        |>     1  1 + 1
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to expr history using abbreviated name`() {
    enterCommand("call histadd('e', '1 + 1')")
    assertCommandOutput(
      "history =",
      """
        |      #  expr history
        |>     1  1 + 1
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to input history using full name`() {
    enterCommand("call histadd('input', 'my input')")
    assertCommandOutput(
      "history @",
      """
        |      #  input history
        |>     1  my input
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to input history using at shortcut`() {
    enterCommand("call histadd('@', 'my input')")
    assertCommandOutput(
      "history @",
      """
        |      #  input history
        |>     1  my input
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds entry to input history using abbreviated name`() {
    enterCommand("call histadd('i', 'my input')")
    assertCommandOutput(
      "history @",
      """
        |      #  input history
        |>     1  my input
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd does not add duplicate entry to search history`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'foo')")
    assertCommandOutput(
      "history search",
      """
        |      #  search history
        |>     2  foo
      """.trimMargin()
    )
  }

  @Test
  fun `test histadd adds multiple distinct entries to search history`() {
    enterCommand("call histadd('search', 'foo')")
    enterCommand("call histadd('search', 'bar')")
    assertCommandOutput(
      "history search",
      """
        |      #  search history
        |      1  foo
        |>     2  bar
      """.trimMargin()
    )
  }
}
