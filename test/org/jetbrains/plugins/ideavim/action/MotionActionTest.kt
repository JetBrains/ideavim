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

import com.intellij.json.JsonFileType
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
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
    doTest("viw",
      fileContents,
      "one ${s}two${se}\n",
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  // |v_iW|
  fun testVisualMotionInnerBigWord() {
    val fileContents = "one tw${c}o.three four\n"
    val fileContentsAfter = "one ${s}two.thre${c}e${se} four\n"
    doTest("viW", fileContents, fileContentsAfter, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
    assertSelection("two.three")
  }

  fun testEscapeInCommand() {
    typeTextInFile(StringHelper.parseKeys("f", "<Esc>", "<Esc>"), """
     on${c}e two
     three
     
     """.trimIndent())
    assertPluginError(true)
    assertOffset(2)
    assertMode(CommandState.Mode.COMMAND)
  }

  // |h| |l|
  fun testLeftRightMove() {
    typeTextInFile(StringHelper.parseKeys("14l", "2h"), "on${c}e two three four five six seven\n")
    assertOffset(14)
  }

  // |j| |k|
  fun testUpDownMove() {
    val editor = typeTextInFile(StringHelper.parseKeys("2j", "k"), """
     one
     tw${c}o
     three
     four
     
     """.trimIndent())
    val position = editor.caretModel.visualPosition
    assertEquals(VisualPosition(2, 2), position)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testDeleteDigitsInCount() {
    typeTextInFile(StringHelper.parseKeys("42<Delete>l"), "on${c}e two three four five six seven\n")
    assertOffset(6)
  }

  // |f|
  fun testForwardToTab() {
    typeTextInFile(StringHelper.parseKeys("f<Tab>"), "on${c}e two\tthree\nfour\n")
    assertOffset(7)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testIllegalCharArgument() {
    typeTextInFile(StringHelper.parseKeys("f<Insert>"), "on${c}e two three four five six seven\n")
    assertOffset(2)
    assertMode(CommandState.Mode.COMMAND)
  }

  // |F| |i_CTRL-K|
  fun testBackToDigraph() {
    typeTextInFile(StringHelper.parseKeys("F<C-K>O:"), "Hallo, Öster${c}reich!\n")
    myFixture.checkResult("Hallo, ${c}Österreich!\n")
    assertMode(CommandState.Mode.COMMAND)
  }

  // VIM-771 |t| |;|
  fun testTillCharRight() {
    typeTextInFile(StringHelper.parseKeys("t:;"), "${c} 1:a 2:b 3:c \n")
    myFixture.checkResult(" 1:a ${c}2:b 3:c \n")
  }

  // VIM-771 |t| |;|
  fun testTillCharRightRepeated() {
    typeTextInFile(StringHelper.parseKeys("t:;"), "${c} 1:a 2:b 3:c \n")
    myFixture.checkResult(" 1:a ${c}2:b 3:c \n")
  }

  // VIM-771 |t| |;|
  fun testTillCharRightRepeatedWithCount2() {
    typeTextInFile(StringHelper.parseKeys("t:2;"), "${c} 1:a 2:b 3:c \n")
    myFixture.checkResult(" 1:a ${c}2:b 3:c \n")
  }

  // VIM-771 |t| |;|
  fun testTillCharRightRepeatedWithCountHigherThan2() {
    typeTextInFile(StringHelper.parseKeys("t:3;"), "${c} 1:a 2:b 3:c \n")
    myFixture.checkResult(" 1:a 2:b ${c}3:c \n")
  }

  // VIM-771 |t| |,|
  fun testTillCharRightReverseRepeated() {
    typeTextInFile(StringHelper.parseKeys("t:,,"), " 1:a 2:b${c} 3:c \n")
    myFixture.checkResult(" 1:${c}a 2:b 3:c \n")
  }

  // VIM-771 |t| |,|
  fun testTillCharRightReverseRepeatedWithCount2() {
    typeTextInFile(StringHelper.parseKeys("t:,2,"), " 1:a 2:b${c} 3:c \n")
    myFixture.checkResult(" 1:${c}a 2:b 3:c \n")
  }

  // VIM-771 |t| |,|
  fun testTillCharRightReverseRepeatedWithCountHigherThan3() {
    typeTextInFile(StringHelper.parseKeys("t:,3,"), " 0:_ 1:a 2:b${c} 3:c \n")
    myFixture.checkResult(" 0:${c}_ 1:a 2:b 3:c \n")
  }

  // VIM-314 |d| |v_iB|
  fun testDeleteInnerCurlyBraceBlock() {
    typeTextInFile(StringHelper.parseKeys("di{"), "{foo, b${c}ar, baz}\n")
    myFixture.checkResult("{}\n")
  }

  // VIM-314 |d| |v_iB|
  fun testDeleteInnerCurlyBraceBlockCaretBeforeString() {
    typeTextInFile(StringHelper.parseKeys("di{"), "{foo, ${c}\"bar\", baz}\n")
    myFixture.checkResult("{}\n")
  }

  // |d| |v_aB|
  fun testDeleteOuterCurlyBraceBlock() {
    typeTextInFile(StringHelper.parseKeys("da{"), "x = {foo, b${c}ar, baz};\n")
    myFixture.checkResult("x = ;\n")
  }

  // VIM-261 |c| |v_iB|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testChangeInnerCurlyBraceBlockMultiLine() {
    typeTextInFile(StringHelper.parseKeys("ci{"), """foo {
    ${c}bar
}
""")
    myFixture.checkResult("""
    foo {
    
    }
    
    """.trimIndent())
    assertOffset(6)
  }

  // |d| |v_aw|
  fun testDeleteOuterWord() {
    typeTextInFile(StringHelper.parseKeys("daw"), "one t${c}wo three\n")
    myFixture.checkResult("one three\n")
  }

  // |d| |v_aW|
  fun testDeleteOuterBigWord() {
    typeTextInFile(StringHelper.parseKeys("daW"), "one \"t${c}wo\" three\n")
    myFixture.checkResult("one three\n")
  }

  // |d| |v_is|
  fun testDeleteInnerSentence() {
    typeTextInFile(StringHelper.parseKeys("dis"), "Hello World! How a${c}re you? Bye.\n")
    myFixture.checkResult("Hello World!  Bye.\n")
  }

  // |d| |v_as|
  fun testDeleteOuterSentence() {
    typeTextInFile(StringHelper.parseKeys("das"), "Hello World! How a${c}re you? Bye.\n")
    myFixture.checkResult("Hello World! Bye.\n")
  }

  // |v_as|
  fun testSentenceMotionPastStartOfFile() {
    typeTextInFile(StringHelper.parseKeys("8("), """
     
     P${c}.
     
     """.trimIndent())
  }

  // |d| |v_ip|
  fun testDeleteInnerParagraph() {
    typeTextInFile(StringHelper.parseKeys("dip"), """
     Hello World!
     
     How a${c}re you?
     Bye.
     
     Bye.
     
     """.trimIndent())
    myFixture.checkResult("""
    Hello World!
    
    
    Bye.
    
    """.trimIndent())
  }

  // |d| |v_ap|
  fun testDeleteOuterParagraph() {
    typeTextInFile(StringHelper.parseKeys("dap"), """
     Hello World!
     
     How a${c}re you?
     Bye.
     
     Bye.
     
     """.trimIndent())
    myFixture.checkResult("""
    Hello World!
    
    Bye.
    
    """.trimIndent())
  }

  // |d| |v_a]|
  fun testDeleteOuterBracketBlock() {
    typeTextInFile(StringHelper.parseKeys("da]"), """foo = [
    one,
    t${c}wo,
    three
];
""")
    myFixture.checkResult("foo = ;\n")
  }

  // |d| |v_i]|
  fun testDeleteInnerBracketBlock() {
    typeTextInFile(StringHelper.parseKeys("di]"), "foo = [one, t${c}wo];\n")
    myFixture.checkResult("foo = [];\n")
  }

  // VIM-1287 |d| |v_i(|
  fun testSelectInsideForStringLiteral() {
    typeTextInFile(StringHelper.parseKeys("di("), "(text \"with quotes(and ${c}braces)\")")
    myFixture.checkResult("(text \"with quotes()\")")
  }

  // VIM-1287 |d| |v_i{|
  fun testBadlyNestedBlockInsideString() {
    configureByText("{\"{foo, ${c}bar\", baz}}")
    typeText(StringHelper.parseKeys("di{"))
    myFixture.checkResult("{\"{foo, ${c}bar\", baz}}")
  }

  // VIM-1287 |d| |v_i{|
  fun testDeleteInsideBadlyNestedBlock() {
    configureByText("a{\"{foo}, ${c}bar\", baz}b}")
    typeText(StringHelper.parseKeys("di{"))
    myFixture.checkResult("a{${c}}b}")
  }

  // VIM-1008 |c| |v_i{|
  fun testDeleteInsideDoubleQuotesSurroundedBlockWithSingleQuote() {
    configureByText("\"{do${c}esn't work}\"")
    typeText(StringHelper.parseKeys("ci{"))
    myFixture.checkResult("\"{${c}}\"")
  }

  // VIM-1008 |c| |v_i{|
  fun testDeleteInsideSingleQuotesSurroundedBlock() {
    configureByText("'{does n${c}ot work}'")
    typeText(StringHelper.parseKeys("ci{"))
    myFixture.checkResult("'{${c}}'")
  }

  // VIM-1008 |c| |v_i{|
  fun testDeleteInsideDoublySurroundedBlock() {
    configureByText("<p class=\"{{ \$ctrl.so${c}meClassName }}\"></p>")
    typeText(StringHelper.parseKeys("ci{"))
    myFixture.checkResult("<p class=\"{{${c}}}\"></p>")
  }

  // |d| |v_i>|
  fun testDeleteInnerAngleBracketBlock() {
    typeTextInFile(StringHelper.parseKeys("di>"), "Foo<Foo, B${c}ar> bar\n")
    myFixture.checkResult("Foo<> bar\n")
  }

  // |d| |v_a>|
  fun testDeleteOuterAngleBracketBlock() {
    typeTextInFile(StringHelper.parseKeys("da>"), "Foo<Foo, B${c}ar> bar\n")
    myFixture.checkResult("Foo bar\n")
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteInnerDoubleQuoteString() {
    typeTextInFile(StringHelper.parseKeys("di\""), "foo = \"bar b${c}az\";\n")
    myFixture.checkResult("foo = \"\";\n")
  }

  // VIM-132 |d| |v_a"|
  fun testDeleteOuterDoubleQuoteString() {
    typeTextInFile(StringHelper.parseKeys("da\""), "foo = \"bar b${c}az\";\n")
    myFixture.checkResult("foo = ;\n")
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteDoubleQuotedStringStart() {
    typeTextInFile(StringHelper.parseKeys("di\""), "foo = [\"one\", ${c}\"two\", \"three\"];\n")
    myFixture.checkResult("foo = [\"one\", \"\", \"three\"];\n")
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteDoubleQuotedStringEnd() {
    typeTextInFile(StringHelper.parseKeys("di\""), "foo = [\"one\", \"two${c}\", \"three\"];\n")
    myFixture.checkResult("foo = [\"one\", \"\", \"three\"];\n")
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteDoubleQuotedStringWithEscapes() {
    typeTextInFile(StringHelper.parseKeys("di\""), "foo = \"fo\\\"o b${c}ar\";\n")
    myFixture.checkResult("foo = \"\";\n")
  }

  // VIM-132 |d| |v_i"|
  fun testDeleteDoubleQuotedStringBefore() {
    typeTextInFile(StringHelper.parseKeys("di\""), "f${c}oo = [\"one\", \"two\", \"three\"];\n")
    myFixture.checkResult("foo = [\"\", \"two\", \"three\"];\n")
  }

  fun testDeleteDoubleQuotedStringOddNumberOfQuotes() {
    typeTextInFile(StringHelper.parseKeys("di\""), "abc\"def${c}\"gh\"i")
    myFixture.checkResult("abc\"\"gh\"i")
  }

  fun testDeleteDoubleQuotedStringBetweenEvenNumberOfQuotes() {
    typeTextInFile(StringHelper.parseKeys("di\""), "abc\"def\"g${c}h\"ijk\"l")
    myFixture.checkResult("abc\"def\"\"ijk\"l")
  }

  fun testDeleteDoubleQuotedStringOddNumberOfQuotesOnLast() {
    typeTextInFile(StringHelper.parseKeys("di\""), "abcdef\"gh\"ij${c}\"kl")
    myFixture.checkResult("abcdef\"gh\"ij\"kl")
  }

  fun testDeleteDoubleQuotedStringEvenNumberOfQuotesOnLast() {
    typeTextInFile(StringHelper.parseKeys("di\""), "abc\"def\"gh\"ij${c}\"kl")
    myFixture.checkResult("abc\"def\"gh\"\"kl")
  }

  // VIM-132 |v_i"|
  fun testInnerDoubleQuotedStringSelection() {
    typeTextInFile(StringHelper.parseKeys("vi\""), "foo = [\"o${c}ne\", \"two\"];\n")
    assertSelection("one")
  }

  // |c| |v_i"|
  fun testChangeEmptyQuotedString() {
    typeTextInFile(StringHelper.parseKeys("ci\""), "foo = \"${c}\";\n")
    myFixture.checkResult("foo = \"\";\n")
  }

  // VIM-132 |d| |v_i'|
  fun testDeleteInnerSingleQuoteString() {
    typeTextInFile(StringHelper.parseKeys("di'"), "foo = 'bar b${c}az';\n")
    myFixture.checkResult("foo = '';\n")
  }

  // VIM-132 |d| |v_i`|
  fun testDeleteInnerBackQuoteString() {
    typeTextInFile(StringHelper.parseKeys("di`"), "foo = `bar b${c}az`;\n")
    myFixture.checkResult("foo = ``;\n")
  }

  // VIM-132 |d| |v_a'|
  fun testDeleteOuterSingleQuoteString() {
    typeTextInFile(StringHelper.parseKeys("da'"), "foo = 'bar b${c}az';\n")
    myFixture.checkResult("foo = ;\n")
  }

  // VIM-132 |d| |v_a`|
  fun testDeleteOuterBackQuoteString() {
    typeTextInFile(StringHelper.parseKeys("da`"), "foo = `bar b${c}az`;\n")
    myFixture.checkResult("foo = ;\n")
  }

  // VIM-1427
  fun testDeleteOuterTagWithCount() {
    typeTextInFile(StringHelper.parseKeys("d2at"), "<a><b><c>${c}</c></b></a>")
    myFixture.checkResult("<a></a>")
  }

  // VIM-2113
  fun testReplaceEmptyTagContent() {
    typeTextInFile(StringHelper.parseKeys("cit"), "<a><c>${c}</c></a>")
    myFixture.checkResult("<a><c></c></a>")
  }

  fun testDeleteToDigraph() {
    typeTextInFile(StringHelper.parseKeys("d/<C-K>O:<CR>"), "ab${c}cdÖef")
    myFixture.checkResult("abÖef")
  }

  // |[(|
  fun testUnmatchedOpenParenthesis() {
    typeTextInFile(StringHelper.parseKeys("[("), """
     foo(bar, foo(bar, ${c}baz
     bar(foo)
     
     """.trimIndent())
    assertOffset(12)
  }

  // |[{|
  fun testUnmatchedOpenBracketMultiLine() {
    typeTextInFile(StringHelper.parseKeys("[{"), """foo {
    bar,
    b${c}az
""")
    assertOffset(4)
  }

  // |])|
  fun testUnmatchedCloseParenthesisMultiLine() {
    typeTextInFile(StringHelper.parseKeys("])"), """foo(bar, ${c}baz,
   quux)
""")
    assertOffset(21)
  }

  // |]}|
  fun testUnmatchedCloseBracket() {
    typeTextInFile(StringHelper.parseKeys("]}"), "{bar, ${c}baz}\n")
    assertOffset(9)
  }

  // VIM-965 |[m|
  fun testMethodMovingInNonJavaFile() {
    myFixture.configureByText(JsonFileType.INSTANCE, "{\"foo\": \"${c}bar\"}\n")
    typeText(StringHelper.parseKeys("[m"))
    myFixture.checkResult("{\"foo\": \"${c}bar\"}\n")
  }

  // VIM-331 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testNonAsciiLettersInWord() {
    typeTextInFile(StringHelper.parseKeys("w"), "Če${c}ská republika")
    assertOffset(6)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testHiraganaToPunctuation() {
    typeTextInFile(StringHelper.parseKeys("w"), "は${c}はは!!!")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testHiraganaToFullWidthPunctuation() {
    typeTextInFile(StringHelper.parseKeys("w"), "は${c}はは！！！")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToHiragana() {
    typeTextInFile(StringHelper.parseKeys("w"), "チ${c}チチははは")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToHalfWidthKana() {
    typeTextInFile(StringHelper.parseKeys("w"), "チ${c}チチｳｳｳ")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToDigits() {
    typeTextInFile(StringHelper.parseKeys("w"), "チ${c}チチ123")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToLetters() {
    typeTextInFile(StringHelper.parseKeys("w"), "チ${c}チチ123")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToFullWidthLatin() {
    typeTextInFile(StringHelper.parseKeys("w"), "チ${c}チチＡＡＡ")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testKatakanaToFullWidthDigits() {
    typeTextInFile(StringHelper.parseKeys("w"), "チ${c}チチ３３３")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testHiraganaToKatakana() {
    typeTextInFile(StringHelper.parseKeys("w"), "は${c}ははチチチ")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testHalftWidthKanaToLetters() {
    typeTextInFile(StringHelper.parseKeys("w"), "ｳｳｳAAA")
    assertOffset(3)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToPunctuation() {
    typeTextInFile(StringHelper.parseKeys("w"), "测试${c}测试!!!")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToFullWidthPunctuation() {
    typeTextInFile(StringHelper.parseKeys("w"), "测试${c}测试！！！")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToDigits() {
    typeTextInFile(StringHelper.parseKeys("w"), "测试${c}测试123")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToFullWidthLatin() {
    typeTextInFile(StringHelper.parseKeys("w"), "测试${c}测试ＡＡＡ")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testCjkToFullWidthDigits() {
    typeTextInFile(StringHelper.parseKeys("w"), "测试${c}测试３３３")
    assertOffset(4)
  }

  // |w|
  fun testEmptyLineIsWord() {
    typeTextInFile(StringHelper.parseKeys("w"), """
     ${c}one
     
     two
     
     """.trimIndent())
    assertOffset(4)
  }

  // |w|
  fun testNotEmptyLineIsNotWord() {
    typeTextInFile(StringHelper.parseKeys("w"), """${c}one
 
two
""")
    assertOffset(6)
  }

  // VIM-312 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testLastWord() {
    typeTextInFile(StringHelper.parseKeys("w"), "${c}one\n")
    assertOffset(2)
  }

  // |b|
  fun testWordBackwardsAtFirstLineWithWhitespaceInFront() {
    typeTextInFile(StringHelper.parseKeys("b"), "    ${c}x\n")
    assertOffset(0)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testRightToLastChar() {
    typeTextInFile(StringHelper.parseKeys("i<Right>"), "on${c}e\n")
    assertOffset(3)
  }

  fun testDownToLastEmptyLine() {
    typeTextInFile(StringHelper.parseKeys("j"), """
     ${c}one
     
     
     """.trimIndent())
    assertOffset(4)
  }

  // VIM-262 |c_CTRL-R|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testSearchFromRegister() {
    VimPlugin.getRegister().setKeys('a', StringHelper.stringToKeys("two"))
    typeTextInFile(StringHelper.parseKeys("/", "<C-R>a", "<Enter>"), """
     ${c}one
     two
     three
     
     """.trimIndent())
    assertOffset(4)
  }

  // |v_gv|
  fun testSwapVisualSelections() {
    typeTextInFile(StringHelper.parseKeys("viw", "<Esc>", "0", "viw", "gv", "d"), "foo ${c}bar\n")
    myFixture.checkResult("foo \n")
  }

  // |CTRL-V|
  fun testVisualBlockSelectionsDisplayedCorrectlyMovingRight() {
    typeTextInFile(StringHelper.parseKeys("<C-V>jl"), """
     ${c}foo
     bar
     
     """.trimIndent())
    myFixture.checkResult("""
    ${s}fo${se}o
    ${s}ba${se}r
    
    """.trimIndent())
  }

  // |CTRL-V|
  fun testVisualBlockSelectionsDisplayedCorrectlyMovingLeft() {
    typeTextInFile(StringHelper.parseKeys("<C-V>jh"), """
     fo${c}o
     bar
     
     """.trimIndent())
    myFixture.checkResult("""
    f${s}oo${se}
    b${s}ar${se}
    
    """.trimIndent())
  }

  // |CTRL-V|
  fun testVisualBlockSelectionsDisplayedCorrectlyInDollarMode() {
    typeTextInFile(StringHelper.parseKeys("<C-V>jj$"), """
     a${c}b
     abc
     ab
     
     """.trimIndent())
    myFixture
      .checkResult("""
    a${s}b${se}
    a${s}bc${se}
    a${s}b${se}
    
    """.trimIndent())
  }

  // |v_o|
  fun testSwapVisualSelectionEnds() {
    typeTextInFile(StringHelper.parseKeys("v", "l", "o", "l", "d"), "${c}foo\n")
    myFixture.checkResult("fo\n")
  }

  // VIM-564 |g_|
  fun testToLastNonBlankCharacterInLine() {
    doTest("g_", """
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
                
                """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // |3g_|
  fun testToLastNonBlankCharacterInLineWithCount3() {
    doTest("3g_", """
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
                
                """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-646 |gv|
  fun testRestoreMultiLineSelectionAfterYank() {
    typeTextInFile(StringHelper.parseKeys("V", "j", "y", "G", "p", "gv", "d"), """
     ${c}foo
     bar
     baz
     
     """.trimIndent())
    myFixture.checkResult("""
    baz
    foo
    bar
    
    """.trimIndent())
  }

  // |v_>| |gv|
  fun testRestoreMultiLineSelectionAfterIndent() {
    typeTextInFile(StringHelper.parseKeys("V", "2j"), """
     ${c}foo
     bar
     baz
     
     """.trimIndent())
    assertSelection("""
    foo
    bar
    baz
    
    """.trimIndent())
    typeText(StringHelper.parseKeys(">"))
    assertMode(CommandState.Mode.COMMAND)
    myFixture.checkResult("""    foo
    bar
    baz
""")
    typeText(StringHelper.parseKeys("gv"))
    assertSelection("""    foo
    bar
    baz
""")
    typeText(StringHelper.parseKeys(">"))
    assertMode(CommandState.Mode.COMMAND)
    myFixture.checkResult("""        foo
        bar
        baz
""")
    typeText(StringHelper.parseKeys("gv"))
    assertSelection("""        foo
        bar
        baz
""")
  }

  // VIM-862 |gv|
  fun testRestoreSelectionRange() {
    configureByText("""
    ${c}foo
    bar
    
    """.trimIndent())
    typeText(StringHelper.parseKeys("vl", "<Esc>", "gv"))
    assertMode(CommandState.Mode.VISUAL)
    assertSelection("fo")
  }

  fun testVisualLineSelectDown() {
    typeTextInFile(StringHelper.parseKeys("Vj"), """
     foo
     ${c}bar
     baz
     quux
     
     """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSelection("""
    bar
    baz
    
    """.trimIndent())
    assertOffset(8)
  }

  // VIM-784
  fun testVisualLineSelectUp() {
    typeTextInFile(StringHelper.parseKeys("Vk"), """
     foo
     bar
     ${c}baz
     quux
     
     """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSelection("""
    bar
    baz
    
    """.trimIndent())
    assertOffset(4)
  }
}