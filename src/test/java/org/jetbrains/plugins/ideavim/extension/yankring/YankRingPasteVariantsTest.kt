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
 * Cycling has to work after every way of pasting, not just `p` and `P` (VIM-301, group C).
 *
 * YankRing maps the paste keys because a paste is what makes `<C-P>` meaningful; anything it does
 * not map is invisible to the ring, and `<C-P>` after it reports that there is nothing to replace.
 */
class YankRingPasteVariantsTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("yankring")
  }

  /** Leaves the ring holding "three", "two", "one" - newest first - with the caret on `A` of `AB`. */
  private fun configureRingOfThreeBeforeAB() {
    configureByText("${c}one two three\nAB")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("wyiw")
    typeText("wyiw")
    typeText("j0")
  }

  // C2
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test gp is cyclable and keeps leaving the caret after the pasted text`() {
    configureRingOfThreeBeforeAB()

    typeText("gp")
    assertState("one two three\nAthree${c}B")

    typeText("<C-P>")
    assertState("one two three\nAtwo${c}B")
  }

  // C2
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test gP is cyclable and keeps leaving the caret after the pasted text`() {
    configureRingOfThreeBeforeAB()

    typeText("gP")
    assertState("one two three\nthree${c}AB")

    typeText("<C-P>")
    assertState("one two three\ntwo${c}AB")
  }

  /**
   * Note the pasted line keeps its own indent rather than taking the current line's. In Vim `]p`
   * adjusts the indent; IdeaVim models it as `indent = false`, meaning "paste raw". That is
   * IdeaVim's behaviour for plain `]p`, so the test takes it as given rather than asserting Vim's
   * semantics through the yank ring.
   */
  // C3
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test bracket p is cyclable`() {
    configureByText("${c}foo\nbaz\n    bar")
    enterCommand("YRClear")
    typeText("yy")
    typeText("jyy")
    typeText("j")

    typeText("]p")
    assertState("foo\nbaz\n    bar\n${c}baz\n")

    typeText("<C-P>")
    assertState("foo\nbaz\n    bar\n${c}foo\n")
  }

  // C4
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test a counted paste is replaced as a unit`() {
    configureByText("${c}one two three\n")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("wyiw")
    typeText("wyiw")
    typeText("j")

    typeText("3p")
    assertState("one two three\nthreethreethre${c}e")

    // The count belongs to the paste, so replacing has to repeat it rather than drop to one copy
    typeText("<C-P>")
    assertState("one two three\ntwotwotw${c}o")
  }

  // C5
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test a visual mode paste is cyclable`() {
    configureByText("${c}one two three\nXXX")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("wyiw")
    typeText("j")

    typeText("viwp")
    assertState("one two three\ntw${c}o")

    typeText("<C-P>")
    assertState("one two three\non${c}e")
  }

  // C5
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test the selection a visual paste replaced enters the ring`() {
    configureByText("${c}one two three\nXXX")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("j")

    typeText("viwp")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     XXX
      |2     one
      """.trimMargin(),
    )
  }
}
