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
 * `:cla[st] [nr]` / `:[nr]cla[st]` jumping to item [nr] of the quickfix list that `:Ggrep`
 * fills, or to the last item without [nr]:
 *
 *  - [nr] past the end jumps to the last item
 *  - on the last item it jumps again and prints the message, without an error
 *  - `E42` when the list is empty
 *  - every jump prints `(N of M): text` and is recorded in the jump list
 *
 * Tests use [grepFiveMatches], which lands on its first match, in `aaa.txt`.
 */
class FugitiveClastTest : FugitiveTestCase() {

  @Test
  fun `test clast jumps to last match`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("clast")

    assertCurrentFile("c.txt")
    assertCurrentPosition(0, 2)
  }

  @Test
  fun `test cla is an abbreviation of clast`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("cla")

    assertCurrentFile("c.txt")
    assertCurrentPosition(0, 2)
  }

  @Test
  fun `test clast with bang jumps to last match`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("clast!")

    assertCurrentFile("c.txt")
  }

  @Test
  fun `test clast with number argument jumps to that match`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("clast 4")

    assertCurrentFile("b.txt")
    assertCurrentPosition(1, 0)
  }

  @Test
  fun `test clast with count jumps to that match`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("4clast")

    assertCurrentFile("b.txt")
    assertCurrentPosition(1, 0)
  }

  @Test
  fun `test clast with number past the end jumps to last match`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("clast 99")

    assertCurrentFile("c.txt")
    assertCurrentPosition(0, 2)
    assertPluginError(false)
  }

  @Test
  fun `test clast on last match prints position without error`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("clast")

    enterCommandInCurrentEditor("clast")

    assertPluginError(false)
    assertCurrentFile("c.txt")
    assertEquals("(5 of 5): x foo c", VimPlugin.getMessage())
  }

  @Test
  fun `test clast prints position in list and match text`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("clast")

    assertEquals("(5 of 5): x foo c", VimPlugin.getMessage())
  }

  @Test
  fun `test clast saves jump location`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("clast")
    typeInCurrentEditor("<C-o>")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cprev after clast continues from last match`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("clast")

    enterCommandInCurrentEditor("cprev")

    assertCurrentFile("b.txt")
    assertCurrentPosition(1, 0)
  }

  @Test
  fun `test clast without Ggrep reports no errors`() {
    configureByText("${c}foo")

    enterCommand("clast")

    assertPluginError(true)
    assertPluginErrorMessage("E42: No Errors")
  }
}
