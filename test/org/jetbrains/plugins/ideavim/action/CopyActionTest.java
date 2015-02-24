package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.option.ListOption;
import com.maddyhome.idea.vim.option.Options;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class CopyActionTest extends VimTestCase {
  // |y| |p| |count|
  public void testYankPutCharacters() {
    typeTextInFile(parseKeys("y2h", "p"),
                   "one two<caret> three\n");
    myFixture.checkResult("one twwoo three\n");
  }

  // |yy|
  public void testYankLine() {
    typeTextInFile(parseKeys("yy", "p"),
                   "one\n" +
                   "tw<caret>o\n" +
                   "three\n");
    myFixture.checkResult("one\n" +
                          "two\n" +
                          "two\n" +
                          "three\n");
  }

  // VIM-723 |p|
  public void testYankPasteToEmptyLine() {
    typeTextInFile(parseKeys("yiw", "j", "p"),
                   "foo\n" +
                   "\n" +
                   "bar\n");
    myFixture.checkResult("foo\n" +
                          "foo\n" +
                          "bar\n");
  }

  // VIM-390 |yy| |p|
  public void testYankLinePasteAtLastLine() {
    typeTextInFile(parseKeys("yy", "p"),
                   "one two\n" +
                   "<caret>three four\n");
    myFixture.checkResult("one two\n" +
                          "three four\n" +
                          "three four\n");
  }

  // |register| |y|
  public void testYankRegister() {
    typeTextInFile(parseKeys("\"ayl", "l", "\"byl", "\"ap", "\"bp"),
                   "hel<caret>lo world\n");
    myFixture.checkResult("hellolo world\n");
  }

  // |P|
  public void testYankPutBefore() {
    typeTextInFile(parseKeys("y2l", "P"),
                   "<caret>two\n");
    myFixture.checkResult("twtwo\n");
  }

  public void testWrongYankQuoteMotion() {
    assertPluginError(false);
    typeTextInFile(parseKeys("y\""),
                   "one <caret>two\n" +
                   "three\n" +
                   "four\n");
    assertPluginError(true);
  }

  public void testWrongYankQuoteYankLine() {
    assertPluginError(false);
    typeTextInFile(parseKeys("y\"", "yy", "p"), "one <caret>two\n" +
                                                "three\n" +
                                                "four\n");
    assertPluginError(false);
    myFixture.checkResult("one two\n" +
                          "one two\n" +
                          "three\n" +
                          "four\n");
  }

  public void testWrongYankRegisterMotion() {
    final Editor editor = typeTextInFile(parseKeys("y\"", "0"),
                                         "one <caret>two\n" +
                                         "three\n" +
                                         "four\n");
    assertEquals(0, editor.getCaretModel().getOffset());
  }

  // VIM-632 |CTRL-V| |v_y| |p|
  public void testYankVisualBlock() {
    typeTextInFile(parseKeys("<C-V>", "jl", "yl", "p"),
                   "<caret>* one\n" +
                   "* two\n");

    // XXX:
    // The correct output should be:
    //
    // * * one
    // * * two
    //
    // The problem is that the selection range should be 1-char wide when entering the visual block mode

    myFixture.checkResult("* * one\n" +
                          "* * two\n");
    assertSelection(null);
    assertOffset(2);
  }

  // VIM-632 |CTRL-V| |v_y|
  public void testStateAfterYankVisualBlock() {
    typeTextInFile(parseKeys("<C-V>", "jl", "y"),
                   "<caret>foo\n" +
                   "bar\n");
    assertOffset(0);
    assertMode(CommandState.Mode.COMMAND);
    assertSelection(null);
  }

  // VIM-476 |yy| |'clipboard'|
  public void testClipboardUnnamed() {
    assertEquals('\"', VimPlugin.getRegister().getDefaultRegister());
    final ListOption clipboardOption = Options.getInstance().getListOption(Options.CLIPBOARD);
    assertNotNull(clipboardOption);
    clipboardOption.set("unnamed");
    assertEquals('*', VimPlugin.getRegister().getDefaultRegister());
    typeTextInFile(parseKeys("yy"),
                   "foo\n" +
                   "<caret>bar\n" +
                   "baz\n");
    final Register starRegister = VimPlugin.getRegister().getRegister('*');
    if (starRegister != null) {
      assertEquals("bar\n", starRegister.getText());
    }
  }

  // VIM-792 |"*| |yy| |p|
  public void testLineWiseClipboardYankPaste() {
    configureByText("<caret>foo\n");
    typeText(parseKeys("\"*yy", "\"*p"));
    final Register register = VimPlugin.getRegister().getRegister('*');
    if (register != null) {
      assertEquals("foo\n", register.getText());
      myFixture.checkResult("foo\n" +
                            "<caret>foo\n");
    }
  }

  // VIM-792 |"*| |CTRL-V| |v_y| |p|
  public void testBlockWiseClipboardYankPaste() {
    configureByText("<caret>foo\n" +
                    "bar\n" +
                    "baz\n");
    typeText(parseKeys("<C-V>j", "\"*y", "\"*p"));
    final Register register = VimPlugin.getRegister().getRegister('*');
    if (register != null) {
      assertEquals("f\n" +
                   "b", register.getText());
      myFixture.checkResult("ffoo\n" +
                            "bbar\n" +
                            "baz\n");
    }
  }
}
