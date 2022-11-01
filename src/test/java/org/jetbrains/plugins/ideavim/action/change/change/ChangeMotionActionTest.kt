/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class ChangeMotionActionTest : VimTestCase() {
  // VIM-515 |c| |W|
  fun `test change big word with punctuation and alpha`() {
    doTest("cW", "foo${c}(bar baz\n", "foo baz\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  // VIM-300 |c| |w|
  fun testChangeWordTwoWordsWithoutWhitespace() {
    doTest("cw", "${c}\$value\n", "value\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  // VIM-296 |cc|
  fun testChangeLineAtLastLine() {
    doTest(
      "cc",
      "foo\n" + "${c}bar\n",
      "foo\n${c}" + "\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-296 |cc|
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
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-536 |cc|
  fun testChangeLineAtSecondLastLine() {
    doTest(
      "ccbaz",
      "${c}foo\n" + "bar\n",
      "baz\n" + "bar\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

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
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun testChangeLineAtSecondLastLineWithUnderscoreMotion() {
    doTest(
      "c_baz",
      "${c}foo\n" + "bar\n",
      "baz\n" + "bar\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-200 |c| |w|
  fun testChangeWordAtLastChar() {
    doTest(
      "cw", "on${c}e two three\n", "on${c} two three\n", VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE
    )
  }

  // VIM-1380 |c| |w| |count|
  fun testChangeTwoWordsAtLastChar() {
    doTest(
      "c2w", "on${c}e two three\n", "on${c} three\n", VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE
    )
  }

  // |c| |t|
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
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-276 |c| |T|
  fun testChangeLinesTillBackwards() {
    doTest(
      "cT(", "if (condition) ${c}{\n" + "}\n", "if ({\n" + "}\n", VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE
    )
  }

  // VIM-276 |c| |F|
  @Suppress("unused")
  fun ignoreTestChangeLinesToBackwards() {
    doTest(
      "cFc",
      "if (condition) {${c}\n" + "}\n",
      "if (\n" + "}\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-421 |c| |w|
  fun testChangeLastWordInLine() {
    doTest(
      "cw",
      "ab.${c}cd\n", "ab.${c}\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-421 |c| |iw|
  fun testChangeLastInnerWordInLine() {
    doTest(
      listOf("c", "iw", "baz"),
      "foo bar bo${c}o\n", "foo bar baz\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-421 |c| |w|
  fun testChangeLastCharInLine() {
    doTest("cw", "fo${c}o\n", "fo${c}\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun testLastSymbolInWord() {
    doTest("cw", "fo${c}o", "fo${c}", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }
}
