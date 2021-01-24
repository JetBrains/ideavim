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
package org.jetbrains.plugins.ideavim.ex.handler.mapping

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.commandState
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssert

/**
 * @author vlan
 */
class MapCommandTest : VimTestCase() {
  fun testMapKtoJ() {
    configureByText("""
  ${c}foo
  bar
  
  """.trimIndent())
    typeText(commandToKeys("nmap k j"))
    assertPluginError(false)
    assertOffset(0)
    typeText(StringHelper.parseKeys("k"))
    assertOffset(4)
  }

  fun testInsertMapJKtoEsc() {
    configureByText("${c}World!\n")
    typeText(commandToKeys("imap jk <Esc>"))
    assertPluginError(false)
    typeText(StringHelper.parseKeys("i", "Hello, ", "jk"))
    myFixture.checkResult("Hello, World!\n")
    assertMode(CommandState.Mode.COMMAND)
    assertOffset(6)
  }

  fun testBackslashEscape() {
    configureByText("\n")
    typeText(commandToKeys("imap \\\\,\\<,\\n foo"))
    assertPluginError(false)
    typeText(StringHelper.stringToKeys("i\\,<,\\n"))
    myFixture.checkResult("foo\n")
  }

  fun testBackslashAtEnd() {
    configureByText("\n")
    typeText(commandToKeys("imap foo\\ bar"))
    assertPluginError(false)
    typeText(StringHelper.stringToKeys("ifoo\\"))
    myFixture.checkResult("bar\n")
  }

  fun testUnfinishedSpecialKey() {
    configureByText("\n")
    typeText(commandToKeys("imap <Esc foo"))
    typeText(StringHelper.stringToKeys("i<Esc"))
    myFixture.checkResult("foo\n")
  }

  fun testUnknownSpecialKey() {
    configureByText("\n")
    typeText(commandToKeys("imap <foo> bar"))
    typeText(StringHelper.stringToKeys("i<foo>"))
    myFixture.checkResult("bar\n")
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
    assertExOutput("""
  i  <C-Down>      <C-O>gt
  i  bar           <Esc>
  i  foo           bar
  
  """.trimIndent())
    typeText(commandToKeys("map"))
    assertExOutput("""   <C-Down>      gt
n  <Plug>Foo     iHello<Esc>
n  ,f            <Plug>Foo
""")
  }

  fun testRecursiveMapping() {
    configureByText("\n")
    typeText(commandToKeys("imap foo bar"))
    typeText(commandToKeys("imap bar baz"))
    typeText(commandToKeys("imap baz quux"))
    typeText(StringHelper.parseKeys("i", "foo"))
    myFixture.checkResult("quux\n")
  }

  fun testNonRecursiveMapping() {
    configureByText("\n")
    typeText(commandToKeys("inoremap a b"))
    assertPluginError(false)
    typeText(commandToKeys("inoremap b a"))
    typeText(StringHelper.parseKeys("i", "ab"))
    myFixture.checkResult("ba\n")
  }

  fun testNonRecursiveMapTable() {
    configureByText("\n")
    typeText(commandToKeys("inoremap jj <Esc>"))
    typeText(commandToKeys("imap foo bar"))
    typeText(commandToKeys("imap"))
    assertExOutput("""
  i  foo           bar
  i  jj          * <Esc>
  
  """.trimIndent())
  }

  fun testNop() {
    configureByText("""
  ${c}foo
  bar
  
  """.trimIndent())
    typeText(commandToKeys("noremap <Right> <nop>"))
    assertPluginError(false)
    typeText(StringHelper.parseKeys("l", "<Right>"))
    assertPluginError(false)
    myFixture.checkResult("""
  foo
  bar
  
  """.trimIndent())
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
    typeText(commandToKeys("nmap <expr> ,f /f<CR>"))
    typeText(commandToKeys("nmap <unique> ,g /g<CR>"))
    typeText(commandToKeys("nmap"))
    assertExOutput("""
  n  ,a            /a<CR>
  n  ,b            /b<CR>
  n  ,c            /c<CR>
  n  ,d            /d<CR>
  n  ,g            /g<CR>
  
  """.trimIndent())
  }

  // VIM-645 |:nmap|
  fun testMapSpace() {
    configureByText("foo\n")
    typeText(commandToKeys("nmap <space> dw"))
    typeText(StringHelper.parseKeys(" "))
    myFixture.checkResult("\n")
    typeText(StringHelper.parseKeys("i", " ", "<Esc>"))
    myFixture.checkResult(" \n")
  }

  // VIM-661 |:noremap| |r|
  fun testNoMappingInReplaceCharacterArgument() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("noremap A Z"))
    typeText(StringHelper.parseKeys("rA"))
    myFixture.checkResult("Aoo\n")
  }

  // VIM-661 |:omap| |d| |t|
  fun testNoMappingInNonFirstCharOfOperatorPendingMode() {
    configureByText("${c}foo, bar\n")
    typeText(commandToKeys("omap , ?"))
    typeText(StringHelper.parseKeys("dt,"))
    myFixture.checkResult(", bar\n")
  }

  // VIM-666 |:imap|
  fun testIgnoreEverythingAfterBar() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("imap a b |c \" Something else"))
    typeText(StringHelper.parseKeys("ia"))
    myFixture.checkResult("b foo\n")
  }

  // VIM-666 |:imap|
  fun testBarEscaped() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("imap a b \\| c"))
    typeText(StringHelper.parseKeys("ia"))
    myFixture.checkResult("b | cfoo\n")
  }

  // VIM-666 |:imap|
  fun testBarEscapedSeveralSpaces() {
    configureByText("${c}foo\n")
    typeText(commandToKeys("imap a b \\| c    |"))
    typeText(StringHelper.parseKeys("ia"))
    myFixture.checkResult("b | c    foo\n")
  }

  // VIM-670 |:map|
  fun testFirstCharIsNonRecursive() {
    configureByText("\n")
    typeText(commandToKeys("map ab abcd"))
    typeText(StringHelper.parseKeys("ab"))
    myFixture.checkResult("bcd\n")
  }

  // VIM-676 |:map|
  fun testBackspaceCharacterInVimRc() {
    configureByText("\n")
    VimScriptParser.executeText(VimScriptParser.readText("inoremap # X\u0008#\n"))
    typeText(StringHelper.parseKeys("i", "#", "<Esc>"))
    myFixture.checkResult("#\n")
    assertMode(CommandState.Mode.COMMAND)
    typeText(commandToKeys("imap"))
    assertExOutput("i  #           * X<C-H>#\n")
  }

  // VIM-679 |:map|
  fun testCancelCharacterInVimRc() {
    configureByText("""
  ${c}foo
  bar
  
  """.trimIndent())
    VimScriptParser.executeText(VimScriptParser.readText("map \u0018i dd\n"))
    typeText(StringHelper.parseKeys("i", "#", "<Esc>"))
    myFixture.checkResult("""
  #foo
  bar
  
  """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    typeText(commandToKeys("map"))
    assertExOutput("   <C-X>i        dd\n")
    typeText(StringHelper.parseKeys("<C-X>i"))
    myFixture.checkResult("bar\n")
  }

  // VIM-679 |:map|
  fun testBarCtrlVEscaped() {
    configureByText("${c}foo\n")
    VimScriptParser.executeText(listOf("imap a b \u0016|\u0016| c |\n"))
    typeText(StringHelper.parseKeys("ia"))
    myFixture.checkResult("b || c foo\n")
  }

  // VIM-679 |:map|
  fun testCtrlMCtrlLAsNewLine() {
    configureByText("${c}foo\n")
    VimScriptParser.executeText(listOf("map A :%s/foo/bar/g\r\u000C\n"))
    typeText(StringHelper.parseKeys("A"))
    myFixture.checkResult("bar\n")
  }

  // VIM-700 |:map|
  fun testRemappingZero() {
    configureByText("x${c}yz\n")
    VimScriptParser.executeText(listOf("map 0 ~"))
    typeText(StringHelper.parseKeys("0"))
    myFixture.checkResult("xYz\n")
  }

  // VIM-700 |:map|
  fun testRemappingZeroStillAllowsZeroToBeUsedInCount() {
    configureByText("a${c}bcdefghijklmnop\n")
    VimScriptParser.executeText(listOf("map 0 ^"))
    typeText(StringHelper.parseKeys("10~"))
    myFixture.checkResult("aBCDEFGHIJKlmnop\n")
  }

  // VIM-700 |:map|
  fun testRemappingDeleteOverridesRemovingLastDigitFromCount() {
    configureByText("a${c}bcdefghijklmnop\n")
    VimScriptParser.executeText(listOf("map <Del> ~"))
    typeText(StringHelper.parseKeys("10<Del>"))
    myFixture.checkResult("aBCDEFGHIJKlmnop\n")
  }

  // VIM-650 |mapleader|
  fun testMapLeader() {
    configureByText("\n")
    typeText(commandToKeys("let mapleader = \",\""))
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"))
    typeText(StringHelper.parseKeys(",z"))
    myFixture.checkResult("zzz\n")
  }

  // VIM-650 |mapleader|
  fun testMapLeaderToSpace() {
    configureByText("\n")
    typeText(commandToKeys("let mapleader = \"\\<SPACE>\""))
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"))
    typeText(StringHelper.parseKeys(" z"))
    myFixture.checkResult("zzz\n")
  }

  // VIM-650 |mapleader|
  fun testMapLeaderToSpaceWithWhitespace() {
    configureByText("\n")
    typeText(commandToKeys("let mapleader = \" \""))
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"))
    typeText(StringHelper.parseKeys(" z"))
    myFixture.checkResult("zzz\n")
  }

  fun testAmbiguousMapping() {
    configureByText("\n")
    typeText(commandToKeys("nmap ,f iHello<Esc>"))
    typeText(commandToKeys("nmap ,fc iBye<Esc>"))
    typeText(StringHelper.parseKeys(",fdh"))
    myFixture.checkResult("Helo\n")
    typeText(StringHelper.parseKeys("diw"))
    myFixture.checkResult("\n")
    typeText(StringHelper.parseKeys(",fch"))
    myFixture.checkResult("Bye\n")
  }

  fun testLongAmbiguousMapping() {
    configureByText("\n")
    typeText(commandToKeys("nmap ,foo iHello<Esc>"))
    typeText(commandToKeys("nmap ,fooc iBye<Esc>"))
    typeText(StringHelper.parseKeys(",foodh"))
    myFixture.checkResult("Helo\n")
    typeText(StringHelper.parseKeys("diw"))
    myFixture.checkResult("\n")
    typeText(StringHelper.parseKeys(",fooch"))
    myFixture.checkResult("Bye\n")
  }

  fun testPlugMapping() {
    configureByText("\n")
    typeText(commandToKeys("nmap ,f <Plug>Foo"))
    typeText(commandToKeys("nmap <Plug>Foo iHello<Esc>"))
    typeText(StringHelper.parseKeys(",fa!<Esc>"))
    myFixture.checkResult("Hello!\n")
  }

  fun testIntersectingCommands() {
    configureByText("123${c}4567890")
    typeText(commandToKeys("map ds h"))
    typeText(commandToKeys("map I 3l"))
    typeText(StringHelper.parseKeys("dI"))
    myFixture.checkResult("123${c}7890")
  }

  fun testIncompleteMapping() {
    configureByText("123${c}4567890")
    typeText(commandToKeys("map <Plug>(Hi)l lll"))
    typeText(commandToKeys("map I <Plug>(Hi)"))
    typeText(StringHelper.parseKeys("Ih"))
    myFixture.checkResult("12${c}34567890")
  }

  fun testIntersectingCommands2() {
    configureByText("123${c}4567890")
    typeText(commandToKeys("map as x"))
    typeText(StringHelper.parseKeys("gas"))
    myFixture.checkResult("123${c}567890")
  }

  fun testMapZero() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 0 w"))
    typeText(StringHelper.parseKeys("0"))
    assertOffset(14)
  }

  fun testMapZeroIgnoredInCount() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 0 w"))
    typeText(StringHelper.parseKeys("10w"))
    assertOffset(51)
  }

  fun testMapNonZeroDigit() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 2 w"))
    typeText(StringHelper.parseKeys("2"))
    assertOffset(14)
  }

  fun testMapNonZeroDigitNotIncludedInCount() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap 2 w"))
    typeText(StringHelper.parseKeys("92"))
    assertOffset(45)
  }

  fun testShiftSpace() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-Space> w"))
    typeText(StringHelper.parseKeys("<S-Space>"))
    myFixture.checkResult("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  fun testShiftLetter() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-D> w"))
    typeText(StringHelper.parseKeys("<S-D>"))
    myFixture.checkResult("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  fun testUppercaseLetter() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap D w"))
    typeText(StringHelper.parseKeys("D"))
    myFixture.checkResult("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  fun `test shift letter doesn't break insert mode`() {
    configureByText("A quick ${c}brown fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
    typeText(commandToKeys("nmap <S-D> w"))
    typeText(StringHelper.parseKeys("<S-D>"))
    myFixture.checkResult("A quick brown ${c}fox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")

    typeText(StringHelper.parseKeys("iD<Esc>"))
    myFixture.checkResult("A quick brown ${c}Dfox jumps over the lazy dog. A quick brown fox jumps over the lazy dog")
  }

  fun `test comment line with action`() {
    configureByJavaText("""
        -----
        1<caret>2345
        abcde
        -----
        """.trimIndent())
    typeText(commandToKeys("map k <Action>(CommentByLineComment)"))
    typeText(StringHelper.parseKeys("k"))
    myFixture.checkResult("""
        -----
        //12345
        abcde
        -----
        """.trimIndent())
  }

  fun `test execute two actions with two mappings`() {
    configureByJavaText("""
          -----
          1<caret>2345
          abcde
          -----
          """.trimIndent())
    typeText(commandToKeys("map k <Action>(CommentByLineComment)"))
    typeText(StringHelper.parseKeys("kk"))
    myFixture.checkResult("""
          -----
          //12345
          //abcde
          -----
          """.trimIndent())
  }

  fun `test execute two actions with single mappings`() {
    configureByJavaText("""
          -----
          1<caret>2345
          abcde
          -----
          """.trimIndent())
    typeText(commandToKeys("map k <Action>(CommentByLineComment)<Action>(CommentByLineComment)"))
    typeText(StringHelper.parseKeys("k"))
    myFixture.checkResult("""
          -----
          //12345
          //abcde
          -----
          """.trimIndent())
  }

  fun `test execute three actions with single mappings`() {
    configureByJavaText("""
          -----
          1<caret>2345
          abcde
          -----
          """.trimIndent())
    typeText(commandToKeys("map k <Action>(CommentByLineComment)<Action>(CommentByLineComment)<Action>(CommentByLineComment)"))
    typeText(StringHelper.parseKeys("k"))
    myFixture.checkResult("""
          -----
          //12345
          //abcde
          //-----
          """.trimIndent())
  }

  fun `test execute action from insert mode`() {
    configureByJavaText("""
          -----
          1<caret>2345
          abcde
          -----
          """.trimIndent())
    typeText(commandToKeys("imap k <Action>(CommentByLineComment)"))
    typeText(StringHelper.parseKeys("ik"))
    myFixture.checkResult("""
          -----
          //12345
          abcde
          -----
          """.trimIndent())
  }

  fun `test execute mapping with a delay`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
          """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map kk l"))
    typeText(StringHelper.parseKeys("k"))

    checkDelayedMapping(text,
      """
              -${c}----
              12345
              abcde
              -----
              """.trimIndent()
    )
  }

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
    typeText(StringHelper.parseKeys("k"))

    checkDelayedMapping(text,
      """
              -----
              12345
              a${c}bcde
              -----
              """.trimIndent()
    )
  }

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
    typeText(StringHelper.parseKeys("k"))

    checkDelayedMapping(text,
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
    typeText(StringHelper.parseKeys("kz"))

    myFixture.checkResult(
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
    typeText(StringHelper.parseKeys("x"))

    TestCase.assertTrue(VimPlugin.isError())
  }

  private fun checkDelayedMapping(before: String, after: String) {
    myFixture.checkResult(before)

    waitAndAssert(5000) { !myFixture.editor.commandState.mappingState.isTimerRunning() }

    myFixture.checkResult(after)
  }
}
