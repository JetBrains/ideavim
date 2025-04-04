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
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.keys
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.ExceptionHandler
import org.jetbrains.plugins.ideavim.OnlyThrowLoggedErrorProcessor
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.exceptionMappingOwner
import org.jetbrains.plugins.ideavim.waitAndAssert
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
@Suppress("SpellCheckingInspection")
class MapCommandTest : VimTestCase() {

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
    enterCommand("nmap k j")
    assertPluginError(false)
    assertOffset(0)
    typeText("k")
    assertOffset(4)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testInsertMapJKtoEsc() {
    configureByText("${c}World!\n")
    enterCommand("imap jk <Esc>")
    assertPluginError(false)
    typeText("i" + "Hello, " + "jk")
    assertState("Hello, World!\n")
    assertMode(Mode.NORMAL())
    assertOffset(6)
  }

  @Test
  fun testBackslashAtEnd() {
    configureByText("\n")
    enterCommand("imap foo\\ bar")
    assertPluginError(false)
    typeText("ifoo\\")
    assertState("bar\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replace term codes")
  @Test
  fun testUnfinishedSpecialKey() {
    configureByText("\n")
    enterCommand("imap <Esc foo")
    typeText("i<Esc")
    assertState("foo\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testUnknownSpecialKey() {
    configureByText("\n")
    enterCommand("imap <foo> bar")
    typeText("i<foo>")
    assertState("bar\n")
  }

  @Test
  fun testMapTable() {
    configureByText("\n")
    enterCommand("map <C-Down> gt")
    enterCommand("imap foo bar")
    enterCommand("imap bar <Esc>")
    enterCommand("imap <C-Down> <C-O>gt")
    enterCommand("nmap ,f <Plug>Foo")
    enterCommand("nmap <Plug>Foo iHello<Esc>")

    assertCommandOutput("imap",
      """
        |i  <C-Down>      <C-O>gt
        |i  bar           <Esc>
        |i  foo           bar
      """.trimMargin(),
    )

    assertCommandOutput("map",
      """
        |   <C-Down>      gt
        |n  <Plug>Foo     iHello<Esc>
        |n  ,f            <Plug>Foo
        """.trimMargin(),
    )
  }

  private fun addTestMaps() {
    // TODO: Support lmap
    enterCommand("map all foo") // NVO
    enterCommand("nmap normal foo")
    enterCommand("imap insert foo")
    enterCommand("vmap visual+select foo")  // V -> Visual+Select
    enterCommand("smap select foo")
    enterCommand("xmap visual foo")
    enterCommand("omap op-pending foo")
    enterCommand("map! insert+cmdline foo") // IC
    enterCommand("cmap cmdline foo")
//    enterCommand("lmap lang foo")   // TODO: Support lmap
  }

  @Test
  fun `test output of map shows maps for NVO modes`() {
    configureByText("\n")
    addTestMaps()

    // Note that Vim doesn't appear to have an order. Items are kinda sorted, but also not. I.e. `m{something}` are
    // grouped together, but followed later by `g{something}`. We'll sort by {lhs}, so we're at least consistent
    assertCommandOutput("map",
      """
        |   all           foo
        |n  normal        foo
        |o  op-pending    foo
        |s  select        foo
        |x  visual        foo
        |v  visual+select   foo
      """.trimMargin()
    )
  }

  @Test
  fun `test output of nmap shows maps for Normal mode`() {
    configureByText("\n")
    addTestMaps()

    assertCommandOutput("nmap",
      """
        |   all           foo
        |n  normal        foo
      """.trimMargin()
    )
  }

  @Test
  fun `test output of vmap shows maps for Visual and Select modes`() {
    configureByText("\n")
    addTestMaps()

    assertCommandOutput("vmap",
      """
        |   all           foo
        |s  select        foo
        |x  visual        foo
        |v  visual+select   foo
      """.trimMargin()
    )
  }

  @Test
  fun `test output of smap shows maps for Select mode`() {
    configureByText("\n")
    addTestMaps()

    assertCommandOutput("smap",
      """
        |   all           foo
        |s  select        foo
        |v  visual+select   foo
      """.trimMargin()
    )
  }

  @Test
  fun `test output of xmap shows maps for Visual mode`() {
    configureByText("\n")
    addTestMaps()

    assertCommandOutput("xmap",
      """
        |   all           foo
        |x  visual        foo
        |v  visual+select   foo
      """.trimMargin()
    )
  }

  @Test
  fun `test output of omap shows maps for Op-pending mode`() {
    configureByText("\n")
    addTestMaps()

    assertCommandOutput("omap",
      """
        |   all           foo
        |o  op-pending    foo
      """.trimMargin()
    )
  }

  @Test
  fun `test output of map! shows maps for Insert and Cmdline modes`() {
    configureByText("\n")
    addTestMaps()

    assertCommandOutput("map!",
      """
        |c  cmdline       foo
        |i  insert        foo
        |!  insert+cmdline   foo
      """.trimMargin()
    )
  }

  @Test
  fun `test bang modifier reports error except for map!`() {
    configureByText("\n")
    enterCommand("vmap!")

    assertPluginError(true)
    assertPluginErrorMessageContains("E477: No ! allowed")
  }

  @Test
  fun `test output of imap shows maps for Insert mode`() {
    configureByText("\n")
    addTestMaps()

    assertCommandOutput("imap",
      """
        |i  insert        foo
        |!  insert+cmdline   foo
      """.trimMargin()
    )
  }

  @Test
  @Disabled("lmap not yet supported")
  fun `test output of lmap shows maps for Language specific modes`() {
    configureByText("\n")
    addTestMaps()

    assertCommandOutput("lmap",
      """
        |l  lang          foo
      """.trimMargin()
    )
  }

  @Test
  fun `test output of cmap shows maps for Command-line mode`() {
    configureByText("\n")
    addTestMaps()

    assertCommandOutput("cmap",
      """
        |c  cmdline       foo
        |!  insert+cmdline   foo
      """.trimMargin()
    )
  }

  @Test
  fun `test ouptut of map shows correct modes after unmapping a single mode`() {
    configureByText("\n")
    addTestMaps() // Adds a mapping of all for NVO

    enterCommand("sunmap all")  // Removes Select from the NVO mapping for foo

    // Note that the formatting is exactly how Vim shows it. Messy, isn't it?
    assertCommandOutput("map",
      """
        |noxall           foo
        |n  normal        foo
        |o  op-pending    foo
        |s  select        foo
        |x  visual        foo
        |v  visual+select   foo
      """.trimMargin()
    )
  }

  @Test
  fun `test output of map shows correct modes after unmapping multiple modes`() {
    configureByText("\n")
    addTestMaps() // Adds a mapping of all for NVO

    enterCommand("vunmap all")  // Removes Visual+Select from the NVO mapping for foo

    assertCommandOutput("map",
      """
        |no all           foo
        |n  normal        foo
        |o  op-pending    foo
        |s  select        foo
        |x  visual        foo
        |v  visual+select   foo
      """.trimMargin()
    )
  }

  @Test
  fun `test output of map shows correct modes after unmapping from vmap and map`() {
    configureByText("\n")
    enterCommand("map foo bar") // Normal, Visual, Select, Op-pending
    enterCommand("vmap foo baz")  // Visual, Select

    // Just to be sure we're set up correctly
    assertCommandOutput("map",
      """
        |no foo           bar
        |v  foo           baz
      """.trimMargin()
    )

    enterCommand("sunmap foo")
    enterCommand("ounmap foo")

    assertCommandOutput("map",
      """
        |n  foo           bar
        |x  foo           baz
      """.trimMargin()
    )
  }

  @Test
  fun `test output of map shows maps with matching prefixes`() {
    configureByText("\n")
    enterCommand("map foo bar")
    enterCommand("imap faa baz")
    enterCommand("nmap fee bap")
    enterCommand("nmap zzz ppp")

    assertCommandOutput("map f",
      """
        |n  fee           bap
        |   foo           bar
      """.trimMargin()
    )
  }

  @Test
  fun `test map with only trailing spaces treated as no arguments`() {
    configureByText("\n")
    enterCommand("map foo bar")

    assertCommandOutput("map     ",
      """
        |   foo           bar
      """.trimMargin()
    )
  }

  @Test
  fun `test map with prefix ignores trailing spaces`() {
    configureByText("\n")
    enterCommand("imap foo bar")

    assertCommandOutput("imap f    ",
      """
        |i  foo           bar
      """.trimMargin()
    )
  }

  @Test
  fun testRecursiveMapping() {
    configureByText("\n")
    enterCommand("imap foo bar")
    enterCommand("imap bar baz")
    enterCommand("imap baz quux")
    typeText("i" + "foo")
    assertState("quux\n")
  }

  @Test
  fun `test dd with mapping starting with d`() {
    configureByText(
      """
      Hello$c 1
      Hello 2
      """.trimIndent(),
    )
    enterCommand("nmap dc k")
    typeText("dd")
    assertState(
      """
      Hello 2
      """.trimIndent(),
    )
  }

  @Test
  fun testNonRecursiveMapping() {
    configureByText("\n")
    enterCommand("inoremap a b")
    assertPluginError(false)
    enterCommand("inoremap b a")
    typeText("i" + "ab")
    assertState("ba\n")
  }

  @Test
  fun testNonRecursiveMapTable() {
    configureByText("\n")
    enterCommand("inoremap jj <Esc>")
    enterCommand("imap foo bar")

    assertCommandOutput("imap",
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
    enterCommand("nnoremap <Right> <nop>")
    assertPluginError(false)
    typeText("l" + "<Right>")
    assertPluginError(false)
    assertState(
      """
  foo
  bar
  
      """.trimIndent(),
    )
    assertOffset(1)
    assertCommandOutput("nmap", "n  <Right>     * <Nop>")
  }

  @Test
  fun testIgnoreModifiers() {
    configureByText("\n")
    enterCommand("nmap <buffer> ,a /a<CR>")
    enterCommand("nmap <nowait> ,b /b<CR>")
    enterCommand("nmap <silent> ,c /c<CR>")
    enterCommand("nmap <special> ,d /d<CR>")
    enterCommand("nmap <script> ,e /e<CR>")
    enterCommand("nmap <expr> ,f '/f<CR>'")
    enterCommand("nmap <unique> ,g /g<CR>")

    assertCommandOutput("nmap",
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
    enterCommand("nmap <space> dw")
    typeText(" ")
    assertState("\n")
    typeText("i" + " " + "<Esc>")
    assertState(" \n")
  }

  // VIM-661 |:noremap| |r|
  @Test
  fun testNoMappingInReplaceCharacterArgument() {
    configureByText("${c}foo\n")
    enterCommand("noremap A Z")
    typeText("rA")
    assertState("Aoo\n")
  }

  // VIM-661 |:omap| |d| |t|
  @Test
  fun testNoMappingInNonFirstCharOfOperatorPendingMode() {
    configureByText("${c}foo, bar\n")
    enterCommand("omap , ?")
    typeText("dt,")
    assertState(", bar\n")
  }

  // VIM-666 |:imap|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testIgnoreEverythingAfterBar() {
    configureByText("${c}foo\n")
    enterCommand("imap a b |c \" Something else")
    typeText("ia")
    assertState("b foo\n")
  }

  // VIM-666 |:imap|
  @Test
  fun testBarEscaped() {
    configureByText("${c}foo\n")
    enterCommand("imap a b \\| c")
    typeText("ia")
    assertState("b | cfoo\n")
  }

  // VIM-666 |:imap|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testBarEscapedSeveralSpaces() {
    configureByText("${c}foo\n")
    enterCommand("imap a b \\| c    |")
    typeText("ia")
    assertState("b | c    foo\n")
  }

  // VIM-670 |:map|
  @Test
  fun testFirstCharIsNonRecursive() {
    configureByText("\n")
    enterCommand("map ab abcd")
    typeText("ab")
    assertState("bcd\n")
  }

  @Test
  @TestFor(issues = ["VIM-3507"])
  fun `test bar in mapping in search`() {
    configureByText("${c}I found it in a legendary land")
    enterCommand(":map t /4\\\\|a<CR>")
    typeText("t")
    assertState("I found it in ${c}a legendary land")
  }

  @Test
  @TestFor(issues = ["VIM-3569"])
  fun `test bar in mapping`() {
    configureByText("${c}I found it in a legendary land")
    enterCommand("nmap <leader>\\| dw")
    typeText("<leader>|")
    assertState("${c}found it in a legendary land")
  }

  // VIM-676 |:map|
  @TestWithoutNeovim(reason = SkipNeovimReason.VIM_SCRIPT)
  @Test
  fun testBackspaceCharacterInVimRc() {
    configureByText("\n")
    executeVimscript("inoremap # X\u0008#\n")
    typeText("i" + "#" + "<Esc>")
    assertState("#\n")
    assertMode(Mode.NORMAL())

    assertCommandOutput("imap", "i  #           * X<C-H>#")
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
    typeText("i" + "#" + "<Esc>")
    assertState(
      """
  #foo
  bar
  
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    assertCommandOutput("map", "   <C-X>i        dd")
    typeText("<C-X>i")
    assertState("bar\n")
  }

  // VIM-679 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testBarCtrlVEscaped() {
    configureByText("${c}foo\n")
    executeVimscript("imap a b \u0016|\u0016| c |\n")
    typeText("ia")
    assertState("b || c foo\n")
  }

  // VIM-679 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad term codes")
  @Test
  fun testCtrlMCtrlLAsNewLine() {
    configureByText("${c}foo\n")
    executeVimscript("map A :%s/foo/bar/g\r\u000C\n")
    typeText("A")
    assertState("bar\n")
  }

  // VIM-700 |:map|
  @Test
  fun testRemappingZero() {
    configureByText("x${c}yz\n")
    enterCommand("map 0 ~")
    typeText("0")
    assertState("xYz\n")
  }

  // VIM-700 |:map|
  @TestWithoutNeovim(reason = SkipNeovimReason.VIM_SCRIPT)
  @Test
  fun testRemappingZeroStillAllowsZeroToBeUsedInCount() {
    configureByText("a${c}bcdefghijklmnop\n")
    executeVimscript("map 0 ^")
    typeText("10~")
    assertState("aBCDEFGHIJKlmnop\n")
  }

  // VIM-700 |:map|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad term codes")
  @Test
  fun testRemappingDeleteOverridesRemovingLastDigitFromCount() {
    configureByText("a${c}bcdefghijklmnop\n")
    enterCommand("map <Del> ~")
    typeText("10<Del>")
    assertState("aBCDEFGHIJKlmnop\n")
  }

  // VIM-650 |mapleader|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testMapLeader() {
    configureByText("\n")
    enterCommand("let mapleader = \",\"")
    enterCommand("nmap <Leader>z izzz<Esc>")
    typeText(",z")
    assertState("zzz\n")
  }

  // VIM-650 |mapleader|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testMapLeaderToSpace() {
    configureByText("\n")
    enterCommand("let mapleader = \"\\<SPACE>\"")
    enterCommand("nmap <Leader>z izzz<Esc>")
    typeText(" z")
    assertState("zzz\n")
  }

  // VIM-650 |mapleader|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testMapLeaderToSpaceWithWhitespace() {
    configureByText("\n")
    enterCommand("let mapleader = \" \"")
    enterCommand("nmap <Leader>z izzz<Esc>")
    typeText(" z")
    assertState("zzz\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replace term codes")
  @Test
  fun testAmbiguousMapping() {
    configureByText("\n")
    enterCommand("nmap ,f iHello<Esc>")
    enterCommand("nmap ,fc iBye<Esc>")
    typeText(",fdh")
    assertState("Helo\n")
    typeText("diw")
    assertState("\n")
    typeText(",fch")
    assertState("Bye\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad term codes")
  @Test
  fun testLongAmbiguousMapping() {
    configureByText("\n")
    enterCommand("nmap ,foo iHello<Esc>")
    enterCommand("nmap ,fooc iBye<Esc>")
    typeText(",foodh")
    assertState("Helo\n")
    typeText("diw")
    assertState("\n")
    typeText(",fooch")
    assertState("Bye\n")
  }

  @Test
  fun `test map applies longest mapping`() {
    configureByText("\n")
    enterCommand("imap ab AB")
    enterCommand("imap abcd ABCD")
    typeText("i", "abcd", "<Esc>")
    assertState("ABCD\n")
  }

  @Test
  fun `test map falls back to previous longest mapping when abandoned`() {
    configureByText("\n")
    enterCommand("imap abc ABC")
    enterCommand("imap abcd ABCD")
    typeText("i", "abcg", "<Esc>")
    assertState("ABCg\n")
  }

  @Test
  fun `test map falls back to previous longest mapping when abandoned with shorter prefix`() {
    configureByText("\n")
    enterCommand("imap ab AB")
    enterCommand("imap abcd ABCD")
    typeText("i", "abcg", "<Esc>")
    assertState("ABcg\n")
  }

  @Test
  fun `test map falls back to previous longest mapping after timeout`() {
    configureByText("\n")
    enterCommand("imap ab AB")
    enterCommand("imap abcd ABCD")
    enterCommand("set timeoutlen=100")
    typeText("i", "abc")
    waitAndAssert(injector.globalOptions().timeoutlen + 100) {
      fixture.editor.document.text == "ABc\n"
    }
    assertState("ABc\n")
  }

  @Test
  fun `test map falls back to previous longest mapping after timeout with shorter prefix`() {
    configureByText("\n")
    enterCommand("imap ab AB")
    enterCommand("imap abcde ABCDE")
    enterCommand("set timeoutlen=100")
    typeText("i", "abcd")
    waitAndAssert(injector.globalOptions().timeoutlen + 100) {
      fixture.editor.document.text == "ABcd\n"
    }
    assertState("ABcd\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUG)
  @Test
  fun testPlugMapping() {
    configureByText("\n")
    enterCommand("nmap ,f <Plug>Foo")
    enterCommand("nmap <Plug>Foo iHello<Esc>")
    typeText(",fa!<Esc>")
    assertState("Hello!\n")
  }

  @Test
  fun testIntersectingCommands() {
    configureByText("123${c}4567890")
    enterCommand("map ds h")
    enterCommand("map I 3l")
    typeText("dI")
    assertState("123${c}7890")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUG)
  @Test
  fun testIncompleteMapping() {
    configureByText("123${c}4567890")
    enterCommand("map <Plug>(Hi)l lll")
    enterCommand("map I <Plug>(Hi)")
    typeText("Ih")
    assertState("12${c}34567890")
  }

  @Test
  fun testIntersectingCommands2() {
    configureByText("123${c}4567890")
    enterCommand("map as x")
    typeText("gas")
    assertState("123${c}567890")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testMapZero() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog")
    enterCommand("nmap 0 w")
    typeText("0")
    assertOffset(14)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testMapZeroIgnoredInCount() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    enterCommand("nmap 0 w")
    typeText("10w")
    assertOffset(51)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testMapNonZeroDigit() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog")
    enterCommand("nmap 2 w")
    typeText("2")
    assertOffset(14)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testMapNonZeroDigitNotIncludedInCount() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    enterCommand("nmap 2 w")
    typeText("92")
    assertOffset(45)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testShiftSpace() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    enterCommand("nmap <S-Space> w")
    typeText("<S-Space>")
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testShiftSpaceAndWorkInInsertMode() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    enterCommand("nmap <S-Space> w")
    typeText("i<S-Space>")
    assertState("A quick  ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun testShiftLetter() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    enterCommand("nmap <S-D> w")
    typeText("<S-D>")
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @Test
  fun testUppercaseLetter() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    enterCommand("nmap D w")
    typeText("D")
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "Bad replace of term codes")
  @Test
  fun `test shift letter doesn't break insert mode`() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    enterCommand("nmap <S-D> w")
    typeText("<S-D>")
    assertState("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")

    typeText("iD<Esc>")
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
    enterCommand("call T()")
    assertPluginError(false)
    assertState("text\n")

    typeText("t")
    assertPluginError(true)
    assertPluginErrorMessageContains("E121: Undefined variable: s:var")
  }


  @Test
  fun `test rhs with triangle brackets`() {
    configureByText("\n")
    enterCommand("inoremap p <p>")
    typeText("ip")
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
    enterCommand("nnoremap ,f ?\\<fun\\><CR>")
    typeText(",f")
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
    enterCommand("nmap ,a :action Back<CR>")
    enterCommand("nmap ,b :action Back<Cr>")
    enterCommand("nmap ,c :action Back<cr>")
    enterCommand("nmap ,d :action Back<ENTER>")
    enterCommand("nmap ,e :action Back<Enter>")
    enterCommand("nmap ,f :action Back<enter>")
    enterCommand("nmap ,g :action Back<C-M>")
    enterCommand("nmap ,h :action Back<C-m>")
    enterCommand("nmap ,i :action Back<c-m>")

    assertCommandOutput("nmap",
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
    enterCommand("nnoremap ,a :action Back<CR>")
    enterCommand("nnoremap ,b :action Back<Cr>")
    enterCommand("nnoremap ,c :action Back<cr>")
    enterCommand("nnoremap ,d :action Back<ENTER>")
    enterCommand("nnoremap ,e :action Back<Enter>")
    enterCommand("nnoremap ,f :action Back<enter>")
    enterCommand("nnoremap ,g :action Back<C-M>")
    enterCommand("nnoremap ,h :action Back<C-m>")
    enterCommand("nnoremap ,i :action Back<c-m>")

    assertCommandOutput("nnoremap",
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
    enterCommand("map A :echo 42<CR>")
    typeText("A")
    assertExOutput("42")
    assertEquals(
      "map A :echo 42<CR>",
      injector.historyGroup.getEntries(VimHistory.Type.Command, 0, 0).last().entry,
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
    enterCommand("map <Enter> <Action>(EditorSelectWord)")
    typeText("<Enter>")
    assertState(
      """
     Lorem Ipsum

     Lorem ipsum dolor sit amet,
     ${s}${c}consectetur${se} adipiscing elit
     Sed in orci mauris.
     Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()
    )
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

    enterCommand("map k abcx")

    val exception = assertThrows<Throwable> {
      LoggedErrorProcessor.executeWith<Throwable>(OnlyThrowLoggedErrorProcessor) {
        typeText("k")
      }
    }
    assertEquals(ExceptionHandler.exceptionMessage, exception.cause!!.cause!!.cause!!.message)

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
    enterCommand("map k :echo 4<CR> \\| :echo 42<CR>")
    assertNull(injector.outputPanel.getCurrentOutputPanel())
    typeText("k")
    assertEquals("4\n42", injector.outputPanel.getCurrentOutputPanel()!!.text)
  }

  @Test
  fun `test map! parsing`() {
    doTest(
      listOf("i", "foo", "<Esc>"),
      """|
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """|
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |ba${c}rconsectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin()
    ) {
      enterCommand("map! foo bar")
    }
  }

  @Test
  fun `test map! parsing 2`() {
    doTest(
      "!",
      """|
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """|
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |${c}Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin()
    ) {
      // Make sure we parse `map !` differently to `map!`
      // Remember that `map` is NVO and `map!` is IC. If this is correctly parsed, we have to test it in e.g., Normal
      enterCommand("map ! dd")
    }
  }

  @Test
  fun `test map with trailing spaces`() {
    doTest(
      listOf("i", "b", "<Esc>"),
      "",
      "test    "
    ) {
      enterCommand("imap b test    ")
    }
  }
}
