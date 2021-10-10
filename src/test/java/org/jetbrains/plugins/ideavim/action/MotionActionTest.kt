/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author vlan
 */
class MotionActionTest : VimTestCase() {
  fun testDoubleToggleVisual() {
    val contents = "one tw${c}o\n"
    doTest("vv", contents, contents, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-198 |v_iw|
  fun testVisualMotionInnerWordNewLineAtEOF() {
    val fileContents = "one tw${c}o\n"
    doTest(
      "viw",
      fileContents,
      "one ${s}two${se}\n",
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER
    )
  }

  // |v_iW|
  fun testVisualMotionInnerBigWord() {
    val fileContents = "one tw${c}o.three four\n"
    val fileContentsAfter = "one ${s}two.thre${c}e$se four\n"
    doTest("viW", fileContents, fileContentsAfter, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
    assertSelection("two.three")
  }

  fun testEscapeInCommand() {
    val content = """
     on${c}e two
     three
     
    """.trimIndent()
    doTest(listOf("f", "<Esc>", "<Esc>"), content, content, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertPluginError(true)
    assertOffset(2)
  }

  // |h| |l|
  fun testLeftRightMove() {
    val before = "on${c}e two three four five six seven\n"
    val after = "one two three ${c}four five six seven\n"
    doTest(listOf("14l", "2h"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |j| |k|
  fun testUpDownMove() {
    val before = """
     one
     tw${c}o
     three
     four
     
    """.trimIndent()
    val after = """
     one
     two
     th${c}ree
     four
     
    """.trimIndent()
    doTest(listOf("2j", "k"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testDeleteDigitsInCount() {
    typeTextInFile(parseKeys("42<Delete>l"), "on${c}e two three four five six seven\n")
    assertOffset(6)
  }

  // |f|
  fun testForwardToTab() {
    val before = "on${c}e two\tthree\nfour\n"
    val after = "one two${c}\tthree\nfour\n"
    doTest(listOf("f<Tab>"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testIllegalCharArgument() {
    typeTextInFile(parseKeys("f<Insert>"), "on${c}e two three four five six seven\n")
    assertOffset(2)
    assertMode(CommandState.Mode.COMMAND)
  }

  // |F| |i_CTRL-K|
  fun testBackToDigraph() {
    val before = "Hallo, Öster${c}reich!\n"
    val after = "Hallo, ${c}Österreich!\n"
    val keys = listOf("F<C-K>O:")
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-771 |t| |;|
  fun testTillCharRight() {
    val keys = listOf("t:;")
    val before = "$c 1:a 2:b 3:c \n"
    val after = " 1:a ${c}2:b 3:c \n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-771 |t| |;|
  fun testTillCharRightRepeated() {
    val keys = listOf("t:;")
    val before = "$c 1:a 2:b 3:c \n"
    val after = " 1:a ${c}2:b 3:c \n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-771 |t| |;|
  fun testTillCharRightRepeatedWithCount2() {
    val keys = listOf("t:2;")
    val before = "$c 1:a 2:b 3:c \n"
    val after = " 1:a ${c}2:b 3:c \n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-771 |t| |;|
  fun testTillCharRightRepeatedWithCountHigherThan2() {
    val keys = listOf("t:3;")
    val before = "$c 1:a 2:b 3:c \n"
    val after = " 1:a 2:b ${c}3:c \n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-771 |t| |,|
  fun testTillCharRightReverseRepeated() {
    val keys = listOf("t:,,")
    val before = " 1:a 2:b$c 3:c \n"
    val after = " 1:${c}a 2:b 3:c \n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-771 |t| |,|
  fun testTillCharRightReverseRepeatedWithCount2() {
    val keys = listOf("t:,2,")
    val before = " 1:a 2:b$c 3:c \n"
    val after = " 1:${c}a 2:b 3:c \n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-771 |t| |,|
  fun testTillCharRightReverseRepeatedWithCountHigherThan3() {
    val keys = listOf("t:,3,")
    val before = " 0:_ 1:a 2:b$c 3:c \n"
    val after = " 0:${c}_ 1:a 2:b 3:c \n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-314 |d| |v_iB|
  fun testDeleteInnerCurlyBraceBlock() {
    val keys = listOf("di{")
    val before = "{foo, b${c}ar, baz}\n"
    val after = "{}\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-314 |d| |v_iB|
  fun testDeleteInnerCurlyBraceBlockCaretBeforeString() {
    val keys = listOf("di{")
    val before = "{foo, ${c}\"bar\", baz}\n"
    val after = "{}\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_aB|
  fun testDeleteOuterCurlyBraceBlock() {
    val keys = listOf("da{")
    val before = "x = {foo, b${c}ar, baz};\n"
    val after = "x = ;\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-261 |c| |v_iB|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testChangeInnerCurlyBraceBlockMultiLine() {
    typeTextInFile(
      parseKeys("ci{"),
      """foo {
    ${c}bar
}
"""
    )
    assertState(
      """
    foo {
    
    }
    
      """.trimIndent()
    )
    assertOffset(6)
  }

  // |d| |v_aw|
  fun testDeleteOuterWord() {
    val keys = listOf("daw")
    val before = "one t${c}wo three\n"
    val after = "one three\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_aW|
  fun testDeleteOuterBigWord() {
    val keys = listOf("daW")
    val before = "one \"t${c}wo\" three\n"
    val after = "one three\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_is|
  fun testDeleteInnerSentence() {
    val keys = listOf("dis")
    val before = "Hello World! How a${c}re you? Bye.\n"
    val after = "Hello World!  Bye.\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_as|
  fun testDeleteOuterSentence() {
    val keys = listOf("das")
    val before = "Hello World! How a${c}re you? Bye.\n"
    val after = "Hello World! Bye.\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |v_as|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  fun testSentenceMotionPastStartOfFile() {
    val keys = listOf("8(")

    val before = """
     
     P$c.
     
    """.trimIndent()
    val after = """
     
     P$c.
     
    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_ip|
  fun testDeleteInnerParagraph() {
    val keys = listOf("dip")
    val before = """
     Hello World!
     
     How a${c}re you?
     Bye.
     
     Bye.
     
    """.trimIndent()
    val after = """
    Hello World!
    
    
    Bye.
    
    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_ap|
  fun testDeleteOuterParagraph() {
    val keys = listOf("dap")
    val before = """
     Hello World!
     
     How a${c}re you?
     Bye.
     
     Bye.
     
    """.trimIndent()
    val after = """
    Hello World!
    
    Bye.
    
    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_a]|
  fun testDeleteOuterBracketBlock() {
    val keys = listOf("da]")
    val before = """foo = [
    one,
    t${c}wo,
    three
];
"""
    val after = "foo = ;\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_i]|
  fun testDeleteInnerBracketBlock() {
    val keys = listOf("di]")
    val before = "foo = [one, t${c}wo];\n"
    val after = "foo = [];\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1287 |d| |v_i(|
  fun testSelectInsideForStringLiteral() {
    val keys = listOf("di(")
    val before = "(text \"with quotes(and ${c}braces)\")"
    val after = "(text \"with quotes()\")"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1287 |d| |v_i{|
  fun testBadlyNestedBlockInsideString() {
    val before = "{\"{foo, ${c}bar\", baz}}"
    val keys = listOf("di{")
    val after = "{\"{foo, ${c}bar\", baz}}"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1287 |d| |v_i{|
  fun testDeleteInsideBadlyNestedBlock() {
    val before = "a{\"{foo}, ${c}bar\", baz}b}"
    val keys = listOf("di{")
    val after = "a{$c}b}"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1008 |c| |v_i{|
  fun testDeleteInsideDoubleQuotesSurroundedBlockWithSingleQuote() {
    val before = "\"{do${c}esn't work}\""
    val keys = listOf("ci{")
    val after = "\"{$c}\""
    doTest(keys, before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-1008 |c| |v_i{|
  fun testDeleteInsideSingleQuotesSurroundedBlock() {
    val keys = listOf("ci{")
    val before = "'{does n${c}ot work}'"
    val after = "'{$c}'"
    doTest(keys, before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-1008 |c| |v_i{|
  fun testDeleteInsideDoublySurroundedBlock() {
    val before = "<p class=\"{{ \$ctrl.so${c}meClassName }}\"></p>"
    val keys = listOf("ci{")
    val after = "<p class=\"{{$c}}\"></p>"
    doTest(keys, before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // |d| |v_i>|
  fun testDeleteInnerAngleBracketBlock() {
    val keys = listOf("di>")
    val before = "Foo<Foo, B${c}ar> bar\n"
    val after = "Foo<> bar\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_a>|
  fun testDeleteOuterAngleBracketBlock() {
    val keys = listOf("da>")
    val before = "Foo<Foo, B${c}ar> bar\n"
    val after = "Foo bar\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteInnerDoubleQuoteString() {
    val keys = listOf("di\"")
    val before = "foo = \"bar b${c}az\";\n"
    val after = "foo = \"\";\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_a"|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testDeleteOuterDoubleQuoteString() {
    val keys = listOf("da\"")
    val before = "foo = \"bar b${c}az\";\n"
    val after = "foo = ;\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteDoubleQuotedStringStart() {
    val keys = listOf("di\"")
    val before = "foo = [\"one\", ${c}\"two\", \"three\"];\n"
    val after = "foo = [\"one\", \"\", \"three\"];\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteDoubleQuotedStringEnd() {
    val keys = listOf("di\"")
    val before = "foo = [\"one\", \"two${c}\", \"three\"];\n"
    val after = "foo = [\"one\", \"\", \"three\"];\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteDoubleQuotedStringWithEscapes() {
    val keys = listOf("di\"")
    val before = "foo = \"fo\\\"o b${c}ar\";\n"
    val after = "foo = \"\";\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteDoubleQuotedStringBefore() {
    val keys = listOf("di\"")
    val before = "f${c}oo = [\"one\", \"two\", \"three\"];\n"
    val after = "foo = [\"\", \"two\", \"three\"];\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteDoubleQuotedStringOddNumberOfQuotes() {
    val keys = listOf("di\"")
    val before = "abc\"def${c}\"gh\"i"
    val after = "abc\"\"gh\"i"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteDoubleQuotedStringBetweenEvenNumberOfQuotes() {
    val keys = listOf("di\"")
    val before = "abc\"def\"g${c}h\"ijk\"l"
    val after = "abc\"def\"\"ijk\"l"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteDoubleQuotedStringOddNumberOfQuotesOnLast() {
    val keys = listOf("di\"")
    val before = "abcdef\"gh\"ij${c}\"kl"
    val after = "abcdef\"gh\"ij\"kl"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteDoubleQuotedStringEvenNumberOfQuotesOnLast() {
    val keys = listOf("di\"")
    val before = "abc\"def\"gh\"ij${c}\"kl"
    val after = "abc\"def\"gh\"\"kl"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |v_i"|
  fun testInnerDoubleQuotedStringSelection() {
    val keys = listOf("vi\"")
    val before = "foo = [\"o${c}ne\", \"two\"];\n"
    val after = "foo = [\"${s}on${c}e${se}\", \"two\"];\n"
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
  }

  // |c| |v_i"|
  fun testChangeEmptyQuotedString() {
    val keys = listOf("ci\"")
    val before = "foo = \"${c}\";\n"
    val after = "foo = \"\";\n"
    doTest(keys, before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_i'|
  fun testDeleteInnerSingleQuoteString() {
    val keys = listOf("di'")
    val before = "foo = 'bar b${c}az';\n"
    val after = "foo = '';\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_i`|
  fun testDeleteInnerBackQuoteString() {
    val keys = listOf("di`")
    val before = "foo = `bar b${c}az`;\n"
    val after = "foo = ``;\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_a'|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testDeleteOuterSingleQuoteString() {
    val keys = listOf("da'")
    val before = "foo = 'bar b${c}az';\n"
    val after = "foo = ;\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-132 |d| |v_a`|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testDeleteOuterBackQuoteString() {
    val keys = listOf("da`")
    val before = "foo = `bar b${c}az`;\n"
    val after = "foo = ;\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1427
  fun testDeleteOuterTagWithCount() {
    val keys = listOf("d2at")
    val before = "<a><b><c>$c</c></b></a>"
    val after = "<a></a>"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-2113
  fun testReplaceEmptyTagContent() {
    val keys = listOf("cit")
    val before = "<a><c>$c</c></a>"
    val after = "<a><c></c></a>"
    doTest(keys, before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun testDeleteToDigraph() {
    val keys = listOf("d/<C-K>O:<CR>")
    val before = "ab${c}cdÖef"
    val after = "abÖef"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |[(|
  fun testUnmatchedOpenParenthesis() {
    typeTextInFile(
      parseKeys("[("),
      """
     foo(bar, foo(bar, ${c}baz
     bar(foo)
     
      """.trimIndent()
    )
    assertOffset(12)
  }

  // |[{|
  fun testUnmatchedOpenBracketMultiLine() {
    typeTextInFile(
      parseKeys("[{"),
      """foo {
    bar,
    b${c}az
"""
    )
    assertOffset(4)
  }

  // |])|
  fun testUnmatchedCloseParenthesisMultiLine() {
    typeTextInFile(
      parseKeys("])"),
      """foo(bar, ${c}baz,
   quux)
"""
    )
    assertOffset(21)
  }

  // |]}|
  fun testUnmatchedCloseBracket() {
    typeTextInFile(parseKeys("]}"), "{bar, ${c}baz}\n")
    assertOffset(9)
  }

  // VIM-965 |[m|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT, "File type specific")
  fun testMethodMovingInNonJavaFile() {
    configureByJsonText("{\"foo\": \"${c}bar\"}\n")
    typeText(parseKeys("[m"))
    assertState("{\"foo\": \"${c}bar\"}\n")
  }

  // VIM-331 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testNonAsciiLettersInWord() {
    typeTextInFile(parseKeys("w"), "Če${c}ská republika")
    assertOffset(6)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testHiraganaToPunctuation() {
    typeTextInFile(parseKeys("w"), "は${c}はは!!!")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testHiraganaToFullWidthPunctuation() {
    typeTextInFile(parseKeys("w"), "は${c}はは！！！")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToHiragana() {
    typeTextInFile(parseKeys("w"), "チ${c}チチははは")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToHalfWidthKana() {
    typeTextInFile(parseKeys("w"), "チ${c}チチｳｳｳ")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToDigits() {
    typeTextInFile(parseKeys("w"), "チ${c}チチ123")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToLetters() {
    typeTextInFile(parseKeys("w"), "チ${c}チチ123")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToFullWidthLatin() {
    typeTextInFile(parseKeys("w"), "チ${c}チチＡＡＡ")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToFullWidthDigits() {
    typeTextInFile(parseKeys("w"), "チ${c}チチ３３３")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testHiraganaToKatakana() {
    typeTextInFile(parseKeys("w"), "は${c}ははチチチ")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testHalftWidthKanaToLetters() {
    typeTextInFile(parseKeys("w"), "ｳｳｳAAA")
    assertOffset(3)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToPunctuation() {
    typeTextInFile(parseKeys("w"), "测试${c}测试!!!")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToFullWidthPunctuation() {
    typeTextInFile(parseKeys("w"), "测试${c}测试！！！")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToDigits() {
    typeTextInFile(parseKeys("w"), "测试${c}测试123")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToFullWidthLatin() {
    typeTextInFile(parseKeys("w"), "测试${c}测试ＡＡＡ")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToFullWidthDigits() {
    typeTextInFile(parseKeys("w"), "测试${c}测试３３３")
    assertOffset(4)
  }

  // |w|
  fun testEmptyLineIsWord() {
    typeTextInFile(
      parseKeys("w"),
      """
     ${c}one
     
     two
     
      """.trimIndent()
    )
    assertOffset(4)
  }

  // |w|
  fun testNotEmptyLineIsNotWord() {
    typeTextInFile(
      parseKeys("w"),
      """${c}one
 
two
"""
    )
    assertOffset(6)
  }

  // VIM-312 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testLastWord() {
    typeTextInFile(parseKeys("w"), "${c}one\n")
    assertOffset(2)
  }

  // |b|
  fun testWordBackwardsAtFirstLineWithWhitespaceInFront() {
    typeTextInFile(parseKeys("b"), "    ${c}x\n")
    assertOffset(0)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testRightToLastChar() {
    typeTextInFile(parseKeys("i<Right>"), "on${c}e\n")
    assertOffset(3)
  }

  fun testDownToLastEmptyLine() {
    typeTextInFile(
      parseKeys("j"),
      """
     ${c}one
     
     
      """.trimIndent()
    )
    assertOffset(4)
  }

  // VIM-262 |c_CTRL-R|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testSearchFromRegister() {
    VimPlugin.getRegister().setKeys('a', StringHelper.stringToKeys("two"))
    typeTextInFile(
      parseKeys("/", "<C-R>a", "<Enter>"),
      """
     ${c}one
     two
     three
     
      """.trimIndent()
    )
    assertOffset(4)
  }

  // |v_gv|
  fun testSwapVisualSelections() {
    val keys = listOf("viw", "<Esc>", "0", "viw", "gv", "d")
    val before = "foo ${c}bar\n"
    val after = "foo \n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |CTRL-V|
  fun testVisualBlockSelectionsDisplayedCorrectlyMovingRight() {
    val keys = listOf("<C-V>jl")
    val before = """
     ${c}foo
     bar
     
    """.trimIndent()
    val after = """
    ${s}fo${se}o
    ${s}ba${se}r
    
    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
  }

  // |CTRL-V|
  fun testVisualBlockSelectionsDisplayedCorrectlyMovingLeft() {
    val keys = listOf("<C-V>jh")
    val before = """
     fo${c}o
     bar
     
    """.trimIndent()
    val after = """
    f${s}oo$se
    b${s}ar$se
    
    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
  }

  // |CTRL-V|
  fun testVisualBlockSelectionsDisplayedCorrectlyInDollarMode() {
    val keys = listOf("<C-V>jj$")
    val before = """
     a${c}b
     abc
     ab
     
    """.trimIndent()
    val after = """
    a${s}b$se
    a${s}bc$se
    a${s}b$se
    
    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
  }

  // |v_o|
  fun testSwapVisualSelectionEnds() {
    val keys = listOf("v", "l", "o", "l", "d")
    val before = "${c}foo\n"
    val after = "fo\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-564 |g_|
  fun testToLastNonBlankCharacterInLine() {
    doTest(
      "g_",
      """
     one   
     two   
     th${c}ree  
     four  
     
      """.trimIndent(),
      """
                one   
                two   
                thre${c}e  
                four  
                
      """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // |3g_|
  fun testToLastNonBlankCharacterInLineWithCount3() {
    doTest(
      "3g_",
      """
     o${c}ne   
     two   
     three  
     four  
     
      """.trimIndent(),
      """
                one   
                two   
                thre${c}e  
                four  
                
      """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // VIM-646 |gv|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  fun testRestoreMultiLineSelectionAfterYank() {
    val keys = listOf("V", "j", "y", "G", "p", "gv", "d")
    val before = """
     ${c}foo
     bar
     baz
     
    """.trimIndent()
    val after = """
    baz
    foo
    bar
    
    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |v_>| |gv|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Complicated")
  fun testRestoreMultiLineSelectionAfterIndent() {
    typeTextInFile(
      parseKeys("V", "2j"),
      """
     ${c}foo
     bar
     baz
     
      """.trimIndent()
    )
    assertSelection(
      """
    foo
    bar
    baz
    
      """.trimIndent()
    )
    typeText(parseKeys(">"))
    assertMode(CommandState.Mode.COMMAND)
    assertState(
      """    foo
    bar
    baz
"""
    )
    typeText(parseKeys("gv"))
    assertSelection(
      """    foo
    bar
    baz
"""
    )
    typeText(parseKeys(">"))
    assertMode(CommandState.Mode.COMMAND)
    assertState(
      """        foo
        bar
        baz
"""
    )
    typeText(parseKeys("gv"))
    assertSelection(
      """        foo
        bar
        baz
"""
    )
  }

  // VIM-862 |gv|
  fun testRestoreSelectionRange() {
    val keys = listOf("vl", "<Esc>", "gv")
    val before = """
    ${c}foo
    bar
    
    """.trimIndent()
    val after = """
    ${s}f${c}o${se}o
    bar
    
    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
  }

  fun testVisualLineSelectDown() {
    typeTextInFile(
      parseKeys("Vj"),
      """
     foo
     ${c}bar
     baz
     quux
     
      """.trimIndent()
    )
    assertMode(CommandState.Mode.VISUAL)
    assertSelection(
      """
    bar
    baz
    
      """.trimIndent()
    )
    assertOffset(8)
  }

  // VIM-784
  fun testVisualLineSelectUp() {
    typeTextInFile(
      parseKeys("Vk"),
      """
     foo
     bar
     ${c}baz
     quux
     
      """.trimIndent()
    )
    assertMode(CommandState.Mode.VISUAL)
    assertSelection(
      """
    bar
    baz
    
      """.trimIndent()
    )
    assertOffset(4)
  }
}
