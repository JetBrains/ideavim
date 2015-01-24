package org.jetbrains.plugins.ideavim.option;

import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.helper.CharacterHelper;
import com.maddyhome.idea.vim.option.KeywordOption;
import com.maddyhome.idea.vim.option.Options;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.ArrayList;

public class KeywordOptionTest extends VimTestCase {

  private KeywordOption option;

  public void setUp() throws Exception {
    super.setUp();
    option = (KeywordOption)Options.getInstance().getOption("iskeyword");
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

  public void testSingleCommaIsAValue() throws ExException {
    option.set(",");
    assertEquals(",", option.values().get(0));
  }

  public void testSingleNegatedCommaIsAValue() throws ExException {
    option.set("^,");
    assertEquals("^,", option.values().get(0));
  }

  public void testCommaInARangeIsAValue() throws ExException {
    option.set("+-,");
    assertEquals("+-,", option.values().get(0));
  }

  public void testSecondCommaIsASeparator() throws ExException {
    option.set(",,a");
    assertEquals(",", option.values().get(0));
    assertEquals("a", option.values().get(1));
  }

  public void testSingleHyphenIsAValue() throws ExException {
    option.set("-");
    assertEquals("-", option.values().get(0));
  }

  public void testHyphenBetweenCharNumsIsARange() throws ExException {
    option.set("a-b");
    assertEquals("a-b", option.values().get(0));
  }

  public void testRangeInWhichLeftValueIsHigherThanRightValueIsInvalid() {
    option.set("b-a");
    assertDoesntContain(option.values(), new ArrayList<String>(){{add("b-a");}});
  }

  public void testAddsACharByChar() throws ExException {
    setKeyword("=-");
    assertIsKeyword('-');
  }

  public void testAddsACharByUnicodeCodePoint() throws ExException {
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
    setKeyword("+=^b-c,d");
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
