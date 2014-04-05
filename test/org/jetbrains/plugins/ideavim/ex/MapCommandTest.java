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
}
