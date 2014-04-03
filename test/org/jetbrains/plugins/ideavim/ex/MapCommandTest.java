package org.jetbrains.plugins.ideavim.ex;

import com.maddyhome.idea.vim.command.CommandState;
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

    typeText(commandToKeys("imap"));
    assertExOutput("i  <C-Down>      <C-O>gt\n" +
                   "i  bar           <Esc>\n" +
                   "i  foo           bar\n");

    typeText(commandToKeys("map"));
    assertExOutput("   <C-Down>      gt\n");
  }
}
