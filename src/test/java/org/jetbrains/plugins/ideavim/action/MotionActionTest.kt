/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * @author vlan
 */
class MotionActionTest : VimTestCase() {
  @Test
  fun testDoubleToggleVisual() {
    val contents = "one tw${c}o\n"
    doTest("vv", contents, contents, Mode.NORMAL())
  }

  // VIM-198 |v_iw|
  @Test
  fun testVisualMotionInnerWordNewLineAtEOF() {
    val fileContents = "one tw${c}o\n"
    doTest(
      "viw",
      fileContents,
      "one ${s}two${se}\n",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // |v_iW|
  @Test
  fun testVisualMotionInnerBigWord() {
    val fileContents = "one tw${c}o.three four\n"
    val fileContentsAfter = "one ${s}two.thre${c}e$se four\n"
    doTest("viW", fileContents, fileContentsAfter, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertSelection("two.three")
  }

  @Test
  @Disabled("VIM-3376")
  fun testEscapeInCommand() {
    val content = """
     on${c}e two
     three
     
    """.trimIndent()
    doTest(listOf("f", "<Esc>", "<Esc>"), content, content, Mode.NORMAL())
    assertPluginError(true)
    assertOffset(2)
  }

  @Test
  fun testEscapeInCommandAndNumber() {
    val content = """
     on${c}e two
     three
     
    """.trimIndent()
    doTest(listOf("12", "<Esc>"), content, content, Mode.NORMAL())
    assertPluginError(false)
    assertEmpty(KeyHandler.getInstance().keyHandlerState.commandBuilder.keys.toList())
  }

  // |h| |l|
  @Test
  fun testLeftRightMove() {
    val before = "on${c}e two three four five six seven\n"
    val after = "one two three ${c}four five six seven\n"
    doTest(listOf("14l", "2h"), before, after, Mode.NORMAL())
  }

  // |j| |k|
  @Test
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
    doTest(listOf("2j", "k"), before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testDeleteDigitsInCount() {
    typeTextInFile(injector.parser.parseKeys("42<Delete>l"), "on${c}e two three four five six seven\n")
    assertOffset(6)
  }

  // |f|
  @Test
  fun testForwardToTab() {
    val before = "on${c}e two\tthree\nfour\n"
    val after = "one two${c}\tthree\nfour\n"
    doTest(listOf("f<Tab>"), before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testIllegalCharArgument() {
    typeTextInFile(injector.parser.parseKeys("f<Insert>"), "on${c}e two three four five six seven\n")
    assertOffset(2)
    assertMode(Mode.NORMAL())
  }

  // |F| |i_CTRL-K|
  @Test
  fun testBackToDigraph() {
    val before = "Hallo, Öster${c}reich!\n"
    val after = "Hallo, ${c}Österreich!\n"
    val keys = listOf("F<C-K>O:")
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-771 |t| |;|
  @Test
  fun testTillCharRight() {
    val keys = listOf("t:;")
    val before = "$c 1:a 2:b 3:c \n"
    val after = " 1:a ${c}2:b 3:c \n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-771 |t| |;|
  @Test
  fun testTillCharRightRepeated() {
    val keys = listOf("t:;")
    val before = "$c 1:a 2:b 3:c \n"
    val after = " 1:a ${c}2:b 3:c \n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-771 |t| |;|
  @Test
  fun testTillCharRightRepeatedWithCount2() {
    val keys = listOf("t:2;")
    val before = "$c 1:a 2:b 3:c \n"
    val after = " 1:a ${c}2:b 3:c \n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-771 |t| |;|
  @Test
  fun testTillCharRightRepeatedWithCountHigherThan2() {
    val keys = listOf("t:3;")
    val before = "$c 1:a 2:b 3:c \n"
    val after = " 1:a 2:b ${c}3:c \n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-771 |t| |,|
  @Test
  fun testTillCharRightReverseRepeated() {
    val keys = listOf("t:,,")
    val before = " 1:a 2:b$c 3:c \n"
    val after = " 1:${c}a 2:b 3:c \n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-771 |t| |,|
  @Test
  fun testTillCharRightReverseRepeatedWithCount2() {
    val keys = listOf("t:,2,")
    val before = " 1:a 2:b$c 3:c \n"
    val after = " 1:${c}a 2:b 3:c \n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-771 |t| |,|
  @Test
  fun testTillCharRightReverseRepeatedWithCountHigherThan3() {
    val keys = listOf("t:,3,")
    val before = " 0:_ 1:a 2:b$c 3:c \n"
    val after = " 0:${c}_ 1:a 2:b 3:c \n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-314 |d| |v_iB|
  @Test
  fun testDeleteInnerCurlyBraceBlock() {
    val keys = listOf("di{")
    val before = "{foo, b${c}ar, baz}\n"
    val after = "{}\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-314 |d| |v_iB|
  @Test
  fun testDeleteInnerCurlyBraceBlockCaretBeforeString() {
    val keys = listOf("di{")
    val before = "{foo, ${c}\"bar\", baz}\n"
    val after = "{}\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_aB|
  @Test
  fun testDeleteOuterCurlyBraceBlock() {
    val keys = listOf("da{")
    val before = "x = {foo, b${c}ar, baz};\n"
    val after = "x = ;\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-261 |c| |v_iB|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testChangeInnerCurlyBraceBlockMultiLine() {
    typeTextInFile(
      injector.parser.parseKeys("ci{"),
      """foo {
    ${c}bar
}
""",
    )
    assertState(
      """
    foo {
    
    }
    
      """.trimIndent(),
    )
    assertOffset(6)
  }

  // |d| |v_aw|
  @Test
  fun testDeleteOuterWord() {
    val keys = listOf("daw")
    val before = "one t${c}wo three\n"
    val after = "one three\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_aW|
  @Test
  fun testDeleteOuterBigWord() {
    val keys = listOf("daW")
    val before = "one \"t${c}wo\" three\n"
    val after = "one three\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_is|
  @Test
  fun testDeleteInnerSentence() {
    val keys = listOf("dis")
    val before = "Hello World! How a${c}re you? Bye.\n"
    val after = "Hello World!  Bye.\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_as|
  @Test
  fun testDeleteOuterSentence() {
    val keys = listOf("das")
    val before = "Hello World! How a${c}re you? Bye.\n"
    val after = "Hello World! Bye.\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |v_as|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  @Test
  fun testSentenceMotionPastStartOfFile() {
    val keys = listOf("8(")

    val before = """
     
     P$c.
     
    """.trimIndent()
    val after = """
     
     P$c.
     
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_ip|
  @Test
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
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_ap|
  @Test
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
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_a]|
  @Test
  fun testDeleteOuterBracketBlock() {
    val keys = listOf("da]")
    val before = """foo = [
    one,
    t${c}wo,
    three
];
"""
    val after = "foo = ;\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_i]|
  @Test
  fun testDeleteInnerBracketBlock() {
    val keys = listOf("di]")
    val before = "foo = [one, t${c}wo];\n"
    val after = "foo = [];\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-1287 |d| |v_i(|
  @Test
  fun testSelectInsideForStringLiteral() {
    val keys = listOf("di(")
    val before = "(text \"with quotes(and ${c}braces)\")"
    val after = "(text \"with quotes()\")"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-1287 |d| |v_i{|
  @Test
  @VimBehaviorDiffers(
    originalVimAfter = "{\"{foo, ${c}bar\", baz}}",
    description = "We have PSI and can resolve this case correctly. I'm not sure if it should be fixed"
  )
  fun testBadlyNestedBlockInsideString() {
    val before = "{\"{foo, ${c}bar\", baz}}"
    val keys = listOf("di{")
    val after = "{}}"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-1287 |d| |v_i{|
  @Test
  fun testDeleteInsideBadlyNestedBlock() {
    val before = "a{\"{foo}, ${c}bar\", baz}b}"
    val keys = listOf("di{")
    val after = "a{$c}b}"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-1008 |c| |v_i{|
  @Test
  fun testDeleteInsideDoubleQuotesSurroundedBlockWithSingleQuote() {
    val before = "\"{do${c}esn't work}\""
    val keys = listOf("ci{")
    val after = "\"{$c}\""
    doTest(keys, before, after, Mode.INSERT)
  }

  @Test
  fun testDeletingInnerBlockWhenItIsPresentInString() {
    val before = "let variable = ('abc' .. \"br${c}aces ( with content )\")"
    val keys = listOf("di(")
    val after = "let variable = ()"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-1008 |c| |v_i{|
  @Test
  fun testDeleteInsideSingleQuotesSurroundedBlock() {
    val keys = listOf("ci{")
    val before = "'{does n${c}ot work}'"
    val after = "'{$c}'"
    doTest(keys, before, after, Mode.INSERT)
  }

  // VIM-1008 |c| |v_i{|
  @Test
  fun testDeleteInsideDoublySurroundedBlock() {
    val before = "<p class=\"{{ \$ctrl.so${c}meClassName }}\"></p>"
    val keys = listOf("ci{")
    val after = "<p class=\"{{$c}}\"></p>"
    doTest(keys, before, after, Mode.INSERT)
  }

  // |d| |v_i>|
  @Test
  fun testDeleteInnerAngleBracketBlock() {
    val keys = listOf("di>")
    val before = "Foo<Foo, B${c}ar> bar\n"
    val after = "Foo<> bar\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_a>|
  @Test
  fun testDeleteOuterAngleBracketBlock() {
    val keys = listOf("da>")
    val before = "Foo<Foo, B${c}ar> bar\n"
    val after = "Foo bar\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |d| |v_i"|
  @Test
  fun testDeleteInnerDoubleQuoteString() {
    val keys = listOf("di\"")
    val before = "foo = \"bar b${c}az\";\n"
    val after = "foo = \"\";\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |d| |v_a"|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testDeleteOuterDoubleQuoteString() {
    val keys = listOf("da\"")
    val before = "foo = \"bar b${c}az\";\n"
    val after = "foo = ;\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |d| |v_i"|
  @Test
  fun testDeleteDoubleQuotedStringStart() {
    val keys = listOf("di\"")
    val before = "foo = [\"one\", ${c}\"two\", \"three\"];\n"
    val after = "foo = [\"one\", \"\", \"three\"];\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |d| |v_i"|
  @Test
  fun testDeleteDoubleQuotedStringEnd() {
    val keys = listOf("di\"")
    val before = "foo = [\"one\", \"two${c}\", \"three\"];\n"
    val after = "foo = [\"one\", \"\", \"three\"];\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |d| |v_i"|
  @Test
  fun testDeleteDoubleQuotedStringWithEscapes() {
    val keys = listOf("di\"")
    val before = "foo = \"fo\\\"o b${c}ar\";\n"
    val after = "foo = \"\";\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |d| |v_i"|
  @Test
  fun testDeleteDoubleQuotedStringBefore() {
    val keys = listOf("di\"")
    val before = "f${c}oo = [\"one\", \"two\", \"three\"];\n"
    val after = "foo = [\"\", \"two\", \"three\"];\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteDoubleQuotedStringOddNumberOfQuotes() {
    val keys = listOf("di\"")
    val before = "abc\"def${c}\"gh\"i"
    val after = "abc\"\"gh\"i"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteDoubleQuotedStringBetweenEvenNumberOfQuotes() {
    val keys = listOf("di\"")
    val before = "abc\"def\"g${c}h\"ijk\"l"
    val after = "abc\"def\"\"ijk\"l"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteDoubleQuotedStringOddNumberOfQuotesOnLast() {
    val keys = listOf("di\"")
    val before = "abcdef\"gh\"ij${c}\"kl"
    val after = "abcdef\"gh\"ij\"kl"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteDoubleQuotedStringEvenNumberOfQuotesOnLast() {
    val keys = listOf("di\"")
    val before = "abc\"def\"gh\"ij${c}\"kl"
    val after = "abc\"def\"gh\"\"kl"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |v_i"|
  @Test
  fun testInnerDoubleQuotedStringSelection() {
    val keys = listOf("vi\"")
    val before = "foo = [\"o${c}ne\", \"two\"];\n"
    val after = "foo = [\"${s}on${c}e${se}\", \"two\"];\n"
    doTest(keys, before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  // |c| |v_i"|
  @Test
  fun testChangeEmptyQuotedString() {
    val keys = listOf("ci\"")
    val before = "foo = \"${c}\";\n"
    val after = "foo = \"\";\n"
    doTest(keys, before, after, Mode.INSERT)
  }

  // VIM-132 |d| |v_i'|
  @Test
  fun testDeleteInnerSingleQuoteString() {
    val keys = listOf("di'")
    val before = "foo = 'bar b${c}az';\n"
    val after = "foo = '';\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |d| |v_i`|
  @Test
  fun testDeleteInnerBackQuoteString() {
    val keys = listOf("di`")
    val before = "foo = `bar b${c}az`;\n"
    val after = "foo = ``;\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |d| |v_a'|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testDeleteOuterSingleQuoteString() {
    val keys = listOf("da'")
    val before = "foo = 'bar b${c}az';\n"
    val after = "foo = ;\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-132 |d| |v_a`|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testDeleteOuterBackQuoteString() {
    val keys = listOf("da`")
    val before = "foo = `bar b${c}az`;\n"
    val after = "foo = ;\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-2733
  @Test
  fun testDeleteOuterQuoteEmptyString() {
    val keys = listOf("da'")
    val before = """
      # Doesn't work <-- note the quote

      print('$c')
    """.trimIndent()
    val after = """
      # Doesn't work <-- note the quote

      print($c)
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

// VIM-2733
  @Test
  fun testDeleteOuterQuoteEmptyString2() {
    val keys = listOf("da'")
    val before = """
      # Doesn't work <-- note the quote

      print($c'')
    """.trimIndent()
    val after = """
      # Doesn't work <-- note the quote

      print($c)
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-2733
  @Test
  fun testDeleteOuterDoubleQuoteEmptyString() {
    val keys = listOf("da\"")
    val before = """
      # This " fails

      print("$c")
    """.trimIndent()
    val after = """
      # This " fails

      print($c)
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-2733
  @Test
  fun testDeleteOuterDoubleQuoteEmptyString2() {
    val keys = listOf("da\"")
    val before = """
      # This " fails

      print($c"")
    """.trimIndent()
    val after = """
      # This " fails

      print($c)
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

// VIM-1427
  @Test
  fun testDeleteOuterTagWithCount() {
    val keys = listOf("d2at")
    val before = "<a><b><c>$c</c></b></a>"
    val after = "<a></a>"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-2113
  @Test
  fun testReplaceEmptyTagContent() {
    val keys = listOf("cit")
    val before = "<a><c>$c</c></a>"
    val after = "<a><c></c></a>"
    doTest(keys, before, after, Mode.INSERT)
  }

  @Test
  fun testDeleteToSearchResult() {
    val before = "Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit"
    val after = "Lorem ${c}sit amet, consectetur adipiscing elit"
    doTest("d/sit<CR>", before, after)
  }

  @Test
  fun testDeleteToLastSearchResult() {
    val before = "Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit"
    val after = "Lorem ${c}sit amet, consectetur adipiscing elit"
    doTest("dn", before, after) {
      enterSearch("sit")
      typeText("0w")  // Cursor back on first "ipsum"
    }
  }

  @Test
  fun testDeleteToSearchResultWithCount() {
    doTest(
      "d3/ipsum<CR>",
      "lorem 1 ipsum lorem 2 ipsum lorem 3 ipsum lorem 4 ipsum lorem 5 ipsum",
      "ipsum lorem 4 ipsum lorem 5 ipsum"
    )
  }

  @Test
  fun testDeleteToSearchResultWithCountAndOperatorCount() {
    doTest(
      "2d3/ipsum<CR>",
      "lorem 1 ipsum lorem 2 ipsum lorem 3 ipsum lorem 4 ipsum lorem 5 ipsum lorem 6 ipsum lorem 7 ipsum",
      "ipsum lorem 7 ipsum"
    )
  }

  @Test
  fun testDeleteToSearchResultWithLinewiseOffset() {
    val before = """
      |First line
      |Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit
      |Lorem ipsum dolor sit amet, consectetur adipiscing elit
      |Lorem ipsum dolor sit amet, consectetur adipiscing elit
      |Lorem ipsum dolor sit amet, consectetur adipiscing elit
      |Last line
      """.trimMargin()
    val after = """
      |First line
      |${c}Last line
      """.trimMargin()
    doTest("d/sit/3<CR>", before, after)
  }

  @Test
  fun testDeleteToEndOfSearchResultInclusive() {
    val before = "Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit"
    val after = "Lorem ${c} amet, consectetur adipiscing elit"
    doTest("d/sit/e<CR>", before, after)
  }

  @Test
  fun testDeleteToEndOfSearchResultWithOffset() {
    val before = "Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit"
    val after = "Lorem ${c}et, consectetur adipiscing elit"
    doTest("d/sit/e+3<CR>", before, after)
  }

  @Test
  fun testDeleteToSearchResultWithIncsearch() {
    val before = "Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit"
    val after = "Lorem ${c}sit amet, consectetur adipiscing elit"
    doTest("d/sit<CR>", before, after) {
      enterCommand("set incsearch")
    }
  }

  @Test
  fun testDeleteToDigraph() {
    val keys = listOf("d/<C-K>O:<CR>")
    val before = "ab${c}cdÖef"
    val after = "abÖef"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteBackwardsToSearchResult() {
    val before = "Lorem ipsum dolor sit amet, ${c}consectetur adipiscing elit"
    val after = "Lorem ipsum dolor ${c}consectetur adipiscing elit"
    doTest("d/sit<CR>", before, after)
  }

  @Test
  fun testDeleteBackwardsToLastSearchResult() {
    val before = "Lorem ipsum dolor sit amet, ${c}consectetur adipiscing elit"
    val after = "Lorem ipsum dolor ${c}elit"
    doTest("dn", before, after) {
      enterSearch("sit", false)
      typeText("\$b") // Cursor to start of "elit"
    }
  }

  @Test
  fun testDeleteBackwardsToSearchResultWithLinewiseOffset() {
    val before = """
      |First line
      |Lorem ipsum dolor sit amet, consectetur ${c}adipiscing elit
      |Lorem ipsum dolor sit amet, consectetur adipiscing elit
      |Lorem ipsum dolor sit amet, consectetur adipiscing elit
      |Lorem ipsum dolor sit amet, consectetur adipiscing elit
      |Last line
      """.trimMargin()
    val after = """
      |First line
      |${c}Last line
      """.trimMargin()
    doTest("d?sit?3<CR>", before, after)
  }

  @Suppress("SpellCheckingInspection")
  @Test
  fun testDeleteBackwardsToEndOfSearchResultInclusive() {
    val before = "Lorem ipsum dolor sit amet, ${c}consectetur adipiscing elit"
    val after = "Lorem ipsum dolor si${c}onsectetur adipiscing elit"
    doTest("d?sit?e<CR>", before, after)
  }

  @Test
  fun testDeleteBackwardsToEndOfSearchResultWithOffset() {
    val before = "Lorem ipsum dolor sit amet, ${c}consectetur adipiscing elit"
    val after = "Lorem ipsum dolor sit a${c}onsectetur adipiscing elit"
    doTest("d?sit?e+3<CR>", before, after)
  }

  @Test
  fun testDeleteBackwardsToSearchResultWithIncsearch() {
    val before = "Lorem ipsum dolor sit amet, ${c}consectetur adipiscing elit"
    val after = "Lorem ipsum dolor ${c}consectetur adipiscing elit"
    doTest("d?sit<CR>", before, after) {
      enterCommand("set incsearch")
    }
  }

  // |[(|
  @Test
  fun testUnmatchedOpenParenthesis() {
    typeTextInFile(
      injector.parser.parseKeys("[("),
      """
     foo(bar, foo(bar, ${c}baz
     bar(foo)
     
      """.trimIndent(),
    )
    assertOffset(12)
  }

  // |[{|
  @Test
  fun testUnmatchedOpenBracketMultiLine() {
    typeTextInFile(
      injector.parser.parseKeys("[{"),
      """foo {
    bar,
    b${c}az
""",
    )
    assertOffset(4)
  }

  // |])|
  @Test
  fun testUnmatchedCloseParenthesisMultiLine() {
    typeTextInFile(
      injector.parser.parseKeys("])"),
      """foo(bar, ${c}baz,
   quux)
""",
    )
    assertOffset(21)
  }

  // |]}|
  @Test
  fun testUnmatchedCloseBracket() {
    typeTextInFile(injector.parser.parseKeys("]}"), "{bar, ${c}baz}\n")
    assertOffset(9)
  }

  // VIM-965 |[m|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT, "File type specific")
  @Test
  fun testMethodMovingInNonJavaFile() {
    configureByJsonText("{\"foo\": \"${c}bar\"}\n")
    typeText(injector.parser.parseKeys("[m"))
    assertState("{\"foo\": \"${c}bar\"}\n")
  }

  // VIM-331 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testNonAsciiLettersInWord() {
    typeTextInFile(injector.parser.parseKeys("w"), "Če${c}ská republika")
    assertOffset(6)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testHiraganaToPunctuation() {
    typeTextInFile(injector.parser.parseKeys("w"), "は${c}はは!!!")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testHiraganaToFullWidthPunctuation() {
    typeTextInFile(injector.parser.parseKeys("w"), "は${c}はは！！！")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testKatakanaToHiragana() {
    typeTextInFile(injector.parser.parseKeys("w"), "チ${c}チチははは")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testKatakanaToHalfWidthKana() {
    typeTextInFile(injector.parser.parseKeys("w"), "チ${c}チチｳｳｳ")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testKatakanaToDigits() {
    typeTextInFile(injector.parser.parseKeys("w"), "チ${c}チチ123")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testKatakanaToLetters() {
    typeTextInFile(injector.parser.parseKeys("w"), "チ${c}チチ123")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testKatakanaToFullWidthLatin() {
    typeTextInFile(injector.parser.parseKeys("w"), "チ${c}チチＡＡＡ")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testKatakanaToFullWidthDigits() {
    typeTextInFile(injector.parser.parseKeys("w"), "チ${c}チチ３３３")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testHiraganaToKatakana() {
    typeTextInFile(injector.parser.parseKeys("w"), "は${c}ははチチチ")
    assertOffset(3)
  }

  // VIM-58 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testHalftWidthKanaToLetters() {
    typeTextInFile(injector.parser.parseKeys("w"), "ｳｳｳAAA")
    assertOffset(3)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testCjkToPunctuation() {
    typeTextInFile(injector.parser.parseKeys("w"), "测试${c}测试!!!")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testCjkToFullWidthPunctuation() {
    typeTextInFile(injector.parser.parseKeys("w"), "测试${c}测试！！！")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testCjkToDigits() {
    typeTextInFile(injector.parser.parseKeys("w"), "测试${c}测试123")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testCjkToFullWidthLatin() {
    typeTextInFile(injector.parser.parseKeys("w"), "测试${c}测试ＡＡＡ")
    assertOffset(4)
  }

  // |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testCjkToFullWidthDigits() {
    typeTextInFile(injector.parser.parseKeys("w"), "测试${c}测试３３３")
    assertOffset(4)
  }

  // |w|
  @Test
  fun testEmptyLineIsWord() {
    typeTextInFile(
      injector.parser.parseKeys("w"),
      """
     ${c}one
     
     two
     
      """.trimIndent(),
    )
    assertOffset(4)
  }

  // |w|
  @Test
  fun testNotEmptyLineIsNotWord() {
    typeTextInFile(
      injector.parser.parseKeys("w"),
      """${c}one
 
two
""",
    )
    assertOffset(6)
  }

  // VIM-312 |w|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testLastWord() {
    typeTextInFile(injector.parser.parseKeys("w"), "${c}one\n")
    assertOffset(2)
  }

  // |b|
  @Test
  fun testWordBackwardsAtFirstLineWithWhitespaceInFront() {
    typeTextInFile(injector.parser.parseKeys("b"), "    ${c}x\n")
    assertOffset(0)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testRightToLastChar() {
    typeTextInFile(injector.parser.parseKeys("i<Right>"), "on${c}e\n")
    assertOffset(3)
  }

  @Test
  fun testDownToLastEmptyLine() {
    typeTextInFile(
      injector.parser.parseKeys("j"),
      """
     ${c}one
     
     
      """.trimIndent(),
    )
    assertOffset(4)
  }

  // VIM-262 |c_CTRL-R|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testSearchFromRegister() {
    VimPlugin.getRegister().setKeys('a', injector.parser.stringToKeys("two"))
    typeTextInFile(
      injector.parser.parseKeys("/" + "<C-R>a" + "<Enter>"),
      """
     ${c}one
     two
     three
     
      """.trimIndent(),
    )
    assertOffset(4)
  }

  // |v_gv|
  @Test
  fun testSwapVisualSelections() {
    val keys = listOf("viw", "<Esc>", "0", "viw", "gv", "d")
    val before = "foo ${c}bar\n"
    val after = "foo \n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |CTRL-V|
  @Test
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
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
  }

  // |CTRL-V|
  @Test
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
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
  }

  // |CTRL-V|
  @Test
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
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
  }

  // |v_o|
  @Test
  fun testSwapVisualSelectionEnds() {
    val keys = listOf("v", "l", "o", "l", "d")
    val before = "${c}foo\n"
    val after = "fo\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-564 |g_|
  @Test
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
      Mode.NORMAL(),
    )
  }

  // |3g_|
  @Test
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
      Mode.NORMAL(),
    )
  }

  // VIM-646 |gv|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  @Test
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
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |v_>| |gv|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Complicated")
  @Test
  fun testRestoreMultiLineSelectionAfterIndent() {
    typeTextInFile(
      injector.parser.parseKeys("V" + "2j"),
      """
     ${c}foo
     bar
     baz
     
      """.trimIndent(),
    )
    assertSelection(
      """
    foo
    bar
    baz
    
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys(">"))
    assertMode(Mode.NORMAL())
    assertState(
      """    foo
    bar
    baz
""",
    )
    typeText(injector.parser.parseKeys("gv"))
    assertSelection(
      """    foo
    bar
    baz
""",
    )
    typeText(injector.parser.parseKeys(">"))
    assertMode(Mode.NORMAL())
    assertState(
      """        foo
        bar
        baz
""",
    )
    typeText(injector.parser.parseKeys("gv"))
    assertSelection(
      """        foo
        bar
        baz
""",
    )
  }

  // VIM-862 |gv|
  @Test
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
    doTest(keys, before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun testVisualLineSelectDown() {
    typeTextInFile(
      injector.parser.parseKeys("Vj"),
      """
     foo
     ${c}bar
     baz
     quux
     
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertSelection(
      """
    bar
    baz
    
      """.trimIndent(),
    )
    assertOffset(8)
  }

  // VIM-784
  @Test
  fun testVisualLineSelectUp() {
    typeTextInFile(
      injector.parser.parseKeys("Vk"),
      """
     foo
     bar
     ${c}baz
     quux
     
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertSelection(
      """
    bar
    baz
    
      """.trimIndent(),
    )
    assertOffset(4)
  }

  @Test
  fun `test gv after backwards selection`() {
    configureByText("${c}Oh, hi Mark\n")
    typeText("yw", "$", "vb", "p", "gv")
    assertSelection("Oh")
  }

  @Test
  fun `test gv after linewise selection`() {
    configureByText("${c}Oh, hi Mark\nOh, hi Markus\n")
    typeText("V", "y", "j", "V", "p", "gv")
    assertSelection("Oh, hi Mark\n")
  }
}
