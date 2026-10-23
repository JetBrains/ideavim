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
 * `:[count]cpf[ile]` / `:[count]cNf[ile]` jumping to the *last* match in the previous file of the
 * quickfix list that `:Ggrep` fills:
 *
 *  - it skips the rest of the matches in the current file, so from `b2` it lands on `a2`, not `b1`
 *  - a count larger than the previous files stops at the last match of the first file
 *  - `E553` only when already in the first file, even with matches before the current one
 *  - every jump prints `(N of M): text` and is recorded in the jump list
 *
 * Tests use [grepFiveMatches] and start from its last match, in `c.txt`.
 */
class FugitiveCpfileTest : FugitiveTestCase() {

  @Test
  fun `test cpfile jumps to last match in previous file`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cpfile")

    assertCurrentFile("b.txt")
    assertCurrentPosition(1, 0)
  }

  @Test
  fun `test cpfile skips the rest of the current file`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("3cnext")
    assertCurrentPosition(1, 0)

    enterCommandInCurrentEditor("cpfile")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cpfile from the first match of a file goes to the last match of the previous one`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("2cnext")
    assertCurrentFile("b.txt")

    enterCommandInCurrentEditor("cpfile")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cpf is an abbreviation of cpfile`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cpf")

    assertCurrentFile("b.txt")
    assertCurrentPosition(1, 0)
  }

  @Test
  fun `test cNfile is an alias of cpfile`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cNfile")

    assertCurrentFile("b.txt")
    assertCurrentPosition(1, 0)
  }

  @Test
  fun `test cNf is an abbreviation of cNfile`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cNf")

    assertCurrentFile("b.txt")
    assertCurrentPosition(1, 0)
  }

  @Test
  fun `test cpfile with count skips files`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("2cpfile")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cpfile with count past the start stops at last match of first file`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("5cpfile")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
    assertPluginError(false)
  }

  @Test
  fun `test cpfile with count that can make only one step moves without error`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("2cnext")

    enterCommandInCurrentEditor("3cpfile")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
    assertPluginError(false)
  }

  @Test
  fun `test cpfile in first file with matches before reports no more items`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("cnext")

    enterCommandInCurrentEditor("cpfile")

    assertPluginError(true)
    assertPluginErrorMessage("E553: No more items")
    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cpfile with bang jumps to previous file`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cpfile!")

    assertCurrentFile("b.txt")
  }

  @Test
  fun `test cpfile prints position in list and match text`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cpfile")

    assertEquals("(4 of 5): foo b2", VimPlugin.getMessage())
  }

  @Test
  fun `test cpfile saves jump location`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cpfile")
    typeInCurrentEditor("<C-o>")

    assertCurrentFile("c.txt")
    assertCurrentPosition(0, 2)
  }

  @Test
  fun `test cpfile without Ggrep reports no errors`() {
    configureByText("${c}foo")

    enterCommand("cpfile")

    assertPluginError(true)
    assertPluginErrorMessage("E42: No Errors")
  }

  private fun grepFiveMatchesAndGoToLast() {
    grepFiveMatches()
    enterCommandInCurrentEditor("4cnext")
    assertCurrentFile("c.txt")
  }
}
