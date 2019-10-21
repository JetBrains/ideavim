/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.ex.handler;

import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;
import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author vlan
 */
public class MapCommandTest extends VimTestCase {
  public void testMapKtoJ() {
    configureByText("<caret>foo\n" +
                    "bar\n");
    typeText(commandToKeys("nmap k j"));
    assertPluginError(false);
    assertOffset(0);
    typeText(parseKeys("k"));
    assertOffset(4);
  }

  public void testInsertMapJKtoEsc() {
    configureByText("<caret>World!\n");
    typeText(commandToKeys("imap jk <Esc>"));
    assertPluginError(false);
    typeText(parseKeys("i", "Hello, ", "jk"));
    myFixture.checkResult("Hello, World!\n");
    assertMode(CommandState.Mode.COMMAND);
    assertOffset(6);
  }

  public void testBackslashEscape() {
    configureByText("\n");
    typeText(commandToKeys("imap \\\\,\\<,\\n foo"));
    assertPluginError(false);
    typeText(stringToKeys("i\\,<,\\n"));
    myFixture.checkResult("foo\n");
  }

  public void testBackslashAtEnd() {
    configureByText("\n");
    typeText(commandToKeys("imap foo\\ bar"));
    assertPluginError(false);
    typeText(stringToKeys("ifoo\\"));
    myFixture.checkResult("bar\n");
  }

  public void testUnfinishedSpecialKey() {
    configureByText("\n");
    typeText(commandToKeys("imap <Esc foo"));
    typeText(stringToKeys("i<Esc"));
    myFixture.checkResult("foo\n");
  }

  public void testUnknownSpecialKey() {
    configureByText("\n");
    typeText(commandToKeys("imap <foo> bar"));
    typeText(stringToKeys("i<foo>"));
    myFixture.checkResult("bar\n");
  }

  public void testMapTable() {
    configureByText("\n");
    typeText(commandToKeys("map <C-Down> gt"));
    typeText(commandToKeys("imap foo bar"));
    typeText(commandToKeys("imap bar <Esc>"));
    typeText(commandToKeys("imap <C-Down> <C-O>gt"));
    typeText(commandToKeys("nmap ,f <Plug>Foo"));
    typeText(commandToKeys("nmap <Plug>Foo iHello<Esc>"));

    typeText(commandToKeys("imap"));
    assertExOutput("i  <C-Down>      <C-O>gt\n" +
                   "i  bar           <Esc>\n" +
                   "i  foo           bar\n");

    typeText(commandToKeys("map"));
    assertExOutput("   <C-Down>      gt\n" +
                   "n  <Plug>Foo     iHello<Esc>\n" +
                   "n  ,f            <Plug>Foo\n");
  }

  public void testRecursiveMapping() {
    configureByText("\n");
    typeText(commandToKeys("imap foo bar"));
    typeText(commandToKeys("imap bar baz"));
    typeText(commandToKeys("imap baz quux"));
    typeText(parseKeys("i", "foo"));
    myFixture.checkResult("quux\n");
  }

  public void testNonRecursiveMapping() {
    configureByText("\n");
    typeText(commandToKeys("inoremap a b"));
    assertPluginError(false);
    typeText(commandToKeys("inoremap b a"));
    typeText(parseKeys("i", "ab"));
    myFixture.checkResult("ba\n");
  }

  public void testNonRecursiveMapTable() {
    configureByText("\n");
    typeText(commandToKeys("inoremap jj <Esc>"));
    typeText(commandToKeys("imap foo bar"));
    typeText(commandToKeys("imap"));
    assertExOutput("i  foo           bar\n" +
                   "i  jj          * <Esc>\n");
  }

  public void testNop() {
    configureByText("<caret>foo\n" +
                    "bar\n");
    typeText(commandToKeys("noremap <Right> <nop>"));
    assertPluginError(false);
    typeText(parseKeys("l", "<Right>"));
    assertPluginError(false);
    myFixture.checkResult("foo\n" +
                          "bar\n");
    assertOffset(1);
    typeText(commandToKeys("nmap"));
    assertExOutput("n  <Right>     * <Nop>\n");
  }

  public void testIgnoreModifiers() {
    configureByText("\n");
    typeText(commandToKeys("nmap <buffer> ,a /a<CR>"));
    typeText(commandToKeys("nmap <nowait> ,b /b<CR>"));
    typeText(commandToKeys("nmap <silent> ,c /c<CR>"));
    typeText(commandToKeys("nmap <special> ,d /d<CR>"));
    typeText(commandToKeys("nmap <script> ,e /e<CR>"));
    typeText(commandToKeys("nmap <expr> ,f /f<CR>"));
    typeText(commandToKeys("nmap <unique> ,g /g<CR>"));
    typeText(commandToKeys("nmap"));
    assertExOutput("n  ,a            /a<CR>\n" +
                   "n  ,b            /b<CR>\n" +
                   "n  ,c            /c<CR>\n" +
                   "n  ,d            /d<CR>\n" +
                   "n  ,g            /g<CR>\n");
  }

  // VIM-645 |:nmap|
  public void testMapSpace() {
    configureByText("foo\n");
    typeText(commandToKeys("nmap <space> dw"));
    typeText(parseKeys(" "));
    myFixture.checkResult("\n");
    typeText(parseKeys("i", " ", "<Esc>"));
    myFixture.checkResult(" \n");
  }

  // VIM-661 |:noremap| |r|
  public void testNoMappingInReplaceCharacterArgument() {
    configureByText("<caret>foo\n");
    typeText(commandToKeys("noremap A Z"));
    typeText(parseKeys("rA"));
    myFixture.checkResult("Aoo\n");
  }

  // VIM-661 |:omap| |d| |t|
  public void testNoMappingInNonFirstCharOfOperatorPendingMode() {
    configureByText("<caret>foo, bar\n");
    typeText(commandToKeys("omap , ?"));
    typeText(parseKeys("dt,"));
    myFixture.checkResult(", bar\n");
  }

  // VIM-666 |:imap|
  public void testIgnoreEverythingAfterBar() {
    configureByText("<caret>foo\n");
    typeText(commandToKeys("imap a b |c \" Something else"));
    typeText(parseKeys("ia"));
    myFixture.checkResult("b foo\n");
  }

  // VIM-666 |:imap|
  public void testBarEscaped() {
    configureByText("<caret>foo\n");
    typeText(commandToKeys("imap a b \\| c"));
    typeText(parseKeys("ia"));
    myFixture.checkResult("b | cfoo\n");
  }

  // VIM-666 |:imap|
  public void testBarEscapedSeveralSpaces() {
    configureByText("<caret>foo\n");
    typeText(commandToKeys("imap a b \\| c    |"));
    typeText(parseKeys("ia"));
    myFixture.checkResult("b | c    foo\n");
  }

  // VIM-670 |:map|
  public void testFirstCharIsNonRecursive() {
    configureByText("\n");
    typeText(commandToKeys("map ab abcd"));
    typeText(parseKeys("ab"));
    myFixture.checkResult("bcd\n");
  }

  // VIM-676 |:map|
  public void testBackspaceCharacterInVimRc() {
    configureByText("\n");
    VimScriptParser.executeText("inoremap # X\u0008#\n");
    typeText(parseKeys("i", "#", "<Esc>"));
    myFixture.checkResult("#\n");
    assertMode(CommandState.Mode.COMMAND);
    typeText(commandToKeys("imap"));
    assertExOutput("i  #           * X<C-H>#\n");
  }

  // VIM-679 |:map|
  public void testCancelCharacterInVimRc() {
    configureByText("<caret>foo\n" +
                    "bar\n");
    VimScriptParser.executeText("map \u0018i dd\n");
    typeText(parseKeys("i", "#", "<Esc>"));
    myFixture.checkResult("#foo\n" +
                          "bar\n");
    assertMode(CommandState.Mode.COMMAND);
    typeText(commandToKeys("map"));
    assertExOutput("   <C-X>i        dd\n");
    typeText(parseKeys("<C-X>i"));
    myFixture.checkResult("bar\n");
  }

  // VIM-679 |:map|
  public void testBarCtrlVEscaped() {
    configureByText("<caret>foo\n");
    VimScriptParser.executeText("imap a b \u0016|\u0016| c |\n");
    typeText(parseKeys("ia"));
    myFixture.checkResult("b || c foo\n");
  }

  // VIM-679 |:map|
  public void testCtrlMCtrlLAsNewLine() {
    configureByText("<caret>foo\n");
    VimScriptParser.executeText("map A :%s/foo/bar/g\r\u000C\n");
    typeText(parseKeys("A"));
    myFixture.checkResult("bar\n");
  }

  // VIM-700 |:map|
  public void testRemappingZero() {
    configureByText("x<caret>yz\n");
    VimScriptParser.executeText("map 0 ~");
    typeText(parseKeys("0"));
    myFixture.checkResult("xYz\n");
  }

  // VIM-700 |:map|
  public void testRemappingZeroStillAllowsZeroToBeUsedInCount() {
    configureByText("a<caret>bcdefghijklmnop\n");
    VimScriptParser.executeText("map 0 ^");
    typeText(parseKeys("10~"));
    myFixture.checkResult("aBCDEFGHIJKlmnop\n");
  }

  // VIM-700 |:map|
  public void testRemappingDeleteOverridesRemovingLastDigitFromCount() {
    configureByText("a<caret>bcdefghijklmnop\n");
    VimScriptParser.executeText("map <Del> ~");
    typeText(parseKeys("10<Del>"));
    myFixture.checkResult("aBCDEFGHIJKlmnop\n");
  }

  // VIM-650 |mapleader|
  public void testMapLeader() {
    configureByText("\n");
    typeText(commandToKeys("let mapleader = \",\""));
    typeText(commandToKeys("nmap <Leader>z izzz<Esc>"));
    typeText(parseKeys(",z"));
    myFixture.checkResult("zzz\n");
  }

  public void testAmbiguousMapping() {
    configureByText("\n");
    typeText(commandToKeys("nmap ,f iHello<Esc>"));
    typeText(commandToKeys("nmap ,fc iBye<Esc>"));
    typeText(parseKeys(",fdh"));
    myFixture.checkResult("Helo\n");

    typeText(parseKeys("diw"));
    myFixture.checkResult("\n");

    typeText(parseKeys(",fch"));
    myFixture.checkResult("Bye\n");
  }

  public void testLongAmbiguousMapping() {
    configureByText("\n");
    typeText(commandToKeys("nmap ,foo iHello<Esc>"));
    typeText(commandToKeys("nmap ,fooc iBye<Esc>"));
    typeText(parseKeys(",foodh"));
    myFixture.checkResult("Helo\n");

    typeText(parseKeys("diw"));
    myFixture.checkResult("\n");

    typeText(parseKeys(",fooch"));
    myFixture.checkResult("Bye\n");
  }

  public void testPlugMapping() {
    configureByText("\n");
    typeText(commandToKeys("nmap ,f <Plug>Foo"));
    typeText(commandToKeys("nmap <Plug>Foo iHello<Esc>"));
    typeText(parseKeys(",fa!<Esc>"));
    myFixture.checkResult("Hello!\n");
  }

  public void testIntersectingCommands() {
    configureByText("123<caret>4567890");
    typeText(commandToKeys("map ds h"));
    typeText(commandToKeys("map I 3l"));
    typeText(parseKeys("dI"));
    myFixture.checkResult("123<caret>7890");
  }

  public void testIncompleteMapping() {
    configureByText("123<caret>4567890");
    typeText(commandToKeys("map <Plug>(Hi)l lll"));
    typeText(commandToKeys("map I <Plug>(Hi)"));
    typeText(parseKeys("Ih"));
    myFixture.checkResult("12<caret>34567890");
  }

  public void testIntersectingCommands2() {
    configureByText("123<caret>4567890");
    typeText(commandToKeys("map as x"));
    typeText(parseKeys("gas"));
    myFixture.checkResult("123<caret>567890");
  }
}
