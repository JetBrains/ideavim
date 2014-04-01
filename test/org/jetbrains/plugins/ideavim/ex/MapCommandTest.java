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
}
