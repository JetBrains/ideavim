/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.fugitive

import com.maddyhome.idea.vim.VimPlugin
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * `:[count]cnf[ile]` jumping to the first match in the next file of the quickfix list that
 * `:Ggrep` fills:
 *
 *  - it skips the rest of the matches in the current file
 *  - a count larger than the remaining files stops at the first match of the last file
 *  - `E553` only when already in the last file, `E42` when the list is empty
 *  - every jump prints `(N of M): text` and is recorded in the jump list
 *
 * Most tests use [grepFiveMatches]: two in `aaa.txt`, two in `b.txt`, one in `c.txt`.
 */
class FugitiveCnfileTest : FugitiveTestCase() {

  @Test
  fun `test cnfile jumps to first match in next file`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("cnfile")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cnfile skips the rest of the current file`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("cnext")

    enterCommandInCurrentEditor("cnfile")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cnfile from the middle of a file goes to the first match of the next one`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("2cnext")
    assertCurrentFile("b.txt")

    enterCommandInCurrentEditor("cnfile")

    assertCurrentFile("c.txt")
    assertCurrentPosition(0, 2)
  }

  @Test
  fun `test cnf is an abbreviation of cnfile`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("cnf")

    assertCurrentFile("b.txt")
  }

  @Test
  fun `test cnfile with count skips files`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("2cnfile")

    assertCurrentFile("c.txt")
    assertCurrentPosition(0, 2)
  }

  @Test
  fun `test cnfile with count past the end stops at last file`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("5cnfile")

    assertCurrentFile("c.txt")
    assertCurrentPosition(0, 2)
    assertPluginError(false)
  }

  @Test
  fun `test cnfile in last file reports no more items`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("2cnfile")

    enterCommandInCurrentEditor("cnfile")

    assertPluginError(true)
    assertPluginErrorMessage("E553: No more items")
    assertCurrentFile("c.txt")
  }

  @Test
  fun `test cnfile in last file with matches left reports no more items`() {
    grepLastFileWithTwoMatches()
    enterCommandInCurrentEditor("cnfile")

    enterCommandInCurrentEditor("cnfile")

    assertPluginError(true)
    assertPluginErrorMessage("E553: No more items")
    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cnfile with count past the end stops at first match of last file`() {
    grepLastFileWithTwoMatches()

    enterCommandInCurrentEditor("5cnfile")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
    assertPluginError(false)
  }

  @Test
  fun `test cnfile with count in last file reports no more items`() {
    grepLastFileWithTwoMatches()
    enterCommandInCurrentEditor("cnfile")

    enterCommandInCurrentEditor("3cnfile")

    assertPluginError(true)
    assertPluginErrorMessage("E553: No more items")
    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cnfile with bang jumps to next file`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("cnfile!")

    assertCurrentFile("b.txt")
  }

  @Test
  fun `test cnfile prints position in list and match text`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("cnfile")

    assertEquals("(3 of 5): foo b1", VimPlugin.getMessage())
  }

  @Test
  fun `test cnfile saves jump location`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("cnfile")
    typeInCurrentEditor("<C-o>")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cnfile without Ggrep reports no errors`() {
    configureByText("${c}foo")

    enterCommand("cnfile")

    assertPluginError(true)
    assertPluginErrorMessage("E42: No Errors")
  }

  /**
   * Greps one match in `aaa.txt` and two in `b.txt`, the last file, and lands on the first.
   */
  private fun grepLastFileWithTwoMatches() {
    configureByText("${c}foo a")
    addFile("b.txt", "foo b1\nfoo b2\n")
    git.respondWith("aaa.txt:1:1:foo a", "b.txt:1:1:foo b1", "b.txt:2:1:foo b2")
    enterCommand("Ggrep foo")
  }
}
