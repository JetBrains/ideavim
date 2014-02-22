package org.jetbrains.plugins.ideavim.option;

import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.option.iNoRemap.INoRemap;
import com.maddyhome.idea.vim.option.iNoRemap.INoRemapResult;
import org.jetbrains.plugins.ideavim.VimTestCase;

public class INoRemapTest extends VimTestCase {
  public void testLineIsUseableTrueForINoRemapWithSupportedArgs() {
    INoRemap iNoRemap = new INoRemap(KeyParser.getInstance());
    INoRemapResult result = iNoRemap.lineIsUseableINoRemap("inoremap jk <esc>");
    assertTrue(result == INoRemapResult.True);
  }

  public void testLineIsUseableFalseIsDoesNotContainINoRemap() {
    INoRemap iNoRemap = new INoRemap(KeyParser.getInstance());
    INoRemapResult result = iNoRemap.lineIsUseableINoRemap("imap jk <esc>");
    assertTrue(result == INoRemapResult.False);
  }

  public void testLineIsUseableFalseIfLineHasLessThanCorrectNumberOfArgs() {
    INoRemap iNoRemap = new INoRemap(KeyParser.getInstance());
    INoRemapResult result = iNoRemap.lineIsUseableINoRemap("inoremap jk");
    assertTrue(result == INoRemapResult.False);
  }

  public void testLineIsUseableFalseIfLineHasMoreThanCorrectNumberOfArgs() {
    INoRemap iNoRemap = new INoRemap(KeyParser.getInstance());
    INoRemapResult result = iNoRemap.lineIsUseableINoRemap("inoremap jk <esc> <esc>");
    assertTrue(result == INoRemapResult.False);
  }

  public void testLineIsUseableNotImplementedIfSecondArgDoesNotEqualBracketedEsc() {
    INoRemap iNoRemap = new INoRemap(KeyParser.getInstance());
    INoRemapResult result = iNoRemap.lineIsUseableINoRemap("inoremap jk esc");
    assertTrue(result == INoRemapResult.NotImplemented);
  }

  public void testTryToAddCustomEscapeAddsForValidINoRemapLine() {
    KeyParser kp = KeyParser.getInstance();
    INoRemap iNoRemap = new INoRemap(kp);
    INoRemapResult result = iNoRemap.tryToAddCustomEscape("inoremap jk <esc>");
    myFixture.configureByText("a.txt", "Hello <caret>World!\n");
    runExCommand("inoremap jk <esc>");
    typeText(StringHelper.stringToKeys("i"));
    assertMode(CommandState.Mode.INSERT);
    typeText(StringHelper.stringToKeys("jk"));
    assertMode(CommandState.Mode.COMMAND);
  }
}
