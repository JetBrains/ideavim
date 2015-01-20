package org.jetbrains.plugins.ideavim.ex;

import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.helper.CharacterHelper;
import com.maddyhome.idea.vim.option.Options;
import org.jetbrains.plugins.ideavim.VimTestCase;

public class SetKeywordCommandTest extends VimTestCase {

  public void setUp() throws Exception {
    super.setUp();
  }

  private void setKeyword(String val) {
    Options.getInstance().parseOptionLine(myFixture.getEditor(), "iskeyword" + val, false);
  }

  private void assertIsKeyword(char c){
    CharacterHelper.CharacterType charType = CharacterHelper.charType(c, false);
    assertSame(CharacterHelper.CharacterType.KEYWORD, charType);
  }

  private void assertIsNotKeyword(char c) {
    CharacterHelper.CharacterType charType = CharacterHelper.charType(c, false);
    assertSame(CharacterHelper.CharacterType.PUNCTUATION, charType);
  }

  public void testAddsACharByChar() throws ExException {
    setKeyword("=-");
    assertIsKeyword('-');
  }

  public void testAddsACharByAsciiCode() throws ExException {
    setKeyword("=" + (int)'-');
    assertIsKeyword('-');
  }

  public void testAddsARange() throws ExException {
    setKeyword("=a-c");
    assertIsKeyword('a');
    assertIsKeyword('b');
    assertIsKeyword('c');
  }

  public void testAtSignRepresentsAllLetters() throws ExException {
    setKeyword("=@");
    assertIsKeyword('A');
    assertIsKeyword('Ā');
  }

  public void testRangeOfAtSignToAtSignRepresentsAtSign() throws ExException {
    setKeyword("=@-@");
    assertIsKeyword('@');
  }

  public void testCaretRemovesAChar() throws ExException {
    setKeyword("=a");
    setKeyword("+=^a");
    assertIsNotKeyword('a');
  }

  public void testCaretRemovesARange() throws ExException {
    setKeyword("=a-c");
    setKeyword("+=^b-c");
    assertIsKeyword('a');
    assertIsNotKeyword('b');
    assertIsNotKeyword('c');
  }

  public void testCaretAloneRepresentsACaret() throws ExException {
    setKeyword("=^");
    assertIsKeyword('^');
  }

  public void testMultibyteCharactersAreKeywords() throws ExException {
    assertIsKeyword('Ź');
  }
}
