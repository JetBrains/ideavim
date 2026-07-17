/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class LmapInsertModeTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test iminsert option defaults to 0`() {
    configureByText("\n")
    assertCommandOutput("set iminsert?", "  iminsert=0")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmap applies to typed text in insert mode when iminsert is on`() {
    configureByText("${c}\n")
    enterCommand("lmap a b")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("a")
    assertMode(Mode.INSERT)
    assertState("b\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmap applies to typed text in replace mode when iminsert is on`() {
    // Replace mode is an Insert variant; Vim applies language mappings there too.
    configureByText("${c}x\n")
    enterCommand("lmap a b")
    enterCommand("set iminsert=1")
    typeText("R")
    typeText("a")
    assertState("b\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmap does not apply to typed text when iminsert is off`() {
    configureByText("${c}\n")
    enterCommand("lmap a b")
    // iminsert defaults to 0, so the language mapping is inactive.
    typeText("i")
    typeText("a")
    assertState("a\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.NEOVIM_RPC_SPECIAL_KEYS_INSERT_MODE)
  @Test
  fun `test ctrl-caret toggles language input mode in insert mode`() {
    configureByText("${c}\n")
    enterCommand("lmap a b")
    typeText("i")
    // Ctrl-^ turns 'iminsert' on, activating the language mapping.
    typeText("<C-^>")
    typeText("a")
    assertState("b\n")
  }

  // --- Finding #3: recursion handling of the re-fed RHS ---

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lnoremap does not remap its rhs`() {
    // :lnoremap is non-recursive: the 'b' it produces must not be re-mapped by :lmap b c.
    configureByText("${c}\n")
    enterCommand("lnoremap a b")
    enterCommand("lmap b c")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("a")
    assertState("b\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test recursive lmap whose rhs begins with lhs does not loop`() {
    // :lmap a ab is recursive, but Vim protects the leading 'a' of the rhs from being re-mapped,
    // so this inserts "ab" rather than looping until 'maxmapdepth'.
    configureByText("${c}\n")
    enterCommand("lmap a ab")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("a")
    assertState("ab\n")
  }

  // --- Coverage gap #1: case-sensitive (uppercase/shifted) mappings, as real keymaps rely on ---

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmap distinguishes uppercase and lowercase mappings`() {
    // russian-jcukenwin maps both 'f' and 'F' to different glyphs, so case must be preserved.
    configureByText("${c}\n")
    enterCommand("lmap f x")
    enterCommand("lmap F Y")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("f")
    typeText("F")
    assertState("xY\n")
  }

  // --- Coverage gap #2: an unmapped char must still be inserted while iminsert is on ---

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test unmapped char is inserted verbatim when iminsert is on`() {
    configureByText("${c}\n")
    enterCommand("lmap a b")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("z")
    assertState("z\n")
  }

  // --- Coverage gap #3: iminsert is local to buffer, not global ---

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test iminsert is local to buffer`() {
    configureByText("\n")
    enterCommand("setlocal iminsert=1")
    assertCommandOutput("set iminsert?", "  iminsert=1")
    // A local change must not leak into the global value used to seed other buffers.
    assertCommandOutput("setglobal iminsert?", "  iminsert=0")
  }

  // --- Coverage gap #4: :lmap is applied before :imap ---

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmap is applied before imap`() {
    // :help language-mapping - language mappings are used for typed characters, so the typed 'a'
    // is converted to 'b' by :lmap before :imap is consulted; the imap on 'a' never fires.
    configureByText("${c}\n")
    enterCommand("imap a x")
    enterCommand("lmap a b")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("a")
    assertState("b\n")
  }

  // --- Coverage gap #5: multi-character lhs (to be implemented) ---

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test lmap with multi-character lhs`() {
    configureByText("${c}\n")
    enterCommand("lmap ab c")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("ab")
    assertState("c\n")
  }
}
