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
import org.junit.jupiter.api.Disabled
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
  @Suppress("unused")
  @Test
  @Disabled
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
}
