/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.fugitive

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.group.git.GitExecutableStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `:[count]cn[ext]` walking the quickfix list that `:Ggrep` fills, following Vim's
 * `get_nth_valid_entry` in `quickfix.c`:
 *
 *  - it moves by list position, not by cursor position
 *  - a count larger than the rest of the list stops at the last entry
 *  - `E553` only when not even one step is possible, `E42` when the list is empty
 *  - every jump prints `(N of M): text` and is recorded in the jump list
 */
class FugitiveCnextTest : FugitiveTestCase() {

  @Test
  fun `test cnext jumps to second match`() {
    grepThreeMatches()

    enterCommandInCurrentEditor("cnext")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cnext jumps to match in another file`() {
    grepThreeMatches()

    enterCommandInCurrentEditor("cnext")
    enterCommandInCurrentEditor("cnext")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cn is an abbreviation of cnext`() {
    grepThreeMatches()

    enterCommandInCurrentEditor("cn")

    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cnext with count skips matches`() {
    grepThreeMatches()

    enterCommandInCurrentEditor("2cnext")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cnext with count past the end stops at last match`() {
    grepThreeMatches()

    enterCommandInCurrentEditor("5cnext")

    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
    assertPluginError(false)
  }

  @Test
  fun `test cnext on last match reports no more items`() {
    grepThreeMatches()
    enterCommandInCurrentEditor("2cnext")

    enterCommandInCurrentEditor("cnext")

    assertPluginError(true)
    assertPluginErrorMessage("E553: No more items")
    assertCurrentFile("b.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test cnext follows the list, not the cursor`() {
    grepThreeMatches()
    typeInCurrentEditor("G$")

    enterCommandInCurrentEditor("cnext")

    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cnext prints position in list and match text`() {
    grepThreeMatches()

    enterCommandInCurrentEditor("cnext")

    // Vim skips the leading whitespace of the text
    assertEquals("(2 of 3): third foo", VimPlugin.getMessage())
  }

  @Test
  fun `test cnext saves jump location`() {
    grepThreeMatches()

    enterCommandInCurrentEditor("cnext")
    typeInCurrentEditor("<C-o>")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
  }

  @Test
  fun `test cnext after Ggrep with bang jumps to second match`() {
    configureByText(THREE_MATCHES_TEXT)
    addFile("b.txt", "foo in b\n")
    git.respondWith(*THREE_MATCHES_OUTPUT)
    enterCommand("Ggrep! foo")

    enterCommandInCurrentEditor("cnext")

    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test new Ggrep replaces the list`() {
    grepThreeMatches()
    enterCommandInCurrentEditor("cnext")
    git.respondWith("aaa.txt:2:1:second line", "aaa.txt:3:3:  third foo")
    enterCommandInCurrentEditor("Ggrep line")

    enterCommandInCurrentEditor("cnext")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 2)
    assertEquals("(2 of 2): third foo", VimPlugin.getMessage())
  }

  @Test
  fun `test cnext with bang jumps to second match`() {
    grepThreeMatches()

    enterCommandInCurrentEditor("cnext!")

    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cnext keeps trailing whitespace of match text`() {
    configureByText(THREE_MATCHES_TEXT)
    git.respondWith("aaa.txt:1:7:first foo", "aaa.txt:3:5:  third foo  ")
    enterCommand("Ggrep foo")

    enterCommandInCurrentEditor("cnext")

    assertEquals("(2 of 2): third foo  ", VimPlugin.getMessage())
  }

  @Test
  fun `test failed Ggrep keeps the previous list`() {
    grepThreeMatches()
    git.executableStatus = GitExecutableStatus.NOT_FOUND
    enterCommandInCurrentEditor("Ggrep bar")

    enterCommandInCurrentEditor("cnext")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test cnext does not print position when the file cannot be opened`() {
    configureByText(THREE_MATCHES_TEXT)
    git.respondWith("aaa.txt:1:7:first foo", "deleted.txt:1:1:foo")
    enterCommand("Ggrep foo")

    enterCommandInCurrentEditor("cnext")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 6)
    assertNull(VimPlugin.getMessage())
  }

  @Test
  fun `test cnext without Ggrep reports no errors`() {
    configureByText("${c}foo")

    enterCommand("cnext")

    assertPluginError(true)
    assertPluginErrorMessage("E42: No Errors")
  }

  @Test
  fun `test cnext after Ggrep without matches reports no errors`() {
    configureByText("${c}foo")
    git.respondWith()
    enterCommand("Ggrep bar")

    enterCommand("cnext")

    assertPluginError(true)
    assertPluginErrorMessage("E42: No Errors")
  }

  /**
   * Greps three matches, two in the current file and one in `b.txt`, and lands on the first.
   */
  private fun grepThreeMatches() {
    configureByText(THREE_MATCHES_TEXT)
    addFile("b.txt", "foo in b\n")
    git.respondWith(*THREE_MATCHES_OUTPUT)
    enterCommand("Ggrep foo")
    assertCurrentPosition(0, 6)
  }

  private companion object {
    val THREE_MATCHES_TEXT = """
      |${c}first foo
      |second line
      |  third foo
    """.trimMargin()

    val THREE_MATCHES_OUTPUT = arrayOf(
      "aaa.txt:1:7:first foo",
      "aaa.txt:3:5:  third foo",
      "b.txt:1:1:foo in b",
    )
  }
}
