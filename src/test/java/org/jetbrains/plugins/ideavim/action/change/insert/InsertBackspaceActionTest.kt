/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertBackspaceActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NEOVIM_RPC_SPECIAL_KEYS_INSERT_MODE)
  @Test
  fun `test insert backspace`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}und it in a legendary land"
    configureByText(before)

    typeText("i", "<BS>")

    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test insert backspace scrolls start of line`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")

    typeText("70zl", "i", "<BS>")

    // Note that because 'sidescroll' has the default value of 0, we scroll the caret to the middle of the screen, as
    // well as applying sidescrolloff. Leftmost column was 69 (zero-based), and the caret is on column 80. Deleting a
    // character moves the caret to column 79, which is within 'sidescrolloff' of the left edge of the screen. The
    // screen is scrolled by 'sidescroll', which has the default value of 0, so we scroll until the caret is in the
    // middle of the screen, which is 80 characters wide: 79-(80/2)=39
    assertVisibleLineBounds(0, 39, 118)
  }

  // Replace mode backspace. Vim keeps a stack of the characters that each typed character replaced, and backspace pops
  // it to put the original text back. See `replace_do_bs` in Vim's edit.c:
  //   - the character was replaced -> put the original character back
  //   - the character was inserted (typed past the end of the line) -> delete it
  //   - the stack is empty -> only move the caret
  // These tests use <C-H> rather than <BS> so that they can be verified against Neovim - <BS> uses Neovim's termcode
  // format, which the RPC layer used for testing cannot send in insert mode (see NEOVIM_RPC_SPECIAL_KEYS_INSERT_MODE).

  @Test
  fun `test replace mode backspace restores all replaced characters`() {
    // "mushroom" is longer than "grzyb", so its last three characters are appended instead of replacing anything. The
    // first three backspaces delete those, the remaining five put the original characters back.
    doTest(
      listOf("R", "mushroom", "<C-H>".repeat(8)),
      "${c}grzyb",
      "${c}grzyb",
      Mode.REPLACE,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.NEOVIM_RPC_SPECIAL_KEYS_INSERT_MODE)
  @Test
  fun `test replace mode backspace restores all replaced characters with BS key`() {
    doTest(
      listOf("R", "mushroom", "<BS>".repeat(8)),
      "${c}grzyb",
      "${c}grzyb",
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace restores a character replaced at the start of the line`() {
    doTest(
      listOf("R", "m", "<C-H>"),
      "${c}grzyb",
      "${c}grzyb",
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace restores characters one at a time`() {
    doTest(
      listOf("R", "mushroom", "<C-H>".repeat(5)),
      "${c}grzyb",
      "mus${c}yb",
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace deletes characters appended past the end of the line`() {
    doTest(
      listOf("R", "mushroom", "<C-H>".repeat(3)),
      "${c}grzyb",
      "mushr${c}",
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace restores a character replaced away from the start of the line`() {
    doTest(
      listOf("ll", "R", "m", "<C-H>"),
      "${c}grzyb",
      "gr${c}zyb",
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace restores text typed by an earlier replacement`() {
    // Escape leaves the caret on the "Y", so "ll" moves onto the "y" of the original text. Each replacement only knows
    // about the characters it typed over itself, so this restores that "y" rather than anything the first one replaced.
    doTest(
      listOf("R", "XY", "<Esc>", "ll", "R", "Z", "<C-H>"),
      "${c}grzyb",
      "XYz${c}yb",
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace with nothing replaced only moves the caret`() {
    // Nothing has been typed, so there is nothing on the replace stack. Vim only moves the caret - the character it
    // moves over was never replaced and must be left alone.
    doTest(
      listOf("l", "R", "<C-H>"),
      "${c}grzyb",
      "${c}grzyb",
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace at the start of a line only moves the caret`() {
    // The line the replacement started on is never joined with the previous one - Vim just moves the caret back.
    doTest(
      listOf("j", "R", "<C-H>"),
      """
        ab
        ${c}cd
      """.trimIndent(),
      """
        ab${c}
        cd
      """.trimIndent(),
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace does not restore before the start of the replacement`() {
    // Replace mode was entered on the third character, so the third backspace has nothing left to restore and only
    // moves the caret. It must not touch the two characters that were never replaced.
    doTest(
      listOf("ll", "R", "mu", "<C-H>".repeat(3)),
      "${c}grzyb",
      "g${c}rzyb",
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace records replaced characters again after restoring them`() {
    doTest(
      listOf("R", "mu", "<C-H>".repeat(2), "xy"),
      "${c}grzyb",
      "xy${c}zyb",
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode backspace joins the lines split by enter`() {
    // Enter in Replace mode splits the line without consuming a character, so backspacing over it joins the lines back
    // together, and the characters replaced on each line are still restored.
    doTest(
      listOf("ll", "R", "<CR>", "x", "<CR>", "y", "<C-H>".repeat(5)),
      "${c}grzyb",
      "g${c}rzyb",
      Mode.REPLACE,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.NEOVIM_RPC_SPECIAL_KEYS_INSERT_MODE)
  @Test
  fun `test replace mode backspace after caret movement does not restore`() {
    // Moving the caret restarts the replacement, so backspacing before the new start point only moves the caret.
    doTest(
      listOf("R", "mush", "<Left>", "<C-H>"),
      "${c}grzyb",
      "mu${c}shb",
      Mode.REPLACE,
    )
  }
}
