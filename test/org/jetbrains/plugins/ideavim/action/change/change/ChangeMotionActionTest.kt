@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class ChangeMotionActionTest : VimTestCase() {
  // VIM-515 |c| |W|
  fun `test change big word with punctuation and alpha`() {
    doTest(parseKeys("cW"), "foo${c}(bar baz\n", "foo baz\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-300 |c| |w|
  fun testChangeWordTwoWordsWithoutWhitespace() {
    doTest(parseKeys("cw"), "${c}\$value\n", "value\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-296 |cc|
  fun testChangeLineAtLastLine() {
    doTest(parseKeys("cc"),
      "foo\n" + "${c}bar\n",
      "foo\n${c}" + "\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-536 |cc|
  fun testChangeLineAtSecondLastLine() {
    doTest(parseKeys("ccbaz"),
      "${c}foo\n" + "bar\n",
      "baz\n" + "bar\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun testChangeLineAtLastLineWithUnderscoreMotion() {
    doTest(parseKeys("c_"),
      """
        foo
        ${c}bar
        
        """.trimIndent(),
      """
        foo
        ${c}
        
        """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun testChangeLineAtSecondLastLineWithUnderscoreMotion() {
    doTest(parseKeys("c_baz"),
      "${c}foo\n" + "bar\n",
      "baz\n" + "bar\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-200 |c| |w|
  fun testChangeWordAtLastChar() {
    doTest(parseKeys("cw"), "on${c}e two three\n", "on${c} two three\n", CommandState.Mode.INSERT,
      CommandState.SubMode.NONE)
  }

  // VIM-1380 |c| |w| |count|
  fun testChangeTwoWordsAtLastChar() {
    doTest(parseKeys("c2w"), "on${c}e two three\n", "on${c} three\n", CommandState.Mode.INSERT,
      CommandState.SubMode.NONE)
  }

  // |c| |t|
  fun testChangeLinesTillForwards() {
    doTest(parseKeys("ct(", "for "), """
   ${c}if (condition) {
   }
   
   """.trimIndent(), """
   for (condition) {
   }
   
   """.trimIndent(),
      CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-276 |c| |T|
  fun testChangeLinesTillBackwards() {
    doTest(parseKeys("cT("), "if (condition) {${c}\n" + "}\n", "if (\n" + "}\n", CommandState.Mode.INSERT,
      CommandState.SubMode.NONE)
  }

  // VIM-276 |c| |F|
  fun ignoreTestChangeLinesToBackwards() {
    doTest(parseKeys("cFc"),
      "if (condition) {${c}\n" + "}\n",
      "if (\n" + "}\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }


  // VIM-421 |c| |w|
  fun testChangeLastWordInLine() {
    doTest(parseKeys("cw"),
      "ab.${c}cd\n", "ab.${c}\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-421 |c| |iw|
  fun testChangeLastInnerWordInLine() {
    doTest(parseKeys("c", "iw", "baz"),
      "foo bar bo${c}o\n", "foo bar baz\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-421 |c| |w|
  fun testChangeLastCharInLine() {
    doTest(parseKeys("cw"), "fo${c}o\n", "fo${c}\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }
}
