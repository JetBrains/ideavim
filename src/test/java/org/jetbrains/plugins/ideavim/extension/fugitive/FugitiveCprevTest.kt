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
 * `:[count]cp[revious]` / `:[count]cN[ext]` walking back the quickfix list that `:Ggrep` fills.
 * It mirrors `:cnext`:
 *
 *  - a count larger than the items before the current one stops at the first item
 *  - `E553` only when the current item already is the first one, `E42` when the list is empty
 *  - every jump prints `(N of M): text` and is recorded in the jump list
 */
class FugitiveCprevTest : FugitiveTestCase() {

  @Test
  fun `test cprev jumps to previous match`() {
    grepThreeMatchesAndGoToSecond()

    enterCommandInCurrentEditor("cprev")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cprev jumps back to another file`() {
    grepThreeMatchesAndGoToLast()

    enterCommandInCurrentEditor("cprev")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cp is an abbreviation of cprevious`() {
    grepThreeMatchesAndGoToSecond()

    enterCommandInCurrentEditor("cp")

    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cprevious is the full name`() {
    grepThreeMatchesAndGoToSecond()

    enterCommandInCurrentEditor("cprevious")

    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cN is an alias of cprevious`() {
    grepThreeMatchesAndGoToSecond()

    enterCommandInCurrentEditor("cN")

    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cNext is an alias of cprevious`() {
    grepThreeMatchesAndGoToSecond()

    enterCommandInCurrentEditor("cNext")

    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cprev with count skips matches`() {
    grepThreeMatchesAndGoToLast()

    enterCommandInCurrentEditor("2cprev")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cprev with count past the start stops at first match`() {
    grepThreeMatchesAndGoToLast()

    enterCommandInCurrentEditor("5cprev")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
    assertPluginError(false)
  }

  @Test
  fun `test cprev on first match reports no more items`() {
    grepThreeMatches()

    enterCommandInCurrentEditor("cprev")

    assertPluginError(true)
    assertPluginErrorMessage("E553: No more items")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cprev with bang jumps to previous match`() {
    grepThreeMatchesAndGoToSecond()

    enterCommandInCurrentEditor("cprev!")

    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cprev follows the list, not the cursor`() {
    grepThreeMatchesAndGoToSecond()
    typeInCurrentEditor("gg")

    enterCommandInCurrentEditor("cprev")

    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cprev prints position in list and match text`() {
    grepThreeMatchesAndGoToLast()

    enterCommandInCurrentEditor("cprev")

    // Vim skips the leading whitespace of the text
    assertEquals("(2 of 3): third foo", VimPlugin.getMessage())
  }

  @Test
  fun `test cprev saves jump location`() {
    grepThreeMatchesAndGoToLast()

    enterCommandInCurrentEditor("cprev")
    typeInCurrentEditor("<C-o>")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cprev without Ggrep reports no errors`() {
    configureByText("${c}foo")

    enterCommand("cprev")

    assertPluginError(true)
    assertPluginErrorMessage("E42: No Errors")
  }

  /**
   * Greps three matches, two in the current file and one in `b.txt`, and lands on the first.
   */
  private fun grepThreeMatches() {
    configureByText(
      """
        |${c}first foo
        |second line
        |  third foo
      """.trimMargin()
    )
    addFile("b.txt", "foo in b\n")
    git.respondWith("aaa.txt:1:7:first foo", "aaa.txt:3:5:  third foo", "b.txt:1:1:foo in b")
    enterCommand("Ggrep foo")
    assertCurrentPosition(0, 6)
  }

  private fun grepThreeMatchesAndGoToSecond() {
    grepThreeMatches()
    enterCommandInCurrentEditor("cnext")
    assertCurrentPosition(2, 4)
  }

  private fun grepThreeMatchesAndGoToLast() {
    grepThreeMatches()
    enterCommandInCurrentEditor("2cnext")
    assertCurrentFile("b.txt")
  }
}
