/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeMotionActionTest : VimTestCase() {
  // VIM-515 |c| |W|
  @Test
  fun `test change big word with punctuation and alpha`() {
    doTest("cW", "foo${c}(bar baz\n", "foo baz\n", Mode.INSERT)
  }

  // VIM-300 |c| |w|
  @Test
  fun testChangeWordTwoWordsWithoutWhitespace() {
    doTest("cw", "${c}\$value\n", "value\n", Mode.INSERT)
  }

  // VIM-296 |cc|
  @Test
  fun testChangeLineAtLastLine() {
    doTest(
      "cc",
      "foo\n" + "${c}bar\n",
      "foo\n${c}" + "\n",
      Mode.INSERT,
    )
  }

  // VIM-296 |cc|
  @Test
  fun testChangeLineWithIndent() {
    doTest(
      "cc",
      """
        fun main {
            ${c}bar
        }
      """.trimIndent(),
      """
        fun main {
        ....${c}
        }
      """.trimIndent().dotToSpace(),
      Mode.INSERT,
    )
  }

  // VIM-536 |cc|
  @Test
  fun testChangeLineAtSecondLastLine() {
    doTest(
      "ccbaz",
      "${c}foo\n" + "bar\n",
      "baz\n" + "bar\n",
      Mode.INSERT,
    )
  }

  @Test
  fun testChangeLineAtLastLineWithUnderscoreMotion() {
    doTest(
      "c_",
      """
        foo
        ${c}bar
        
      """.trimIndent(),
      """
        foo
        ${c}
        
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun testChangeLineAtSecondLastLineWithUnderscoreMotion() {
    doTest(
      "c_baz",
      "${c}foo\n" + "bar\n",
      "baz\n" + "bar\n",
      Mode.INSERT,
    )
  }

  // VIM-200 |c| |w|
  @Test
  fun testChangeWordAtLastChar() {
    doTest(
      "cw",
      "on${c}e two three\n",
      "on${c} two three\n",
      Mode.INSERT,
    )
  }

  // VIM-1380 |c| |w| |count|
  @Test
  fun testChangeTwoWordsAtLastChar() {
    doTest(
      "c2w",
      "on${c}e two three\n",
      "on${c} three\n",
      Mode.INSERT,
    )
  }

  // |c| |t|
  @Test
  fun testChangeLinesTillForwards() {
    doTest(
      listOf("ct(", "for "),
      """
   ${c}if (condition) {
   }
   
      """.trimIndent(),
      """
   for (condition) {
   }
   
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  // VIM-276 |c| |T|
  @Test
  fun testChangeLinesTillBackwards() {
    doTest(
      "cT(",
      "if (condition) ${c}{\n" + "}\n",
      "if ({\n" + "}\n",
      Mode.INSERT,
    )
  }

  // VIM-276 |c| |F|
  @Test
  fun testChangeLinesToBackwards() {
    doTest(
      "cFc",
      "if (condition) {${c}\n" + "}\n",
      "if (\n" + "}\n",
      Mode.INSERT,
    )
  }

  // VIM-421 |c| |w|
  @Test
  fun testChangeLastWordInLine() {
    doTest(
      "cw",
      "ab.${c}cd\n",
      "ab.${c}\n",
      Mode.INSERT,
    )
  }

  // VIM-421 |c| |iw|
  @Test
  fun testChangeLastInnerWordInLine() {
    doTest(
      listOf("c", "iw", "baz"),
      "foo bar bo${c}o\n",
      "foo bar baz\n",
      Mode.INSERT,
    )
  }

  // VIM-421 |c| |w|
  @Test
  fun testChangeLastCharInLine() {
    doTest("cw", "fo${c}o\n", "fo${c}\n", Mode.INSERT)
  }

  @Test
  fun testLastSymbolInWord() {
    doTest("cw", "fo${c}o", "fo${c}", Mode.INSERT)
  }

  // VIM-3729
  @Test
  fun `test change with count applies only to motion when repeated`() {
    doTest(
      listOf("2c3l", "foo<Esc>", "w", "."),
      """
        banana banana
      """.trimIndent(),
      """
        foo foo
      """.trimIndent()
    )
  }

  @Test
  fun `test undo after change word`() {
    configureByText("Hello ${c}world and more")
    typeText("cw")
    typeText("Vim")
    typeText("<Esc>")
    assertState("Hello Vi${c}m and more")
    typeText("u")
    assertState("Hello ${c}world and more")
  }

  @Test
  fun `test undo after change word with oldundo`() {
    configureByText("Hello ${c}world and more")
    try {
      enterCommand("set oldundo")
      typeText("cw")
      typeText("Vim")
      typeText("<Esc>")
      assertState("Hello Vi${c}m and more")
      typeText("u")
      assertState("Hello Vim${c} and more")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after change line`() {
    configureByText(
      """
      First line
      ${c}Second line with text
      Third line
    """.trimIndent()
    )
    typeText("cc")
    typeText("Changed line")
    typeText("<Esc>")
    assertState(
      """
      First line
      Changed lin${c}e
      Third line
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      First line
      ${c}Second line with text
      Third line
    """.trimIndent()
    )
  }

  @Test
  fun `test undo after change line with oldundo`() {
    configureByText(
      """
      First line
      ${c}Second line with text
      Third line
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("cc")
      typeText("Changed line")
      typeText("<Esc>")
      assertState(
        """
      First line
      Changed lin${c}e
      Third line
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      First line
      Changed line${c}
      Third line
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after change to end of line`() {
    configureByText("Start ${c}middle end")
    typeText("C")
    typeText("new ending")
    typeText("<Esc>")
    assertState("Start new endin${c}g")
    typeText("u")
    assertState("Start ${c}middle end")
  }

  @Test
  fun `test undo after change to end of line with oldundo`() {
    configureByText("Start ${c}middle end")
    try {
      enterCommand("set oldundo")
      typeText("C")
      typeText("new ending")
      typeText("<Esc>")
      assertState("Start new endin${c}g")
      typeText("u")
      assertState("Start new ending${c}")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after change with motion`() {
    configureByText("The ${c}quick brown fox")
    typeText("c3w")
    typeText("slow")
    typeText("<Esc>")
    assertState("The slo${c}w")
    typeText("u")
    assertState("The ${c}quick brown fox")
  }

  @Test
  fun `test undo after change with motion with oldundo`() {
    configureByText("The ${c}quick brown fox")
    try {
      enterCommand("set oldundo")
      typeText("c3w")
      typeText("slow")
      typeText("<Esc>")
      assertState("The slo${c}w")
      typeText("u")
      assertState("The slow${c}")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo change with motion and caret movement`() {
    configureByText("a${c}bc(xxx)def")
    typeText("ci(")
    typeText("yyy")
    typeText("<Esc>")
    assertState("abc(yy${c}y)def")
    typeText("u")
    assertState("a${c}bc(xxx)def")
  }

  @Test
  fun `test undo change with motion and caret movement with oldundo`() {
    configureByText("a${c}bc(xxx)def")
    try {
      enterCommand("set oldundo")
      typeText("ci(")
      typeText("yyy")
      typeText("<Esc>")
      assertState("abc(yy${c}y)def")
      typeText("u")
      assertState("abc(yyy${c})def")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test multiple undo after sequential changes`() {
    configureByText("${c}one two three")
    typeText("cw")
    typeText("ONE")
    typeText("<Esc>")
    assertState("ON${c}E two three")

    typeText("w")
    typeText("cw")
    typeText("TWO")
    typeText("<Esc>")
    assertState("ONE TW${c}O three")

    // Undo second change
    typeText("u")
    assertState("ONE ${c}two three")

    // Undo first change
    typeText("u")
    assertState("${c}one two three")
  }

  @Test
  fun `test multiple undo after sequential changes with oldundo`() {
    configureByText("${c}one two three")
    try {
      enterCommand("set oldundo")
      typeText("cw")
      typeText("ONE")
      typeText("<Esc>")
      assertState("ON${c}E two three")

      typeText("w")
      typeText("cw")
      typeText("TWO")
      typeText("<Esc>")
      assertState("ONE TW${c}O three")

      // Undo second change
      typeText("u")
      assertState("ONE TWO${c} three")

      // Undo first change
      typeText("u")
      assertState("ONE ${c} three")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo change character`() {
    configureByText("a${c}bcdef")
    typeText("s")
    typeText("X")
    typeText("<Esc>")
    assertState("a${c}Xcdef")
    typeText("u")
    assertState("a${c}bcdef")
  }

  @Test
  fun `test undo change character with oldundo`() {
    configureByText("a${c}bcdef")
    try {
      enterCommand("set oldundo")
      typeText("s")
      typeText("X")
      typeText("<Esc>")
      assertState("a${c}Xcdef")
      typeText("u")
      assertState("aX${c}cdef")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo change multiple characters`() {
    configureByText("abc${c}defghijk")
    typeText("3s")
    typeText("XXX")
    typeText("<Esc>")
    assertState("abcXX${c}Xghijk")
    typeText("u")
    assertState("abc${c}defghijk")
  }

  @Test
  fun `test undo change multiple characters with oldundo`() {
    configureByText("abc${c}defghijk")
    try {
      enterCommand("set oldundo")
      typeText("3s")
      typeText("XXX")
      typeText("<Esc>")
      assertState("abcXX${c}Xghijk")
      typeText("u")
      assertState("abcXXX${c}ghijk")
    } finally {
      enterCommand("set nooldundo")
    }
  }
}
