/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.VimStateMachine;
import com.maddyhome.idea.vim.register.Register;
import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

/**
 * @author vlan
 */
public class CopyActionTest extends VimTestCase {
  // |y| |p| |count|
  public void testYankPutCharacters() {
    typeTextInFile("y2h" + "p", "one two<caret> three\n");
    assertState("one twwoo three\n");
  }

  // |yy|
  public void testYankLine() {
    typeTextInFile("yy" + "p", "one\n" + "tw<caret>o\n" + "three\n");
    assertState("one\n" + "two\n" + "two\n" + "three\n");
  }

  // VIM-723 |p|
  public void testYankPasteToEmptyLine() {
    typeTextInFile("yiw" + "j" + "p", "foo\n" + "\n" + "bar\n");
    assertState("foo\n" + "foo\n" + "bar\n");
  }

  // VIM-390 |yy| |p|
  public void testYankLinePasteAtLastLine() {
    typeTextInFile("yy" + "p", "one two\n" + "<caret>three four\n");
    assertState("one two\n" + "three four\n" + "three four\n");
  }

  // |register| |y|
  public void testYankRegister() {
    typeTextInFile("\"ayl" + "l" + "\"byl" + "\"ap" + "\"bp", "hel<caret>lo world\n");
    assertState("hellolo world\n");
  }

  // |register| |y| |quote|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testYankRegisterUsesLastEnteredRegister() {
    typeTextInFile("\"a\"byl" + "\"ap", "hel<caret>lo world\n");
    assertState("helllo world\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testYankAppendRegister() {
    typeTextInFile("\"Ayl" + "l" + "\"Ayl" + "\"Ap", "hel<caret>lo world\n");
    assertState("hellolo world\n");
  }

  public void testYankWithInvalidRegister() {
    typeTextInFile("\"&", "hel<caret>lo world\n");
    assertPluginError(true);
  }

  // |P|
  public void testYankPutBefore() {
    typeTextInFile("y2l" + "P", "<caret>two\n");
    assertState("twtwo\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  public void testWrongYankQuoteMotion() {
    assertPluginError(false);
    typeTextInFile("y\"", "one <caret>two\n" + "three\n" + "four\n");
    assertPluginError(true);
  }

  public void testWrongYankQuoteYankLine() {
    assertPluginError(false);
    typeTextInFile("y\"" + "yy" + "p", "one <caret>two\n" + "three\n" + "four\n");
    assertPluginError(false);
    assertState("one two\n" + "one two\n" + "three\n" + "four\n");
  }

  public void testWrongYankRegisterMotion() {
    final Editor editor = typeTextInFile("y\"" + "0", "one <caret>two\n" + "three\n" + "four\n");
    assertEquals(0, editor.getCaretModel().getOffset());
  }

  // VIM-632 |CTRL-V| |v_y| |p|
  public void testYankVisualBlock() {
    typeTextInFile("<C-V>" + "jl" + "yl" + "p", "<caret>* one\n" + "* two\n");

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
    typeTextInFile("<C-V>" + "jl" + "y", "<caret>foo\n" + "bar\n");
    assertOffset(0);
    assertMode(VimStateMachine.Mode.COMMAND);
    assertSelection(null);
  }

  // VIM-476 |yy| |'clipboard'|
  // TODO: Review this test
  // This doesn't use the system clipboard, but the TestClipboardModel
  public void testClipboardUnnamed() {
    configureByText("foo\n" + "<caret>bar\n" + "baz\n");
    assertEquals('\"', VimPlugin.getRegister().getDefaultRegister());
    enterCommand("set clipboard=unnamed");
    assertEquals('*', VimPlugin.getRegister().getDefaultRegister());
    typeText("yy");
    final Register starRegister = VimPlugin.getRegister().getRegister('*');
    assertNotNull(starRegister);
    assertEquals("bar\n", starRegister.getText());
  }

  // VIM-792 |"*| |yy| |p|
  // TODO: Review this test
  // This doesn't use the system clipboard, but the TestClipboardModel
  public void testLineWiseClipboardYankPaste() {
    configureByText("<caret>foo\n");
    typeText("\"*yy" + "\"*p");
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
    typeText("<C-V>j" + "\"*y" + "\"*p");
    final Register register = VimPlugin.getRegister().getRegister('*');
    assertNotNull(register);
    assertEquals("f\n" + "b", register.getText());
    assertState("ffoo\n" + "bbar\n" + "baz\n");
  }

  // VIM-1431
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testPutInEmptyFile() {
    VimPlugin.getRegister().setKeys('a', VimInjectorKt.getInjector().getParser().parseKeys("test"));
    typeTextInFile("\"ap", "");
    assertState("test");
  }

  public void testOverridingRegisterWithEmptyTag() {
    configureByText("<root>\n" + "<a><caret>value</a>\n" + "<b></b>\n" + "</root>\n");
    typeText("dit", "j", "cit", "<C-R>\"");
    assertState("<root>\n" + "<a></a>\n" + "<b>value</b>\n" + "</root>\n");
  }
}
