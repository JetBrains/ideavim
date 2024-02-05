/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.sneak

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class IdeaVimSneakTest : VimTestCase() {
  @Throws(Exception::class)
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("sneak")
  }

  @Test
  fun testSneakForward() {
    val before = "som${c}e text"
    val after = "some te${c}xt"

    doTest("sxt", before, after, Mode.NORMAL())
  }

  @Test
  fun testSneakForwardVertical() {
    val before = """som${c}e text
        another line
        third line"""
    val after = """some text
        another line
        thi${c}rd line"""

    doTest("srd", before, after, Mode.NORMAL())
  }

  @Test
  fun testSneakForwardDoNotIgnoreCase() {
    val before = "som${c}e teXt"
    val after = "som${c}e teXt"

    doTest("sxt", before, after, Mode.NORMAL())
  }

  @Test
  fun testSneakForwardIgnoreCase() {
    val before = "som${c}e teXt"
    val after = "some te${c}Xt"

    enableExtensions("ignorecase")

    doTest("sxt", before, after, Mode.NORMAL())
    doTest("sXt", before, after, Mode.NORMAL())
    doTest("sXT", before, after, Mode.NORMAL())
    doTest("sxT", before, after, Mode.NORMAL())
  }

  @Test
  fun testSneakForwardSmartIgnoreCase() {
    val before = "som${c}e teXt"
    val after = "some te${c}Xt"

    enableExtensions("ignorecase", "smartcase")

    doTest("sxt", before, after, Mode.NORMAL())
    doTest("sXt", before, after, Mode.NORMAL())
    doTest("sXT", before, before, Mode.NORMAL())
    doTest("sxT", before, before, Mode.NORMAL())
  }

  @Test
  fun testSneakForwardAndFindAgain() {
    val before = "som${c}e text text"
    val after = "some text te${c}xt"

    doTest("sxt;", before, after, Mode.NORMAL())
  }

  @Test
  fun testSneakForwardAndFindReverseAgain() {
    val before = "some tex${c}t text"
    val after = "some ${c}text text"

    doTest("ste,", before, after, Mode.NORMAL())
  }

  @Test
  fun testSneakBackward() {
    val before = "some tex${c}t"
    val after = "so${c}me text"

    doTest("Sme", before, after, Mode.NORMAL())
  }

  @Test
  fun testSneakBackwardVertical() {
    val before = """some text
        another line
        thi${c}rd line"""
    val after = """so${c}me text
        another line
        third line"""

    doTest("Sme", before, after, Mode.NORMAL())
  }

  @Test
  fun testSneakBackwardAndFindAgain() {
    // caret has to be before another character (space here)
    val before = "some text text${c} "
    val after = "some ${c}text text "

    doTest("Ste;", before, after, Mode.NORMAL())
  }

  @Test
  fun testSneakBackwardAndFindReverseAgain() {
    val before = "some tex${c}t text"
    val after = "some text ${c}text"

    doTest("Ste,", before, after, Mode.NORMAL())
  }

  @Test
  fun testEndOfFile() {
    val before = """first line
        some te${c}xt
        another line
        last line."""
    val after = """first line
        some text
        another line
        last lin${c}e."""

    doTest("se.", before, after, Mode.NORMAL())
  }

  @Test
  fun testStartOfFile() {
    val before = """first line
        some text
        another${c} line
        last line."""
    val after = """${c}first line
        some text
        another line
        last line."""

    doTest("Sfi", before, after, Mode.NORMAL())
  }

  @Test
  fun testEscapeFirstChar() {
    val before = "so${c}me dwarf"
    val after = "some ${c}dwarf"

    doTest("sa<ESC>sdw", before, after, Mode.NORMAL())
  }
}
