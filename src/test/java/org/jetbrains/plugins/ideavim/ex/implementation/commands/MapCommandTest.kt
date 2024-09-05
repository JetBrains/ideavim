/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.idea.TestFor
import com.intellij.testFramework.LoggedErrorProcessor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.keys
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.history.HistoryConstants
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.ExceptionHandler
import org.jetbrains.plugins.ideavim.OnlyThrowLoggedErrorProcessor
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCaseBase
import org.jetbrains.plugins.ideavim.exceptionMappingOwner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @author vlan
 */
class MapCommandTest : VimTestCaseBase() {

  @AfterEach
  fun tearDown() {
    injector.keyGroup.removeKeyMapping(exceptionMappingOwner)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testMapKtoJ() {
    configureByText(
      """
  ${c}foo
  bar
  
      """.trimIndent(),
    )
    typeText(commandToKeys("nmap k j"))
    assertPluginError(false)
    assertOffset(0)
    typeText(injector.parser.parseKeys("k"))
    assertOffset(4)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testInsertMapJKtoEsc() {
    configureByText("${c}World!\n")
    typeText(commandToKeys("imap jk <Esc>"))
    assertPluginError(false)
    typeText(injector.parser.parseKeys("i" + "Hello, " + "jk"))
    assertState("Hello, World!\n")
    assertMode(Mode.NORMAL())
    assertOffset(6)
  }

  @Test
  fun testBackslashAtEnd() {
    configureByText("\n")
    typeText(commandToKeys("imap foo\\ bar"))
    assertPluginError(false)
    typeText(injector.parser.stringToKeys("ifoo\\"))
    assertState("bar\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replace term codes")
  @Test
  fun testUnfinishedSpecialKey() {
    configureByText("\n")
    typeText(commandToKeys("imap <Esc foo"))
    typeText(injector.parser.stringToKeys("i<Esc"))
    assertState("foo\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testUnknownSpecialKey() {
    configureByText("\n")
    typeText(commandToKeys("imap <foo> bar"))
    typeText(injector.parser.stringToKeys("i<foo>"))
    assertState("bar\n")
  }

  @Test
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
        |i  <C-Down>      <C-O>gt
        |i  bar           <Esc>
        |i  foo           bar
      """.trimMargin(),
    )
    typeText(commandToKeys("map"))
    assertExOutput(
      """
        |   <C-Down>      gt
        |n  <Plug>Foo     iHello<Esc>
        |n  ,f            <Plug>Foo
        """.trimMargin(),
    )
  }

  @Test
  fun testRecursiveMapping() {
    configureByText("\n")
    typeText(commandToKeys("imap foo bar"))
    typeText(commandToKeys("imap bar baz"))
    typeText(commandToKeys("imap baz quux"))
    typeText(injector.parser.parseKeys("i" + "foo"))
    assertState("quux\n")
  }

  @Test
  fun testddWithMapping() {
    configureByText(
      """
      Hello$c 1
      Hello 2
      """.trimIndent(),
    )
    typeText(commandToKeys("nmap dc k"))
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      Hello 2
      """.trimIndent(),
    )
  }

  @Test
  fun testNonRecursiveMapping() {
    configureByText("\n")
    typeText(commandToKeys("inoremap a b"))
    assertPluginError(false)
    typeText(commandToKeys("inoremap b a"))
    typeText(injector.parser.parseKeys("i" + "ab"))
    assertState("ba\n")
  }

  @Test
  fun testNonRecursiveMapTable() {
    configureByText("\n")
    typeText(commandToKeys("inoremap jj <Esc>"))
    typeText(commandToKeys("imap foo bar"))
    typeText(commandToKeys("imap"))
    assertExOutput(
      """
        |i  foo           bar
        |i  jj          * <Esc>
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testNop() {
    configureByText(
      """
  ${c}foo
  bar
  
      """.trimIndent(),
    )
    typeText(commandToKeys("noremap <Right> <nop>"))
    assertPluginError(false)
    typeText(injector.parser.parseKeys("l" + "<Right>"))
    assertPluginError(false)
    assertState(
      """
  foo
  bar
  
      """.trimIndent(),
    )
    assertOffset(1)
    typeText(commandToKeys("nmap"))
    assertExOutput("n  <Right>     * <Nop>")
  }

  @Test
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
        |n  ,a            /a<CR>
        |n  ,b            /b<CR>
        |n  ,c            /c<CR>
        |n  ,d            /d<CR>
        |n  ,f            '/f<CR>'
        |n  ,g            /g<CR>
      """.trimMargin(),
    )
  }

  // VIM-645 |:nmap|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testMapSpace() {
    configureByText("foo\n")
    typeText(commandToKeys("nmap <space> dw"))
    typeText(injector.parser.parseKeys(" "))
    assertState("\n")
    typeText(injector.parser.parseKeys("i" + " " + "<Esc>"))
    assertState(" \n")
  }

  // VIM-661 |:noremap| |r|
  @Test
  fun testNoMappingInReplaceCharacterArgument() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("noremap A Z"))
    typeText(injector.parser.parseKeys("rA"))
    assertState("Aoo\n")
  }

  // VIM-661 |:omap| |d| |t|
  @Test
  fun testNoMappingInNonFirstCharOfOperatorPendingMode() {
    configureByText("${c}foo, bar\n")
    typeText(commandToKeys("omap , ?"))
    typeText(injector.parser.parseKeys("dt,"))
    assertState(", bar\n")
  }

  // VIM-666 |:imap|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testIgnoreEverythingAfterBar() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("imap a b |c \" Something else"))
    typeText(injector.parser.parseKeys("ia"))
    assertState("b foo\n")
  }

  // VIM-666 |:imap|
  @Test
  fun testBarEscaped() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("imap a b \\| c"))
    typeText(injector.parser.parseKeys("ia"))
    assertState("b | cfoo\n")
  }

  // VIM-666 |:imap|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testBarEscapedSeveralSpaces() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("imap a b \\| c    |"))
    typeText(injector.parser.parseKeys("ia"))
    assertState("b | c    foo\n")
  }

  // VIM-670 |:map|
  @Test
  fun testFirstCharIsNonRecursive() {
    configureByText("\n")
    typeText(commandToKeys("map ab abcd"))
    typeText(injector.parser.parseKeys("ab"))
    assertState("bcd\n")
  }

  @Test
  @TestFor(issues = ["VIM-3507"])
  fun `test bar in mapping in search`() {
    configureByText("${c}I found it in a legendary land")
    typeText(commandToKeys(":map t /4\\\\|a<CR>"))
    typeText("t")
    assertState("I found it in ${c}a legendary land")
  }

  @Test
  @TestFor(issues = ["VIM-3569"])
  fun `test bar in mapping`() {
    configureByText("${c}I found it in a legendary land")
    typeText(commandToKeys("nmap <leader>\\| dw"))
    typeText("<leader>|")
    assertState("${c}found it in a legendary land")
  }

  // VIM-676 |:map|
  @TestWithoutNeovim(reason = SkipNeovimReason.VIM_SCRIPT)
  @Test
  fun testBackspaceCharacterInVimRc() {
    configureByText("\n")
    executeVimscript("inoremap # X\u0008#\n")
    typeText(injector.parser.parseKeys("i" + "#" + "<Esc>"))
    assertState("#\n")
    assertMode(Mode.NORMAL())
    typeText(commandToKeys("imap"))
    assertExOutput("i  #           * X<C-H>#")
  }

  // VIM-679 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testCancelCharacterInVimRc() {
    configureByText(
      """
  ${c}foo
  bar
  
      """.trimIndent(),
    )
    executeVimscript("map \u0018i dd\n", true)
    typeText(injector.parser.parseKeys("i" + "#" + "<Esc>"))
    assertState(
      """
  #foo
  bar
  
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    typeText(commandToKeys("map"))
    assertExOutput("   <C-X>i        dd")
    typeText(injector.parser.parseKeys("<C-X>i"))
    assertState("bar\n")
  }

  // VIM-679 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testBarCtrlVEscaped() {
    configureByText("${c}foo\n")
    executeVimscript("imap a b \u0016|\u0016| c |\n")
    typeText(injector.parser.parseKeys("ia"))
    assertState("b || c foo\n")
  }

  // VIM-679 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad term codes")
  @Test
  fun testCtrlMCtrlLAsNewLine() {
    configureByText("${c}foo\n")
    executeVimscript("map A :%s/foo/bar/g\r\u000C\n")
    typeText(injector.parser.parseKeys("A"))
    assertState("bar\n")
  }

  // VIM-700 |:map|
  @Test
  fun testRemappingZero() {
    configureByText("x${c}yz\n")
    typeText(commandToKeys("map 0 ~"))
    typeText(injector.parser.parseKeys("0"))
    assertState("xYz\n")
  }

  // VIM-700 |:map|
  @TestWithoutNeovim(reason = SkipNeovimReason.VIM_SCRIPT)
  @Test
  fun testRemappingZeroStillAllowsZeroToBeUsedInCount() {
    configureByText("a${c}bcdefghijklmnop\n")
    executeVimscript("map 0 ^")
    typeText(injector.parser.parseKeys("10~"))
    assertState("aBCDEFGHIJKlmnop\n")
  }

  // VIM-700 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad term codes")
  @Test
  fun testRemappingDeleteOverridesRemovingLastDigitFromCount() {
    configureByText("a${c}bcdefghijklmnop\n")
    typeText(commandToKeys("map <Del> ~"))
    typeText(injector.parser.parseKeys("10<Del>"))
    assertState("aBCDEFGHIJKlmnop\n")
  }

  // VIM-650 |mapleader|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testMapLeader() {
    configureByText("\n")
    typeText(commandToKeys("let mapleader = \",\""))
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"))
    typeText(injector.parser.parseKeys(",z"))
    assertState("zzz\n")
  }

  // VIM-650 |mapleader|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testMapLeaderToSpace() {
    configureByText("\n")
    typeText(commandToKeys("let mapleader = \"\\<SPACE>\""))
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"))
    typeText(injector.parser.parseKeys(" z"))
    assertState("zzz\n")
  }

  // VIM-650 |mapleader|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testMapLeaderToSpaceWithWhitespace() {
    configureByText("\n")
    typeText(commandToKeys("let mapleader = \" \""))
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"))
    typeText(injector.parser.parseKeys(" z"))
    assertState("zzz\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replace term codes")
  @Test
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
  @Test
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
  @Test
  fun testPlugMapping() {
    configureByText("\n")
    typeText(commandToKeys("nmap ,f <Plug>Foo"))
    typeText(commandToKeys("nmap <Plug>Foo iHello<Esc>"))
    typeText(injector.parser.parseKeys(",fa!<Esc>"))
    assertState("Hello!\n")
  }

  @Test
  fun testIntersectingCommands() {
    configureByText("123${c}4567890")
    typeText(commandToKeys("map ds h"))
    typeText(commandToKeys("map I 3l"))
    typeText(injector.parser.parseKeys("dI"))
    assertState("123${c}7890")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUG)
  @Test
  fun testIncompleteMapping() {
    configureByText("123${c}4567890")
    typeText(commandToKeys("map <Plug>(Hi)l lll"))
    typeText(commandToKeys("map I <Plug>(Hi)"))
    typeText(injector.parser.parseKeys("Ih"))
    assertState("12${c}34567890")
  }

  @Test
  fun testIntersectingCommands2() {
    configureByText("123${c}4567890")
    typeText(commandToKeys("map as x"))
    typeText(injector.parser.parseKeys("gas"))
    assertState("123${c}567890")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testMapZero() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 0 w"))
    typeText(injector.parser.parseKeys("0"))
    assertOffset(14)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testMapZeroIgnoredInCount() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 0 w"))
    typeText(injector.parser.parseKeys("10w"))
    assertOffset(51)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testMapNonZeroDigit() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 2 w"))
    typeText(injector.parser.parseKeys("2"))
    assertOffset(14)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testMapNonZeroDigitNotIncludedInCount() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 2 w"))
    typeText(injector.parser.parseKeys("92"))
    assertOffset(45)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testShiftSpace() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-Space> w"))
    typeText(injector.parser.parseKeys("<S-Space>"))
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testShiftSpaceAndWorkInInsertMode() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-Space> w"))
    typeText(injector.parser.parseKeys("i<S-Space>"))
    assertState("A quick  ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testShiftLetter() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-D> w"))
    typeText(injector.parser.parseKeys("<S-D>"))
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @Test
  fun testUppercaseLetter() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap D w"))
    typeText(injector.parser.parseKeys("D"))
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun `test shift letter doesn't break insert mode`() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-D> w"))
    typeText(injector.parser.parseKeys("<S-D>"))
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")

    typeText(injector.parser.parseKeys("iD<Esc>"))
    assertState("A quick brown ${c}Dfox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  // todo keyPresses invoked inside a script should have access to the script context
  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  @Disabled
  fun `test map expr context 2`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      let s:var = 'itext'|
      nnoremap <expr> t s:var|
      fun! T()|
        normal t|
      endfun|
        """.trimIndent(),
      ),
    )
    assertState("\n")
    typeText(commandToKeys("call T()"))
    assertPluginError(false)
    assertState("text\n")

    typeText(injector.parser.parseKeys("t"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E121: Undefined variable: s:var")
  }


  @Test
  fun `test rhc with triangle brackets`() {
    configureByText("\n")
    typeText(commandToKeys("inoremap p <p>"))
    typeText(injector.parser.parseKeys("ip"))
    assertState("<p>\n")
  }

  @Test
  fun `test pattern in mapping`() {
    configureByText(
      """
      private fun myfun(funArg: String) {
        println(${c}funArg)
      }
      """.trimIndent(),
    )
    typeText(commandToKeys("nnoremap ,f ?\\<fun\\><CR>"))
    typeText(injector.parser.parseKeys(",f"))
    assertState(
      """
      private ${c}fun myfun(funArg: String) {
        println(funArg)
      }
      """.trimIndent(),
    )
  }

  @Test
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
        |n  ,a            <Action>(Back)
        |n  ,b            <Action>(Back)
        |n  ,c            <Action>(Back)
        |n  ,d            <Action>(Back)
        |n  ,e            <Action>(Back)
        |n  ,f            <Action>(Back)
        |n  ,g            <Action>(Back)
        |n  ,h            <Action>(Back)
        |n  ,i            <Action>(Back)
      """.trimMargin(),
    )
  }

  @Test
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
        |n  ,a            <Action>(Back)
        |n  ,b            <Action>(Back)
        |n  ,c            <Action>(Back)
        |n  ,d            <Action>(Back)
        |n  ,e            <Action>(Back)
        |n  ,f            <Action>(Back)
        |n  ,g            <Action>(Back)
        |n  ,h            <Action>(Back)
        |n  ,i            <Action>(Back)
      """.trimMargin(),
    )
  }

  @Test
  fun `test command from map isn't added to history`() {
    configureByText("\n")
    typeText(commandToKeys("map A :echo 42<CR>"))
    typeText(injector.parser.parseKeys("A"))
    assertExOutput("42")
    kotlin.test.assertEquals(
      "map A :echo 42<CR>",
      injector.historyGroup.getEntries(HistoryConstants.COMMAND, 0, 0).last().entry,
    )
  }

  @TestFor(issues = ["VIM-3103"])
  @TestWithoutNeovim(reason = SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test map enter to action`() {
    configureByText(
      """
     Lorem Ipsum

     Lorem ipsum dolor sit amet,
     ${c}consectetur adipiscing elit
     Sed in orci mauris.
     Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()
    )
    typeText(commandToKeys("map <Enter> <Action>(EditorSelectWord)"))
    typeText("<Enter>")
    assertState("""
     Lorem Ipsum

     Lorem ipsum dolor sit amet,
     ${s}${c}consectetur${se} adipiscing elit
     Sed in orci mauris.
     Cras id tellus in ex imperdiet egestas. 
    """.trimIndent())
  }

  @TestFor(issues = ["VIM-2929"])
  @TestWithoutNeovim(reason = SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `mapping to handler with exception`() {
    configureByText(
      """
     Lorem Ipsum

     Lorem ipsum dolor sit amet,
     ${c}consectetur adipiscing elit
     Sed in orci mauris.
     Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()
    )
    injector.keyGroup.putKeyMapping(MappingMode.NXO, keys("abc"), exceptionMappingOwner, ExceptionHandler(), false)

    typeText(commandToKeys("map k abcx"))

    val exception = assertThrows<Throwable> {
      LoggedErrorProcessor.executeWith<Throwable>(OnlyThrowLoggedErrorProcessor) {
        typeText("k")
      }
    }
    assertEquals(ExceptionHandler.exceptionMessage, exception.cause!!.cause!!.message)

    assertTrue(KeyHandler.getInstance().keyStack.isEmpty())
  }

  @TestFor(issues = ["VIM-3601"])
  @Test
  fun `mapping to something with bars`() {
    configureByText(
      """
     Lorem Ipsum

     Lorem ipsum dolor sit amet,
     ${c}consectetur adipiscing elit
     Sed in orci mauris.
     Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()
    )
    typeText(commandToKeys("map k :echo 4<CR> \\| :echo 42<CR>"))
    assertNull(injector.outputPanel.getCurrentOutputPanel())
    typeText("k")
    assertEquals("4\n42", injector.outputPanel.getCurrentOutputPanel()!!.text)
  }
}
