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
 * Cycling has to keep working when the unnamed register is aliased onto the system clipboard
 * (VIM-4309): with `clipboard+=unnamed` a paste reads the clipboard rather than `"`, so loading a
 * ring entry into `"` alone leaves the buffer unchanged and `<C-P>` looks like it does nothing.
 *
 * The yank ring is a port of YankRing.vim, so there is nothing to compare against in Neovim.
 */
class YankRingClipboardTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("yankring")
  }

  /**
   * Leaves the ring holding "three", "two", "one" - newest first - and the caret on an empty second
   * line, ready to paste into.
   */
  private fun configureRingOfThree(vararg clipboardValues: String) {
    configureByText("${c}one two three\n")
    clipboardValues.forEach { enterCommand("set clipboard+=$it") }
    enterCommand("YRClear")
    typeText("yiw")
    typeText("wyiw")
    typeText("wyiw")
    typeText("j")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test ctrl-p cycles with clipboard unnamed`() {
    configureRingOfThree("unnamed")

    typeText("p")
    assertState("one two three\nthre${c}e")

    typeText("<C-P>")
    assertState("one two three\ntw${c}o")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test ctrl-p cycles with clipboard unnamedplus`() {
    configureRingOfThree("unnamedplus")

    typeText("p")
    assertState("one two three\nthre${c}e")

    typeText("<C-P>")
    assertState("one two three\ntw${c}o")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test repeated ctrl-p walks further back with clipboard unnamed`() {
    configureRingOfThree("unnamed", "unnamedplus")

    typeText("p")
    typeText("<C-P>")
    typeText("<C-P>")
    assertState("one two three\non${c}e")
  }

  /**
   * Cycling loads its entries into `"`, so it must not leave them behind in the clipboard the user
   * was pasting from: a fresh paste afterwards still puts the text that was yanked last.
   */
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test cycling does not clobber the clipboard that was pasted from`() {
    configureRingOfThree("unnamed")

    typeText("p")
    typeText("<C-P>")
    assertState("one two three\ntw${c}o")

    typeText("o<Esc>")
    typeText("p")
    assertState("one two three\ntwo\nthre${c}e")
  }

  /**
   * A paste over a selection is replayed through `gv`, which makes it the path most likely to lose
   * track of the register - so it gets its own clipboard case.
   */
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test a visual mode paste is cyclable with the clipboard aliased`() {
    configureByText("${c}one two three\nXXX")
    enterCommand("set clipboard+=unnamed")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("wyiw")
    typeText("j")

    typeText("viwp")
    assertState("one two three\ntw${c}o")

    typeText("<C-P>")
    assertState("one two three\non${c}e")
  }

  /**
   * An explicitly registered paste still replays from that register, and cycling must not spill the
   * ring entries into it - `'clipboard'` moves the default register, not the named ones.
   */
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test cycling from a named register with the clipboard aliased leaves it alone`() {
    configureByText("${c}one two three\n")
    enterCommand("set clipboard+=unnamed")
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

  /**
   * Cycling loads entries into a register, and every register write feeds the ring - so the listing
   * has to come back out in the order it went in, not reshuffled by the plugin's own bookkeeping.
   */
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test the ring keeps its order after cycling with the clipboard aliased`() {
    configureRingOfThree("unnamed", "unnamedplus")

    typeText("p")
    typeText("<C-P>")

    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     three
      |2     two
      |3     one
      """.trimMargin(),
    )
  }
}
