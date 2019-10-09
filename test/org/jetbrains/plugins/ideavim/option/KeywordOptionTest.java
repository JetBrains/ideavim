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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.option;

import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.helper.CharacterHelper;
import com.maddyhome.idea.vim.option.KeywordOption;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.ArrayList;
import java.util.List;

public class KeywordOptionTest extends VimTestCase {

  private KeywordOption option;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    option = OptionsManager.INSTANCE.getIskeyword();
  }

  private void setKeyword(String val) {
    OptionsManager.INSTANCE.parseOptionLine(myFixture.getEditor(), "iskeyword" + val, false);
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

  public void testTwoAdjacentLettersAreInvalid() {
    option.set("ab");
    List<String> v = option.values();
    assertDoesntContain(option.values(), new ArrayList<String>(){{add("ab");}});
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

  public void testToRegex() {
    setKeyword("=-,a-c");
    final List<String> res = option.toRegex();
    assertEquals(2, res.size());
    assertTrue(res.contains("-"));
    assertTrue(res.contains("[a-c]"));
  }

  public void testAllLettersToRegex() {
    setKeyword("=@");
    final List<String> res = option.toRegex();
    assertEquals(res.get(0), "\\p{L}");
  }
}
