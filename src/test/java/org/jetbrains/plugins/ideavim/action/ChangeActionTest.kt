/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author vlan
 */
class ChangeActionTest : VimTestCase() {
  // VIM-620 |i_CTRL-O|
  @Test
  fun testInsertSingleCommandAndInserting() {
    doTest(
      listOf("i", "<C-O>", "a", "123", "<Esc>", "x"),
      "abc${c}d\n",
      "abcd12\n",
      Mode.NORMAL(),
    )
  }

  // VIM-620 |i_CTRL-O|
  @Test
  fun testInsertSingleCommandAndNewLineInserting() {
    doTest(
      listOf("i", "<C-O>", "o", "123", "<Esc>", "x"),
      "abc${c}d\n",
      "abcd\n12\n",
      Mode.NORMAL(),
    )
  }

  // VIM-620 |i_CTRL-O|
  @Test
  fun testInsertSingleCommandAndNewLineInserting2() {
    doTest(
      listOf("i", "<C-O>", "v"),
      "12${c}345",
      "12${s}${c}3${se}45",
      Mode.VISUAL(SelectionType.CHARACTER_WISE, Mode.INSERT)
    )
  }

  // VIM-620 |i_CTRL-O|
  @Test
  fun testInsertSingleCommandAndNewLineInserting3() {
    doTest(
      listOf("i", "<C-O>", "v", "<esc>"),
      "12${c}345",
      "12${c}345",
      Mode.INSERT,
    )
  }

  // VIM-620 |i_CTRL-O|
  @Test
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  fun testInsertSingleCommandAndNewLineInserting4() {
    doTest(
      listOf("i", "<C-O>", "v", "d"),
      "12${c}345",
      "12${c}45",
      Mode.INSERT,
    )
  }

  // VIM-620 |i_CTRL-O|
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun testInsertSingleCommandAndNewLineInserting5() {
    doTest(
      listOf("i", "<C-O>", "v", "<C-G>"),
      "12${c}345",
      "12${s}3${c}${se}45",
      Mode.SELECT(SelectionType.CHARACTER_WISE, Mode.INSERT),
    )
  }

  // VIM-620 |i_CTRL-O|
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun testInsertSingleCommandAndNewLineInserting6() {
    doTest(
      listOf("i", "<C-O>", "gh"),
      "12${c}345",
      "12${s}3${c}${se}45",
      Mode.SELECT(SelectionType.CHARACTER_WISE, Mode.INSERT),
    )
  }

  // VIM-620 |i_CTRL-O|
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun testInsertSingleCommandAndNewLineInserting7() {
    doTest(
      listOf("i", "<C-O>", "gh", "<esc>"),
      "12${c}345",
      "123${c}45",
      Mode.INSERT,
    )
  }

  // VIM-620 |i_CTRL-O|
  @Test
  fun testInsertSingleCommandAndNewLineInserting8() {
    doTest(
      listOf("i", "<C-O>", "gh", "d"),
      "12${c}345",
      "12d${c}45",
      Mode.INSERT,
    )
  }

  // VIM-311 |i_CTRL-O|
  @Test
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  fun testInsertSingleCommand() {
    doTest(
      listOf("i", "def", "<C-O>", "d2h", "x"),
      "abc$c.\n",
      "abcdx.\n",
      Mode.INSERT,
    )
  }

  // VIM-321 |d| |count|
  @Test
  fun testDeleteEmptyRange() {
    doTest(
      "d0",
      "${c}hello\n",
      "hello\n",
      Mode.NORMAL(),
    )
  }

  // VIM-157 |~|
  @Test
  fun testToggleCharCase() {
    doTest(
      "~~",
      "${c}hello world\n",
      "HEllo world\n",
      Mode.NORMAL(),
    )
  }

  // VIM-157 |~|
  @Test
  fun testToggleCharCaseLineEnd() {
    doTest(
      "~~",
      "hello wor${c}ld\n",
      "hello worLD\n",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testToggleCaseMotion() {
    doTest(
      "g~w",
      "${c}FooBar Baz\n",
      "fOObAR Baz\n",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testChangeUpperCase() {
    doTest(
      "gUw",
      "${c}FooBar Baz\n",
      "FOOBAR Baz\n",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testChangeLowerCase() {
    doTest(
      "guw",
      "${c}FooBar Baz\n",
      "foobar Baz\n",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testToggleCaseVisual() {
    doTest(
      "ve~",
      "${c}FooBar Baz\n",
      "fOObAR Baz\n",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testChangeUpperCaseVisual() {
    doTest(
      "veU",
      "${c}FooBar Baz\n",
      "FOOBAR Baz\n",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testChangeLowerCaseVisual() {
    doTest(
      "veu",
      "${c}FooBar Baz\n",
      "foobar Baz\n",
      Mode.NORMAL(),
    )
  }

  // VIM-85 |i| |gi| |gg|
  @Test
  fun testInsertAtPreviousAction() {
    doTest(
      listOf("i", "hello", "<Esc>", "gg", "gi", " world! "),
      """
   one
   two ${c}three
   four

      """.trimIndent(),
      """
   one
   two hello world! three
   four

      """.trimIndent(),
      Mode.INSERT,
    )
  }

  // VIM-312 |d| |w|
  @Test
  fun testDeleteLastWordInFile() {
    doTest(
      "dw",
      """
        one
        ${c}two

      """.trimIndent(),
      """
        one


      """.trimIndent(),
      Mode.NORMAL(),
    )
    assertOffset(4)
  }

  // |d| |w|
  @Test
  fun testDeleteLastWordBeforeEOL() {
    doTest(
      "dw",
      """
   one ${c}two
   three

      """.trimIndent(),
      """
   one 
   three

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-105 |d| |w|
  @Test
  fun testDeleteLastWordBeforeEOLs() {
    doTest(
      "dw",
      """
   one ${c}two

   three

      """.trimIndent(),
      """
   one 

   three

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-105 |d| |w|
  @Test
  fun testDeleteLastWordBeforeEOLAndWhitespace() {
    doTest(
      "dw",
      """
        one ${c}two
         three
      """.trimIndent(),
      """
        one 
         three
      """.trimIndent(),
      Mode.NORMAL(),
    )
    assertOffset(3)
  }

  // VIM-105 |d| |w| |count|
  @Test
  fun testDeleteTwoWordsOnTwoLines() {
    doTest(
      "d2w",
      """
   one ${c}two
   three four

      """.trimIndent(),
      "one four\n",
      Mode.NORMAL(),
    )
  }

  // VIM-1380 |d| |w| |count|
  @Test
  fun testDeleteTwoWordsAtLastChar() {
    doTest(
      "d2w",
      "on${c}e two three\n",
      "on${c}three\n",
      Mode.NORMAL(),
    )
  }

  // VIM-394 |d| |v_aw|
  @Test
  fun testDeleteIndentedWordBeforePunctuation() {
    doTest(
      "daw",
      """foo
  ${c}bar, baz
""",
      """foo
  , baz
""",
      Mode.NORMAL(),
    )
  }

  // |d| |v_aw|
  @Test
  fun testDeleteLastWordAfterPunctuation() {
    doTest(
      "daw",
      """
   foo(${c}bar
   baz

      """.trimIndent(),
      """
   foo(
   baz

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-244 |d| |l|
  @Test
  fun testDeleteLastCharInLine() {
    doTest(
      "dl",
      """
        fo${c}o
        bar

      """.trimIndent(),
      """
        fo
        bar

      """.trimIndent(),
      Mode.NORMAL(),
    )
    assertOffset(1)
  }

  // VIM-393 |d|
  @Test
  fun testDeleteBadArgument() {
    doTest(
      listOf("dD", "dd"),
      """
   one
   two

      """.trimIndent(),
      "two\n",
      Mode.NORMAL(),
    )
  }

  // VIM-262 |i_CTRL-R|
  @Test
  fun testInsertFromRegister() {
    setRegister('a', "World")
    doTest(
      listOf("A", ", ", "<C-R>", "a", "!"),
      "${c}Hello\n",
      "Hello, World!\n",
      Mode.INSERT,
    )
  }

  // VIM-404 |O|
  @Test
  fun testInsertNewLineAboveFirstLine() {
    doTest(
      listOf("O", "bar"),
      "fo${c}o\n",
      "bar\nfoo\n",
      Mode.INSERT,
    )
  }

  // VIM-472 |v|
  @Test
  fun testVisualSelectionRightMargin() {
    doTest(
      listOf("v", "k\$d"),
      "foo\n${c}bar\n",
      "fooar\n",
      Mode.NORMAL(),
    )
  }

  // VIM-632 |CTRL-V| |v_d|
  @Test
  fun testDeleteVisualBlock() {
    doTest(
      listOf("<C-V>", "jjl", "d"),
      """
        ${c}foo
        bar
        baz
        quux

      """.trimIndent(),
      """
        ${c}o
        r
        z
        quux

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteCharVisualBlock() {
    doTest(
      listOf("<C-V>", "jjl", "x"),
      """
        ${c}foo
        bar
        baz
        quux

      """.trimIndent(),
      """
        ${c}o
        r
        z
        quux

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteJoinLinesSpaces() {
    doTest(
      "3J",
      """    a$c 1
    b 2
    c 3
quux
""",
      """    a 1 b 2 c 3
quux
""",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteJoinLines() {
    doTest(
      "3gJ",
      """    a$c 1
    b 2
    c 3
quux
""",
      """    a 1    b 2    c 3
quux
""",
      Mode.NORMAL(),
    )
  }

  @VimBehaviorDiffers(originalVimAfter = "foo  bar")
  @Test
  fun testDeleteJoinLinesWithTrailingSpaceThenEmptyLine() {
    doTest(
      "3J",
      """
        foo.

        bar
      """.dotToSpace().trimIndent(),
      "foo bar",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteJoinLinesWithTwoTrailingSpaces() {
    doTest(
      "J",
      """
        foo..
        bar
      """.dotToSpace().trimIndent(),
      "foo  bar",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteJoinVisualLinesSpaces() {
    doTest(
      "v2jJ",
      """
        a$c 1
        b 2
        c 3
        quux
      """.trimIndent(),
      """
        a 1 b 2 c 3
        quux
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteJoinVisualLines() {
    doTest(
      "v2jgJ",
      """    a$c 1
    b 2
    c 3
quux
""",
      """    a 1    b 2    c 3
quux
""",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteCharVisualBlockOnLastCharOfLine() {
    doTest(
      listOf("<C-V>", "x"),
      "fo${c}o\n",
      "fo\n",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteCharVisualBlockOnEmptyLinesDoesntDeleteAnything() {
    setupChecks {
      this.neoVim.ignoredRegisters = setOf('1', '"')
    }
    doTest(
      listOf("<C-V>", "j", "x"),
      "\n\n",
      "\n\n",
      Mode.NORMAL(),
    )
  }

  // VIM-781 |CTRL-V| |j|
  @Test
  fun testDeleteCharVisualBlockWithEmptyLineInTheMiddle() {
    doTest(
      listOf("l", "<C-V>", "jj", "x"),
      """
        foo

        bar

      """.trimIndent(),
      """
        fo

        br

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-781 |CTRL-V| |j|
  @VimBehaviorDiffers(description = "Different registers content")
  @Test
  fun testDeleteCharVisualBlockWithShorterLineInTheMiddle() {
    doTest(
      listOf("l", "<C-V>", "jj", "x"),
      """
        foo
        x
        bar

      """.trimIndent(),
      """
        fo
        x
        br

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-845 |CTRL-V| |x|
  @Test
  fun testDeleteVisualBlockOneCharWide() {
    configureByText(
      """
  foo
  bar

      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("<C-V>" + "j" + "x"))
    assertState(
      """
  oo
  ar

      """.trimIndent(),
    )
  }

  // |r|
  @Test
  fun testReplaceOneChar() {
    doTest(
      "rx",
      "b${c}ar\n",
      "b${c}xr\n",
      Mode.NORMAL(),
    )
  }

  // |r|
  @VimBehaviorDiffers(originalVimAfter = "foXX${c}Xr\n")
  @Test
  fun testReplaceMultipleCharsWithCount() {
    doTest(
      "3rX",
      "fo${c}obar\n",
      "fo${c}XXXr\n",
      Mode.NORMAL(),
    )
  }

  // |r|
  @Test
  fun testReplaceMultipleCharsWithCountPastEndOfLine() {
    doTest(
      "6rX",
      "fo${c}obar\n",
      "fo${c}obar\n",
      Mode.NORMAL(),
    )
  }

  // |r|
  @VimBehaviorDiffers(description = "Different caret position")
  @Test
  fun testReplaceMultipleCharsWithVisual() {
    doTest(
      listOf("v", "ll", "j", "rZ"),
      """
        fo${c}obar
        foobaz

      """.trimIndent(),
      """
        foZZZZ
        ZZZZZz

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // |r|
  @Test
  fun testReplaceOneCharWithNewline() {
    doTest(
      "r<Enter>",
      """
        fo${c}obar
        foobaz
      """.trimIndent(),
      """
        fo
        bar
        foobaz
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // |r|
  @VimBehaviorDiffers(description = "Different caret position")
  @Test
  fun testReplaceCharWithNewlineAndCountAddsOnlySingleNewline() {
    doTest(
      "3r<Enter>",
      """
        fo${c}obar
        foobaz
      """.trimIndent(),
      """
        fo
        r
        foobaz
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // |s|
  @Test
  fun testReplaceOneCharWithText() {
    doTest(
      "sxy<Esc>",
      "b${c}ar\n",
      "bx${c}yr\n",
      Mode.NORMAL(),
    )
  }

  // |s|
  @Test
  fun testReplaceMultipleCharsWithTextWithCount() {
    doTest(
      "3sxy<Esc>",
      "fo${c}obar\n",
      "fox${c}yr\n",
      Mode.NORMAL(),
    )
  }

  // |s|
  @Test
  fun testReplaceMultipleCharsWithTextWithCountPastEndOfLine() {
    doTest(
      "99sxyz<Esc>",
      """
        foo${c}bar
        biff

      """.trimIndent(),
      """
        fooxy${c}z
        biff

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // |R|
  @Test
  fun testReplaceMode() {
    doTest(
      "Rbaz<Esc>",
      "foo${c}bar\n",
      "fooba${c}z\n",
      Mode.NORMAL(),
    )
  }

  // |R| |i_<Insert>|
  @VimBehaviorDiffers(description = "Different caret position")
  @Test
  fun testReplaceModeSwitchToInsertModeAndBack() {
    doTest(
      "RXXX<Ins>YYY<Ins>ZZZ<Esc>",
      "aaa${c}bbbcccddd\n",
      "aaaXXXYYYZZ${c}Zddd\n",
      Mode.NORMAL(),
    )
  }

  // |i| |i_<Insert>|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "<INS> works strange")
  @Test
  fun testInsertModeSwitchToReplaceModeAndBack() {
    doTest(
      "iXXX<Ins>YYY<Ins>ZZZ<Esc>",
      "aaa${c}bbbcccddd\n",
      "aaaXXXYYYZZ${c}Zcccddd\n",
      Mode.NORMAL(),
    )
  }

  // VIM-511 |.|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "Backspace workspace strange")
  @Test
  fun testRepeatWithBackspaces() {
    doTest(
      listOf("ce", "foo", "<BS><BS><BS>", "foo", "<Esc>", "j0", "."),
      """
        ${c}foo baz
        baz quux

      """.trimIndent(),
      """
        foo baz
        fo${c}o quux

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-613 |.|
  @Test
  fun testDeleteEndOfLineAndAgain() {
    configureByText(
      """
  $c- 1
  - 2
  - 3

      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("d$" + "j" + "."))
    assertState(
      """


  - 3

      """.trimIndent(),
    )
  }


  // VIM-1067 |.|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testRepeatWithInsertAfterLineEnd() {
    // Case 1
    configureByText(
      """
  $c- 1
  - 2
  - 3

      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("A" + "<BS>" + "<Esc>" + "j" + "."))
    assertState(
      """
  - 
  - 
  - 3

      """.trimIndent(),
    )

    // Case 2
    configureByText(
      """
  $c- 1
  - 2
  - 3

      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("A" + "4" + "<BS>" + "<Esc>" + "j" + "."))
    assertState(
      """
  - 1
  - 2
  - 3

      """.trimIndent(),
    )

    // Case 3
    configureByText(
      """
  $c- 1
  - 2
  - 3

      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("A" + "<BS>" + "4" + "<Esc>" + "j" + "."))
    assertState(
      """
  - 4
  - 4
  - 3

      """.trimIndent(),
    )
  }

  @Test
  fun testRepeatChangeWordDoesNotBreakNextRepeatFind() {
    doTest(
      "fXcfYPATATA<Esc>fX.;.",
      "${c}aaaaXBBBBYaaaaaaaXBBBBYaaaaaaXBBBBYaaaaaaaa\n",
      "aaaaPATATAaaaaaaaPATATAaaaaaaPATATAaaaaaaaa\n",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testRepeatReplace() {
    configureByText("${c}foobarbaz spam\n")
    typeText(injector.parser.parseKeys("R"))
    assertMode(Mode.REPLACE)
    typeText(injector.parser.parseKeys("FOO" + "<Esc>" + "l" + "2."))
    assertState("FOOFOOFO${c}O spam\n")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun testDownMovementAfterDeletionToStart() {
    doTest(
      "ld^j",
      """
        lorem ${c}ipsum dolor sit amet
        Lorem Ipsumm dolor sit amet
      """.trimIndent(),
      """
        psum dolor sit amet
        ${c}Lorem Ipsumm dolor sit amet
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDownMovementAfterDeletionToPrevWord() {
    doTest(
      "ldbj",
      """
        lorem$c ipsum dolor sit amet
        Lorem Ipsumm dolor sit amet
      """.trimIndent(),
      """
        ipsum dolor sit amet
        ${c}Lorem Ipsumm dolor sit amet
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDownMovementAfterChangeToPrevWord() {
    doTest(
      "lcb<Esc>j",
      """
        lorem$c ipsum dolor sit amet
        Lorem Ipsumm dolor sit amet
      """.trimIndent(),
      """
        ipsum dolor sit amet
        ${c}Lorem Ipsumm dolor sit amet
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDownMovementAfterChangeToLineStart() {
    doTest(
      "lc^<Esc>j",
      """
        lorem$c ipsum dolor sit amet
        Lorem Ipsumm dolor sit amet
      """.trimIndent(),
      """
        ipsum dolor sit amet
        ${c}Lorem Ipsumm dolor sit amet
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun testUpMovementAfterDeletionToStart() {
    doTest(
      "ld^k",
      """
        Lorem Ipsumm dolor sit amet
        lorem ${c}ipsum dolor sit amet
      """.trimIndent(),
      """
        ${c}Lorem Ipsumm dolor sit amet
        psum dolor sit amet
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun testUpMovementAfterChangeToPrevWord() {
    doTest(
      "lcb<Esc>k",
      """
        Lorem Ipsumm dolor sit amet
        lorem$c ipsum dolor sit amet
      """.trimIndent(),
      """
        ${c}Lorem Ipsumm dolor sit amet
        ipsum dolor sit amet
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-714 |v|
  @Test
  fun testDeleteVisualColumnPositionOneLine() {
    doTest(
      "vwxj",
      """
        ${c}lorem ipsum dolor sit amet
        lorem ipsum dolor sit amet

      """.trimIndent(),
      """
        psum dolor sit amet
        ${c}lorem ipsum dolor sit amet

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-714 |v|
  @Test
  fun testDeleteVisualColumnPositionMultiLine() {
    doTest(
      "v3wfixj",
      """
        gaganis ${c}gaganis gaganis
        gaganis gaganis gaganis
        gaganis gaganis gaganis

      """.trimIndent(),
      """
        gaganis s gaganis
        gaganis ${c}gaganis gaganis

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun testChangeSameLine() {
    doTest(
      "d_",
      """
        line 1
        line$c 2
        line 3
      """.trimIndent(),
      """
        line 1
        ${c}line 3
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-2074"])
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  fun `backspace with replace mode`() {
    configureByText("${c}Hello world")
    typeText("R1111")
    assertState("1111o world")
    typeText("<BS><BS><BS>")
    assertState("1ello world")
  }

  @Test
  fun `backspace with replace modes`() {
    configureByText("")
    typeText("c<C-Home><Esc>.")
    assertState("")
    assertPluginError(false)
  }
}
