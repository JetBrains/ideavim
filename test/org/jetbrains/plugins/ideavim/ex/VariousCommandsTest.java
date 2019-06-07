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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex;

import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class VariousCommandsTest extends VimTestCase {
  // VIM-550 |:put|
  public void testPutCreatesNewLine() {
    myFixture.configureByText("a.txt", "Test\n" +
                                       "Hello <caret>World!\n");
    typeText(parseKeys("\"ayw"));
    typeText(commandToKeys("put a"));
    myFixture.checkResult("Test\n" +
                          "Hello World!\n" +
                          "<caret>World\n");
  }

  // VIM-551 |:put|
  public void testPutDefault() {
    myFixture.configureByText("a.txt", "<caret>Hello World!\n");
    typeText(parseKeys("yw"));
    typeText(commandToKeys("put"));
    myFixture.checkResult("Hello World!\n" +
                          "<caret>Hello \n");
  }

  // VIM-652 |:action|
  public void testEditorRightAction() {
    configureByText("<caret>foo\n" +
                    "bar\n");
    typeText(commandToKeys("action EditorRight"));
    assertMode(CommandState.Mode.COMMAND);
    myFixture.checkResult("f<caret>oo\n" +
                          "bar\n");
  }

  // VIM-862 |:action| in visual character mode
  public void testExCommandInVisualCharacterMode() {
    configureByJavaText("-----\n" +
                        "1<caret>2345\n" +
                        "abcde\n" +
                        "-----");
    typeText(parseKeys("vjl"));
    typeText(commandToKeys("'<,'>action CommentByBlockComment"));
    assertMode(CommandState.Mode.VISUAL);
    myFixture.checkResult("-----\n" +
                          "1/*2345\n" +
                          "abc*/de\n" +
                          "-----");
  }

  // VIM-862 |:action|
  public void testExCommandInVisualCharacterModeSameLine() {
    configureByJavaText("1<caret>2345\n" +
                        "abcde\n");
    typeText(parseKeys("vl"));
    typeText(commandToKeys("'<,'>action CommentByBlockComment"));
    assertMode(CommandState.Mode.VISUAL);
    myFixture.checkResult("1/*23*/45\n" +
                          "abcde\n");
  }

  // VIM-862 |:action| in visual line mode
  public void testExCommandInVisualLineMode() {
    configureByJavaText("-----\n" +
                        "1<caret>2345\n" +
                        "abcde\n" +
                        "-----");
    typeText(parseKeys("Vj"));
    typeText(commandToKeys("'<,'>action CommentByBlockComment"));
    assertMode(CommandState.Mode.VISUAL);
    myFixture.checkResult("-----\n" +
                          "/*12345\n" +
                          "abcde*/\n" +
                          "-----");
  }

  // VIM-862 |:action| in visual block mode
  public void testExCommandInVisualBlockMode() {
    configureByJavaText("-----\n" +
                        "1<caret>2345\n" +
                        "abcde\n" +
                        "-----");
    typeText(parseKeys("<C-V>lj"));
    typeText(commandToKeys("'<,'>action CommentByBlockComment"));
    assertMode(CommandState.Mode.VISUAL);
    myFixture.checkResult("-----\n" +
                          "1/*23*/45\n" +
                          "a/*bc*/de\n" +
                          "-----");
  }
}
