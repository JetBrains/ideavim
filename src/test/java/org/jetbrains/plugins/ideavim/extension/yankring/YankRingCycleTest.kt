/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.yankring

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for replacing the text of the last paste with an older ring entry, YankRing's `<C-P>` and
 * `<C-N>` (VIM-301, group B of the roadmap).
 *
 * The yank ring is a port of YankRing.vim, so there is nothing to compare against in Neovim.
 */
class YankRingCycleTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("yankring")
  }

  /**
   * Leaves the ring holding "three", "two", "one" - newest first - and the caret on an empty second
   * line, ready to paste into.
   */
  private fun configureRingOfThree() {
    configureByText("${c}one two three\n")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("wyiw")
    typeText("wyiw")
    typeText("j")
  }

  // B1
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test ctrl-p replaces the paste with the previous entry`() {
    configureRingOfThree()
    typeText("p")
    assertState("one two three\nthre${c}e")

    typeText("<C-P>")
    assertState("one two three\ntw${c}o")
  }

  // B2
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test repeated ctrl-p walks further back`() {
    configureRingOfThree()
    typeText("p")
    typeText("<C-P>")
    typeText("<C-P>")
    assertState("one two three\non${c}e")
  }

  // B3
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test ctrl-n walks forward again`() {
    configureRingOfThree()
    typeText("p")
    typeText("<C-P>")
    typeText("<C-P>")
    typeText("<C-N>")
    assertState("one two three\ntw${c}o")
  }

  // B4
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test cycling wraps around past the oldest entry`() {
    configureRingOfThree()
    typeText("p")
    typeText("<C-P>")
    typeText("<C-P>")
    typeText("<C-P>")
    assertState("one two three\nthre${c}e")
  }

  // B5
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test count on ctrl-p skips entries`() {
    configureRingOfThree()
    typeText("p")
    typeText("2<C-P>")
    assertState("one two three\non${c}e")
  }

  // B7 - the mapping takes the keys over completely, so they must not fall back to `k` / `j`
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test ctrl-p without a pending paste reports there is nothing to replace`() {
    configureByText("one\n${c}two")
    enterCommand("YRClear")

    typeText("<C-P>")
    assertState("one\n${c}two")
    assertPluginError(true)
    assertPluginErrorMessage("YR: You must paste text first, before you can replace")
  }

  // B7
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test ctrl-n without a pending paste reports there is nothing to replace`() {
    configureByText("${c}one\ntwo")
    enterCommand("YRClear")

    typeText("<C-N>")
    assertState("${c}one\ntwo")
    assertPluginError(true)
  }

  // B8
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test ctrl-p after an intervening edit does not cycle`() {
    configureRingOfThree()
    typeText("p")
    typeText("A!<Esc>")

    typeText("<C-P>")
    assertState("one two three\nthree${c}!")
    assertPluginError(true)
  }

  // B9 - `s:YRReplace` refuses unless the caret is still on the line holding the '[ mark
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test ctrl-p away from the pasted line does not cycle`() {
    configureRingOfThree()
    typeText("p")
    typeText("k")

    typeText("<C-P>")
    assertState("one ${c}two three\nthree")
    assertPluginError(true)
  }

  /**
   * Yank four whole lines, paste onto a fifth, then cycle. The replacement has to stay on the
   * pasted line: an earlier version moved the caret and rewrote one of the source lines instead.
   */
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test cycling line-wise pastes leaves the source lines alone`() {
    configureByText("${c}one\ntwo\nthree\nfour")
    enterCommand("YRClear")
    typeText("yy")
    typeText("jyy")
    typeText("jyy")
    typeText("jyy")

    typeText("G")
    typeText("p")
    assertState("one\ntwo\nthree\nfour\n${c}four\n")

    typeText("<C-P>")
    assertState("one\ntwo\nthree\nfour\n${c}three\n")

    typeText("<C-P>")
    assertState("one\ntwo\nthree\nfour\n${c}two\n")
  }

  /**
   * Cycling between a character-wise and a line-wise entry changes the line count, because the
   * line-wise entry takes a whole line of its own. The caret has to stay on the replaced text
   * either way.
   */
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test cycling between character-wise and line-wise entries with paste before`() {
    configureByText("${c}one\ntwo\nthree\nfour")
    enterCommand("YRClear")
    typeText("yy")
    typeText("jyy")
    typeText("gg")
    typeText("yiw")

    typeText("G")
    typeText("P")
    assertState("one\ntwo\nthree\non${c}efour")

    // "two\n" is line-wise, so it lands on a line of its own above the caret's line
    typeText("<C-P>")
    assertState("one\ntwo\nthree\n${c}two\nfour")

    // ... and stepping back to the character-wise entry takes that line away again. This is where
    // relying on undo to restore the caret used to insert the text inside the neighbouring word.
    typeText("<C-N>")
    assertState("one\ntwo\nthree\non${c}efour")
  }

  // B10
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test undo after cycling restores the state before the paste`() {
    configureRingOfThree()
    typeText("p")
    typeText("<C-P>")
    typeText("<C-P>")

    typeText("u")
    assertState("one two three\n$c")
  }

  // B11
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test cycling does not clobber the register that was pasted from`() {
    configureByText("${c}one two three\n")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("w\"ayiw")
    typeText("j")

    typeText("\"ap")
    assertState("one two three\ntw${c}o")

    typeText("<C-P>")
    assertState("one two three\non${c}e")
    assertRegister('a', "two")
  }
}
