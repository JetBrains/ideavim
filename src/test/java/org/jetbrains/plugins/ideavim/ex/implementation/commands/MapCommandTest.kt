/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.history.HistoryConstants
import com.maddyhome.idea.vim.newapi.vim
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssert
import javax.swing.JTextArea

/**
 * @author vlan
 */
class MapCommandTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testMapKtoJ() {
    configureByText(
      """
  ${c}foo
  bar
  
      """.trimIndent()
    )
    typeText(commandToKeys("nmap k j"))
    assertPluginError(false)
    assertOffset(0)
    typeText(injector.parser.parseKeys("k"))
    assertOffset(4)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testInsertMapJKtoEsc() {
    configureByText("${c}World!\n")
    typeText(commandToKeys("imap jk <Esc>"))
    assertPluginError(false)
    typeText(injector.parser.parseKeys("i" + "Hello, " + "jk"))
    assertState("Hello, World!\n")
    assertMode(VimStateMachine.Mode.COMMAND)
    assertOffset(6)
  }

  fun testBackslashAtEnd() {
    configureByText("\n")
    typeText(commandToKeys("imap foo\\ bar"))
    assertPluginError(false)
    typeText(injector.parser.stringToKeys("ifoo\\"))
    assertState("bar\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replace term codes")
  fun testUnfinishedSpecialKey() {
    configureByText("\n")
    typeText(commandToKeys("imap <Esc foo"))
    typeText(injector.parser.stringToKeys("i<Esc"))
    assertState("foo\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testUnknownSpecialKey() {
    configureByText("\n")
    typeText(commandToKeys("imap <foo> bar"))
    typeText(injector.parser.stringToKeys("i<foo>"))
    assertState("bar\n")
  }

  fun testMapTable() {
    configureByText("\n")
    typeText(commandToKeys("map <C-Down> gt"))
    typeText(commandToKeys("imap foo bar"))
    typeText(commandToKeys("imap bar <Esc>"))
    typeText(commandToKeys("imap <C-Down> <C-O>gt"))
    typeText(commandToKeys("nmap ,f <Plug>Foo"))
    typeText(commandToKeys("nmap <Plug>Foo iHello<Esc>"))
    typeText(commandToKeys("imap"))
    assertExOutput(
      """
  i  <C-Down>      <C-O>gt
  i  bar           <Esc>
  i  foo           bar
  
      """.trimIndent()
    )
    typeText(commandToKeys("map"))
    assertExOutput(
      """   <C-Down>      gt
n  <Plug>Foo     iHello<Esc>
n  ,f            <Plug>Foo
"""
    )
  }

  fun testRecursiveMapping() {
    configureByText("\n")
    typeText(commandToKeys("imap foo bar"))
    typeText(commandToKeys("imap bar baz"))
    typeText(commandToKeys("imap baz quux"))
    typeText(injector.parser.parseKeys("i" + "foo"))
    assertState("quux\n")
  }

  fun testddWithMapping() {
    configureByText(
      """
      Hello$c 1
      Hello 2
      """.trimIndent()
    )
    typeText(commandToKeys("nmap dc k"))
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      Hello 2
      """.trimIndent()
    )
  }

  fun testNonRecursiveMapping() {
    configureByText("\n")
    typeText(commandToKeys("inoremap a b"))
    assertPluginError(false)
    typeText(commandToKeys("inoremap b a"))
    typeText(injector.parser.parseKeys("i" + "ab"))
    assertState("ba\n")
  }

  fun testNonRecursiveMapTable() {
    configureByText("\n")
    typeText(commandToKeys("inoremap jj <Esc>"))
    typeText(commandToKeys("imap foo bar"))
    typeText(commandToKeys("imap"))
    assertExOutput(
      """
  i  foo           bar
  i  jj          * <Esc>
  
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testNop() {
    configureByText(
      """
  ${c}foo
  bar
  
      """.trimIndent()
    )
    typeText(commandToKeys("noremap <Right> <nop>"))
    assertPluginError(false)
    typeText(injector.parser.parseKeys("l" + "<Right>"))
    assertPluginError(false)
    assertState(
      """
  foo
  bar
  
      """.trimIndent()
    )
    assertOffset(1)
    typeText(commandToKeys("nmap"))
    assertExOutput("n  <Right>     * <Nop>\n")
  }

  fun testIgnoreModifiers() {
    configureByText("\n")
    typeText(commandToKeys("nmap <buffer> ,a /a<CR>"))
    typeText(commandToKeys("nmap <nowait> ,b /b<CR>"))
    typeText(commandToKeys("nmap <silent> ,c /c<CR>"))
    typeText(commandToKeys("nmap <special> ,d /d<CR>"))
    typeText(commandToKeys("nmap <script> ,e /e<CR>"))
    typeText(commandToKeys("nmap <expr> ,f '/f<CR>'"))
    typeText(commandToKeys("nmap <unique> ,g /g<CR>"))
    typeText(commandToKeys("nmap"))
    assertExOutput(
      """
  n  ,a            /a<CR>
  n  ,b            /b<CR>
  n  ,c            /c<CR>
  n  ,d            /d<CR>
  n  ,f            '/f<CR>'
  n  ,g            /g<CR>
  
      """.trimIndent()
    )
  }

  // VIM-645 |:nmap|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  fun testMapSpace() {
    configureByText("foo\n")
    typeText(commandToKeys("nmap <space> dw"))
    typeText(injector.parser.parseKeys(" "))
    assertState("\n")
    typeText(injector.parser.parseKeys("i" + " " + "<Esc>"))
    assertState(" \n")
  }

  // VIM-661 |:noremap| |r|
  fun testNoMappingInReplaceCharacterArgument() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("noremap A Z"))
    typeText(injector.parser.parseKeys("rA"))
    assertState("Aoo\n")
  }

  // VIM-661 |:omap| |d| |t|
  fun testNoMappingInNonFirstCharOfOperatorPendingMode() {
    configureByText("${c}foo, bar\n")
    typeText(commandToKeys("omap , ?"))
    typeText(injector.parser.parseKeys("dt,"))
    assertState(", bar\n")
  }

  // VIM-666 |:imap|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testIgnoreEverythingAfterBar() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("imap a b |c \" Something else"))
    typeText(injector.parser.parseKeys("ia"))
    assertState("b foo\n")
  }

  // VIM-666 |:imap|
  fun testBarEscaped() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("imap a b \\| c"))
    typeText(injector.parser.parseKeys("ia"))
    assertState("b | cfoo\n")
  }

  // VIM-666 |:imap|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testBarEscapedSeveralSpaces() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("imap a b \\| c    |"))
    typeText(injector.parser.parseKeys("ia"))
    assertState("b | c    foo\n")
  }

  // VIM-670 |:map|
  fun testFirstCharIsNonRecursive() {
    configureByText("\n")
    typeText(commandToKeys("map ab abcd"))
    typeText(injector.parser.parseKeys("ab"))
    assertState("bcd\n")
  }

  // VIM-676 |:map|
  @TestWithoutNeovim(reason = SkipNeovimReason.VIM_SCRIPT)
  fun testBackspaceCharacterInVimRc() {
    configureByText("\n")
    injector.vimscriptExecutor.execute("inoremap # X\u0008#\n")
    typeText(injector.parser.parseKeys("i" + "#" + "<Esc>"))
    assertState("#\n")
    assertMode(VimStateMachine.Mode.COMMAND)
    typeText(commandToKeys("imap"))
    assertExOutput("i  #           * X<C-H>#\n")
  }

  // VIM-679 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testCancelCharacterInVimRc() {
    configureByText(
      """
  ${c}foo
  bar
  
      """.trimIndent()
    )
    injector.vimscriptExecutor.execute("map \u0018i dd\n", true)
    typeText(injector.parser.parseKeys("i" + "#" + "<Esc>"))
    assertState(
      """
  #foo
  bar
  
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.COMMAND)
    typeText(commandToKeys("map"))
    assertExOutput("   <C-X>i        dd\n")
    typeText(injector.parser.parseKeys("<C-X>i"))
    assertState("bar\n")
  }

  // VIM-679 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testBarCtrlVEscaped() {
    configureByText("${c}foo\n")
    injector.vimscriptExecutor.execute("imap a b \u0016|\u0016| c |\n")
    typeText(injector.parser.parseKeys("ia"))
    assertState("b || c foo\n")
  }

  // VIM-679 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad term codes")
  fun testCtrlMCtrlLAsNewLine() {
    configureByText("${c}foo\n")
    injector.vimscriptExecutor.execute("map A :%s/foo/bar/g\r\u000C\n")
    typeText(injector.parser.parseKeys("A"))
    assertState("bar\n")
  }

  // VIM-700 |:map|
  fun testRemappingZero() {
    configureByText("x${c}yz\n")
    typeText(commandToKeys("map 0 ~"))
    typeText(injector.parser.parseKeys("0"))
    assertState("xYz\n")
  }

  // VIM-700 |:map|
  @TestWithoutNeovim(reason = SkipNeovimReason.VIM_SCRIPT)
  fun testRemappingZeroStillAllowsZeroToBeUsedInCount() {
    configureByText("a${c}bcdefghijklmnop\n")
    injector.vimscriptExecutor.execute("map 0 ^")
    typeText(injector.parser.parseKeys("10~"))
    assertState("aBCDEFGHIJKlmnop\n")
  }

  // VIM-700 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad term codes")
  fun testRemappingDeleteOverridesRemovingLastDigitFromCount() {
    configureByText("a${c}bcdefghijklmnop\n")
    typeText(commandToKeys("map <Del> ~"))
    typeText(injector.parser.parseKeys("10<Del>"))
    assertState("aBCDEFGHIJKlmnop\n")
  }

  // VIM-650 |mapleader|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  fun testMapLeader() {
    configureByText("\n")
    typeText(commandToKeys("let mapleader = \",\""))
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"))
    typeText(injector.parser.parseKeys(",z"))
    assertState("zzz\n")
  }

  // VIM-650 |mapleader|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  fun testMapLeaderToSpace() {
    configureByText("\n")
    typeText(commandToKeys("let mapleader = \"\\<SPACE>\""))
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"))
    typeText(injector.parser.parseKeys(" z"))
    assertState("zzz\n")
  }

  // VIM-650 |mapleader|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  fun testMapLeaderToSpaceWithWhitespace() {
    configureByText("\n")
    typeText(commandToKeys("let mapleader = \" \""))
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"))
    typeText(injector.parser.parseKeys(" z"))
    assertState("zzz\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replace term codes")
  fun testAmbiguousMapping() {
    configureByText("\n")
    typeText(commandToKeys("nmap ,f iHello<Esc>"))
    typeText(commandToKeys("nmap ,fc iBye<Esc>"))
    typeText(injector.parser.parseKeys(",fdh"))
    assertState("Helo\n")
    typeText(injector.parser.parseKeys("diw"))
    assertState("\n")
    typeText(injector.parser.parseKeys(",fch"))
    assertState("Bye\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad term codes")
  fun testLongAmbiguousMapping() {
    configureByText("\n")
    typeText(commandToKeys("nmap ,foo iHello<Esc>"))
    typeText(commandToKeys("nmap ,fooc iBye<Esc>"))
    typeText(injector.parser.parseKeys(",foodh"))
    assertState("Helo\n")
    typeText(injector.parser.parseKeys("diw"))
    assertState("\n")
    typeText(injector.parser.parseKeys(",fooch"))
    assertState("Bye\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUG)
  fun testPlugMapping() {
    configureByText("\n")
    typeText(commandToKeys("nmap ,f <Plug>Foo"))
    typeText(commandToKeys("nmap <Plug>Foo iHello<Esc>"))
    typeText(injector.parser.parseKeys(",fa!<Esc>"))
    assertState("Hello!\n")
  }

  fun testIntersectingCommands() {
    configureByText("123${c}4567890")
    typeText(commandToKeys("map ds h"))
    typeText(commandToKeys("map I 3l"))
    typeText(injector.parser.parseKeys("dI"))
    assertState("123${c}7890")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUG)
  fun testIncompleteMapping() {
    configureByText("123${c}4567890")
    typeText(commandToKeys("map <Plug>(Hi)l lll"))
    typeText(commandToKeys("map I <Plug>(Hi)"))
    typeText(injector.parser.parseKeys("Ih"))
    assertState("12${c}34567890")
  }

  fun testIntersectingCommands2() {
    configureByText("123${c}4567890")
    typeText(commandToKeys("map as x"))
    typeText(injector.parser.parseKeys("gas"))
    assertState("123${c}567890")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testMapZero() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 0 w"))
    typeText(injector.parser.parseKeys("0"))
    assertOffset(14)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testMapZeroIgnoredInCount() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 0 w"))
    typeText(injector.parser.parseKeys("10w"))
    assertOffset(51)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testMapNonZeroDigit() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 2 w"))
    typeText(injector.parser.parseKeys("2"))
    assertOffset(14)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testMapNonZeroDigitNotIncludedInCount() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 2 w"))
    typeText(injector.parser.parseKeys("92"))
    assertOffset(45)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  fun testShiftSpace() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-Space> w"))
    typeText(injector.parser.parseKeys("<S-Space>"))
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  fun testShiftSpaceAndWorkInInsertMode() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-Space> w"))
    typeText(injector.parser.parseKeys("i<S-Space>"))
    assertState("A quick  ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  fun testShiftLetter() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-D> w"))
    typeText(injector.parser.parseKeys("<S-D>"))
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  fun testUppercaseLetter() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap D w"))
    typeText(injector.parser.parseKeys("D"))
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  fun `test shift letter doesn't break insert mode`() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-D> w"))
    typeText(injector.parser.parseKeys("<S-D>"))
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")

    typeText(injector.parser.parseKeys("iD<Esc>"))
    assertState("A quick brown ${c}Dfox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun `test comment line with action`() {
    configureByJavaText(
      """
        -----
        1<caret>2345
        abcde
        -----
      """.trimIndent()
    )
    typeText(commandToKeys("map k <Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("k"))
    assertState(
      """
        -----
        //12345
        abcde
        -----
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun `test execute two actions with two mappings`() {
    configureByJavaText(
      """
          -----
          1<caret>2345
          abcde
          -----
      """.trimIndent()
    )
    typeText(commandToKeys("map k <Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("kk"))
    assertState(
      """
          -----
          //12345
          //abcde
          -----
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun `test execute two actions with single mappings`() {
    configureByJavaText(
      """
          -----
          1<caret>2345
          abcde
          -----
      """.trimIndent()
    )
    typeText(commandToKeys("map k <Action>(CommentByLineComment)<Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("k"))
    assertState(
      """
          -----
          //12345
          //abcde
          -----
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun `test execute three actions with single mappings`() {
    configureByJavaText(
      """
          -----
          1<caret>2345
          abcde
          -----
      """.trimIndent()
    )
    typeText(commandToKeys("map k <Action>(CommentByLineComment)<Action>(CommentByLineComment)<Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("k"))
    assertState(
      """
          -----
          //12345
          //abcde
          //-----
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun `test execute action from insert mode`() {
    configureByJavaText(
      """
          -----
          1<caret>2345
          abcde
          -----
      """.trimIndent()
    )
    typeText(commandToKeys("imap k <Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("ik"))
    assertState(
      """
          -----
          //12345
          abcde
          -----
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun `test execute mapping with a delay`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map kk l"))
    typeText(injector.parser.parseKeys("k"))

    checkDelayedMapping(
      text,
      """
              -$c----
              12345
              abcde
              -----
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun `test execute mapping with a delay and second mapping`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map k j"))
    typeText(commandToKeys("map kk l"))
    typeText(injector.parser.parseKeys("k"))

    checkDelayedMapping(
      text,
      """
              -----
              12345
              a${c}bcde
              -----
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test execute mapping with a delay and second mapping and another starting mappings`() {
    // TODO: 24.01.2021  mapping time should be only 1000 sec
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map k j"))
    typeText(commandToKeys("map kk l"))
    typeText(commandToKeys("map j h"))
    typeText(commandToKeys("map jz w"))
    typeText(injector.parser.parseKeys("k"))

    checkDelayedMapping(
      text,
      """
              -----
              ${c}12345
              abcde
              -----
      """.trimIndent()
    )
  }

  fun `test execute mapping with a delay and second mapping and another starting mappings with another key`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map k j"))
    typeText(commandToKeys("map kk l"))
    typeText(commandToKeys("map j h"))
    typeText(commandToKeys("map jz w"))
    typeText(injector.parser.parseKeys("kz"))

    assertState(
      """
              -----
              12345
              ${c}abcde
              -----
      """.trimIndent()
    )
  }

  fun `test recursion`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map x y"))
    typeText(commandToKeys("map y x"))
    typeText(injector.parser.parseKeys("x"))

    TestCase.assertTrue(VimPlugin.isError())
  }

  fun `test map with expression`() {
    // we test that ternary expression works and cursor stays at the same place after leaving normal mode
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)
    typeText(commandToKeys("inoremap <expr> jk col(\".\") == 1? '<Esc>' : '<Esc><Right>'"))
    typeText(injector.parser.parseKeys("ijk"))
    assertState(text)
    val text2 = """
          -----
          ${c}12345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text2)
    typeText(injector.parser.parseKeys("ijk"))
    assertState(text2)
  }

  fun `test map with invalid expression`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)
    typeText(commandToKeys("nnoremap <expr> t ^f8a"))
    typeText(injector.parser.parseKeys("t"))
    assertPluginErrorMessageContains("E15: Invalid expression: ^f8a")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test map expr context`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    val editor = TextComponentEditorImpl(null, JTextArea())
    val context = DataContext.EMPTY_CONTEXT
    injector.vimscriptExecutor.execute(
      """
      let s:mapping = '^f8a'
      nnoremap <expr> t s:mapping
      """.trimIndent(),
      editor.vim, context.vim, skipHistory = false, indicateErrors = true, null
    )
    typeText(injector.parser.parseKeys("t"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E121: Undefined variable: s:mapping")
  }

  // todo keyPresses invoked inside a script should have access to the script context
//  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
//  fun `test map expr context`() {
//    configureByText("\n")
//    typeText(commandToKeys("""
//      let s:var = 'itext'|
//      nnoremap <expr> t s:var|
//      fun! T()|
//        normal t|
//      endfun|
//    """.trimIndent()))
//    assertState("\n")
//    typeText(commandToKeys("call T()"))
//    assertPluginError(false)
//    assertState("text\n")
//
//    typeText(parseKeys("t"))
//    assertPluginError(true)
//    assertPluginErrorMessageContains("E121: Undefined variable: s:var")
//  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test exception during expression evaluation in map with expression`() {
    val text = """
          -----
          ${c}12345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)
    typeText(commandToKeys("inoremap <expr> <cr> unknownFunction() ? '\\<C-y>' : '\\<C-g>u\\<CR>'"))
    typeText(injector.parser.parseKeys("i<CR>"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E117: Unknown function: unknownFunction")
    assertState(text)
  }

  fun `test rhc with triangle brackets`() {
    configureByText("\n")
    typeText(commandToKeys("inoremap p <p>"))
    typeText(injector.parser.parseKeys("ip"))
    assertState("<p>\n")
  }

  fun `test pattern in mapping`() {
    configureByText(
      """
      private fun myfun(funArg: String) {
        println(${c}funArg)
      }
      """.trimIndent()
    )
    typeText(commandToKeys("nnoremap ,f ?\\<fun\\><CR>"))
    typeText(injector.parser.parseKeys(",f"))
    assertState(
      """
      private ${c}fun myfun(funArg: String) {
        println(funArg)
      }
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun `ignoretest with shorter conflict`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map kkk l"))
    typeText(commandToKeys("map kk h"))
    typeText(injector.parser.parseKeys("kk"))

    checkDelayedMapping(
      text,
      """
              -----
              ${c}12345
              abcde
              -----
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.COMMAND)
    assertSubMode(VimStateMachine.SubMode.NONE)
  }

  private fun checkDelayedMapping(before: String, after: String) {
    assertState(before)

    waitAndAssert(5000) {
      return@waitAndAssert try {
        assertState(after)
        true
      } catch (e: AssertionError) {
        false
      }
    }
  }

  fun `test autocast to action notation`() {
    configureByText("\n")
    typeText(commandToKeys("nmap ,a :action Back<CR>"))
    typeText(commandToKeys("nmap ,b :action Back<Cr>"))
    typeText(commandToKeys("nmap ,c :action Back<cr>"))
    typeText(commandToKeys("nmap ,d :action Back<ENTER>"))
    typeText(commandToKeys("nmap ,e :action Back<Enter>"))
    typeText(commandToKeys("nmap ,f :action Back<enter>"))
    typeText(commandToKeys("nmap ,g :action Back<C-M>"))
    typeText(commandToKeys("nmap ,h :action Back<C-m>"))
    typeText(commandToKeys("nmap ,i :action Back<c-m>"))
    typeText(commandToKeys("nmap"))
    assertExOutput(
      """
n  ,a            <Action>(Back)
n  ,b            <Action>(Back)
n  ,c            <Action>(Back)
n  ,d            <Action>(Back)
n  ,e            <Action>(Back)
n  ,f            <Action>(Back)
n  ,g            <Action>(Back)
n  ,h            <Action>(Back)
n  ,i            <Action>(Back)

      """.trimIndent()
    )
  }

  fun `test autocast to action notation 2`() {
    configureByText("\n")
    typeText(commandToKeys("nnoremap ,a :action Back<CR>"))
    typeText(commandToKeys("nnoremap ,b :action Back<Cr>"))
    typeText(commandToKeys("nnoremap ,c :action Back<cr>"))
    typeText(commandToKeys("nnoremap ,d :action Back<ENTER>"))
    typeText(commandToKeys("nnoremap ,e :action Back<Enter>"))
    typeText(commandToKeys("nnoremap ,f :action Back<enter>"))
    typeText(commandToKeys("nnoremap ,g :action Back<C-M>"))
    typeText(commandToKeys("nnoremap ,h :action Back<C-m>"))
    typeText(commandToKeys("nnoremap ,i :action Back<c-m>"))
    typeText(commandToKeys("nnoremap"))
    assertExOutput(
      """
n  ,a            <Action>(Back)
n  ,b            <Action>(Back)
n  ,c            <Action>(Back)
n  ,d            <Action>(Back)
n  ,e            <Action>(Back)
n  ,f            <Action>(Back)
n  ,g            <Action>(Back)
n  ,h            <Action>(Back)
n  ,i            <Action>(Back)

      """.trimIndent()
    )
  }

  fun `test command from map isn't added to history`() {
    configureByText("\n")
    typeText(commandToKeys("map A :echo 42<CR>"))
    typeText(injector.parser.parseKeys("A"))
    assertExOutput("42\n")
    assertEquals("map A :echo 42<CR>", injector.historyGroup.getEntries(HistoryConstants.COMMAND, 0, 0).last().entry)
  }
}
