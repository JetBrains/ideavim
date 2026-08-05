/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for `:YRReplace`
 *
 * This is the yank ring's real entry point: YankRing has no `<Plug>` layer, and binds the keys
 * straight onto the command.
 *
 * ```vim
 * nnoremap <silent> <C-P> :<C-U>YRReplace '-1', P<CR>
 * nnoremap <silent> <C-N> :<C-U>YRReplace '1', p<CR>
 * ```
 *
 * The offset is expressed against YankRing's oldest-first history, so `-1` means the older entry
 * and `1` the newer one - the opposite sign to our newest-first `YankRing.entries()`.
 */
class YRReplaceCommandTest : VimTestCase() {

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

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test negative offset steps to the older entry`() {
    configureRingOfThree()
    typeText("p")
    assertState("one two three\nthre${c}e")

    enterCommand("YRReplace -1 P")
    assertState("one two three\ntw${c}o")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test positive offset steps back to the newer entry`() {
    configureRingOfThree()
    typeText("p")
    enterCommand("YRReplace -1 P")
    assertState("one two three\ntw${c}o")

    enterCommand("YRReplace 1 p")
    assertState("one two three\nthre${c}e")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test offset larger than one skips entries`() {
    configureRingOfThree()
    typeText("p")

    enterCommand("YRReplace -2 P")
    assertState("one two three\non${c}e")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test offset wraps around past the oldest entry`() {
    configureRingOfThree()
    typeText("p")

    enterCommand("YRReplace -3 P")
    assertState("one two three\nthre${c}e")

    // Wrapping lands back on the text the paste already put there, so without this the test would
    // also pass while the command simply does not exist
    assertPluginError(false)
  }

  /**
   * `<f-args>` splits `'-1', P` into `'-1',` and `P`, and `s:YRPaste` digs the number out with
   * `matchstr(a:nextvalue, '-\?\d\+')`. Users copying the mapping out of the plugin's docs will
   * pass exactly this, so the quotes and the trailing comma have to be tolerated.
   */
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test quoted and comma separated arguments are accepted`() {
    configureRingOfThree()
    typeText("p")

    enterCommand("YRReplace '-1', P")
    assertState("one two three\ntw${c}o")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test the mapped keys and the command agree`() {
    configureRingOfThree()
    typeText("p")
    typeText("<C-P>")
    val afterMapping = fixture.editor.document.text

    configureRingOfThree()
    typeText("p")
    enterCommand("YRReplace -1 P")

    kotlin.test.assertEquals(afterMapping, fixture.editor.document.text)
  }

  /**
   * `s:YRPaste` declares `-nargs=*` but then reads `a:nextvalue` unconditionally, so the plugin
   * simply throws on a missing argument. We report instead of guessing a direction.
   */
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test replacing without an offset reports an error`() {
    configureRingOfThree()
    typeText("p")

    enterCommand("YRReplace")
    assertState("one two three\nthre${c}e")
    assertPluginError(true)
    assertPluginErrorMessage("YR: YRReplace needs an offset, for example :YRReplace -1 P")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test replacing with nothing pasted reports an error`() {
    configureByText("one\n${c}two")
    enterCommand("YRClear")

    enterCommand("YRReplace -1 P")
    assertState("one\n${c}two")
    assertPluginError(true)
    assertPluginErrorMessage("YR: You must paste text first, before you can replace")
  }
}
