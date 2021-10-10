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

package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.option.StringListOption;
import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class CopyActionTest extends VimTestCase {
  // |y| |p| |count|
  public void testYankPutCharacters() {
    typeTextInFile(parseKeys("y2h", "p"), "one two<caret> three\n");
    assertState("one twwoo three\n");
  }

  // |yy|
  public void testYankLine() {
    typeTextInFile(parseKeys("yy", "p"), "one\n" + "tw<caret>o\n" + "three\n");
    assertState("one\n" + "two\n" + "two\n" + "three\n");
  }

  // VIM-723 |p|
  public void testYankPasteToEmptyLine() {
    typeTextInFile(parseKeys("yiw", "j", "p"), "foo\n" + "\n" + "bar\n");
    assertState("foo\n" + "foo\n" + "bar\n");
  }

  // VIM-390 |yy| |p|
  public void testYankLinePasteAtLastLine() {
    typeTextInFile(parseKeys("yy", "p"), "one two\n" + "<caret>three four\n");
    assertState("one two\n" + "three four\n" + "three four\n");
  }

  // |register| |y|
  public void testYankRegister() {
    typeTextInFile(parseKeys("\"ayl", "l", "\"byl", "\"ap", "\"bp"), "hel<caret>lo world\n");
    assertState("hellolo world\n");
  }

  // |register| |y| |quote|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testYankRegisterUsesLastEnteredRegister() {
    typeTextInFile(parseKeys("\"a\"byl", "\"ap"), "hel<caret>lo world\n");
    assertState("helllo world\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testYankAppendRegister() {
    typeTextInFile(parseKeys("\"Ayl", "l", "\"Ayl", "\"Ap"), "hel<caret>lo world\n");
    assertState("hellolo world\n");
  }

  public void testYankWithInvalidRegister() {
    typeTextInFile(parseKeys("\"&"), "hel<caret>lo world\n");
    assertPluginError(true);
  }

  // |P|
  public void testYankPutBefore() {
    typeTextInFile(parseKeys("y2l", "P"), "<caret>two\n");
    assertState("twtwo\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  public void testWrongYankQuoteMotion() {
    assertPluginError(false);
    typeTextInFile(parseKeys("y\""), "one <caret>two\n" + "three\n" + "four\n");
    assertPluginError(true);
  }

  public void testWrongYankQuoteYankLine() {
    assertPluginError(false);
    typeTextInFile(parseKeys("y\"", "yy", "p"), "one <caret>two\n" + "three\n" + "four\n");
    assertPluginError(false);
    assertState("one two\n" + "one two\n" + "three\n" + "four\n");
  }

  public void testWrongYankRegisterMotion() {
    final Editor editor = typeTextInFile(parseKeys("y\"", "0"), "one <caret>two\n" + "three\n" + "four\n");
    assertEquals(0, editor.getCaretModel().getOffset());
  }

  // VIM-632 |CTRL-V| |v_y| |p|
  public void testYankVisualBlock() {
    typeTextInFile(parseKeys("<C-V>", "jl", "yl", "p"), "<caret>* one\n" + "* two\n");

    // XXX:
    // The correct output should be:
    //
    // * * one
    // * * two
    //
    // The problem is that the selection range should be 1-char wide when entering the visual block mode

    assertState("* * one\n" + "* * two\n");
    assertSelection(null);
    assertOffset(2);
  }

  // VIM-632 |CTRL-V| |v_y|
  public void testStateAfterYankVisualBlock() {
    typeTextInFile(parseKeys("<C-V>", "jl", "y"), "<caret>foo\n" + "bar\n");
    assertOffset(0);
    assertMode(CommandState.Mode.COMMAND);
    assertSelection(null);
  }

  // VIM-476 |yy| |'clipboard'|
  // TODO: Review this test
  // This doesn't use the system clipboard, but the TestClipboardModel
  public void testClipboardUnnamed() throws ExException {
    assertEquals('\"', VimPlugin.getRegister().getDefaultRegister());
    final StringListOption clipboardOption = OptionsManager.INSTANCE.getClipboard();
    assertNotNull(clipboardOption);
    clipboardOption.set("unnamed");
    assertEquals('*', VimPlugin.getRegister().getDefaultRegister());
    typeTextInFile(parseKeys("yy"), "foo\n" + "<caret>bar\n" + "baz\n");
    final Register starRegister = VimPlugin.getRegister().getRegister('*');
    assertNotNull(starRegister);
    assertEquals("bar\n", starRegister.getText());
  }

  // VIM-792 |"*| |yy| |p|
  // TODO: Review this test
  // This doesn't use the system clipboard, but the TestClipboardModel
  public void testLineWiseClipboardYankPaste() {
    configureByText("<caret>foo\n");
    typeText(parseKeys("\"*yy", "\"*p"));
    final Register register = VimPlugin.getRegister().getRegister('*');
    assertNotNull(register);
    assertEquals("foo\n", register.getText());
    assertState("foo\n" + "<caret>foo\n");
  }

  // VIM-792 |"*| |CTRL-V| |v_y| |p|
  // TODO: Review this test
  // This doesn't use the system clipboard, but the TestClipboardModel
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testBlockWiseClipboardYankPaste() {
    configureByText("<caret>foo\n" + "bar\n" + "baz\n");
    typeText(parseKeys("<C-V>j", "\"*y", "\"*p"));
    final Register register = VimPlugin.getRegister().getRegister('*');
    assertNotNull(register);
    assertEquals("f\n" + "b", register.getText());
    assertState("ffoo\n" + "bbar\n" + "baz\n");
  }

  // VIM-1431
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testPutInEmptyFile() {
    VimPlugin.getRegister().setKeys('a', parseKeys("test"));
    typeTextInFile(parseKeys("\"ap"), "");
    assertState("test");
  }

  public void testOverridingRegisterWithEmptyTag() {
    configureByText("<root>\n" + "<a><caret>value</a>\n" + "<b></b>\n" + "</root>\n");
    typeText(parseKeys("dit", "j", "cit", "<C-R>\""));
    assertState("<root>\n" + "<a></a>\n" + "<b>value</b>\n" + "</root>\n");
  }
}
