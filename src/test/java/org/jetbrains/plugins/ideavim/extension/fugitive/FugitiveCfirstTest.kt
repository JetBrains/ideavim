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
 * `:cfir[st] [nr]` / `:[nr]cfir[st]` / `:cr[ewind]` jumping to item [nr] of the quickfix list
 * that `:Ggrep` fills, or to the first item without [nr]:
 *
 *  - [nr] past the end jumps to the last item
 *  - on the first item it jumps again and prints the message, without an error
 *  - `E42` when the list is empty
 *  - every jump prints `(N of M): text` and is recorded in the jump list
 *
 * Tests use [grepFiveMatches] and start from its last match, in `c.txt`.
 */
class FugitiveCfirstTest : FugitiveTestCase() {

  @Test
  fun `test cfirst jumps to first match`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cfirst")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cfir is an abbreviation of cfirst`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cfir")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test crewind is an alias of cfirst`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("crewind")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cfirst with bang jumps to first match`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cfirst!")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cfirst with number argument jumps to that match`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cfirst 3")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cfirst with count jumps to that match`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("3cfirst")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cfirst with number past the end jumps to last match`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("cfirst 99")

    assertCurrentFile("c.txt")
    assertCurrentPosition(0, 2)
    assertPluginError(false)
  }

  @Test
  fun `test cfirst on first match prints position without error`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("cfirst")

    assertPluginError(false)
    assertCurrentPosition(0, 6)
    assertEquals("(1 of 5): first foo", VimPlugin.getMessage())
  }

  @Test
  fun `test cfirst prints position in list and match text`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cfirst 2")

    // The leading whitespace of the text is skipped
    assertEquals("(2 of 5): third foo", VimPlugin.getMessage())
  }

  @Test
  fun `test cfirst saves jump location`() {
    grepFiveMatchesAndGoToLast()

    enterCommandInCurrentEditor("cfirst")
    typeInCurrentEditor("<C-o>")

    assertCurrentFile("c.txt")
    assertCurrentPosition(0, 2)
  }

  @Test
  fun `test cnext after cfirst with number continues from that match`() {
    grepFiveMatchesAndGoToLast()
    enterCommandInCurrentEditor("cfirst 2")

    enterCommandInCurrentEditor("cnext")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cfirst without Ggrep reports no errors`() {
    configureByText("${c}foo")

    enterCommand("cfirst")

    assertPluginError(true)
    assertPluginErrorMessage("E42: No Errors")
  }

  private fun grepFiveMatchesAndGoToLast() {
    grepFiveMatches()
    enterCommandInCurrentEditor("4cnext")
    assertCurrentFile("c.txt")
  }
}
