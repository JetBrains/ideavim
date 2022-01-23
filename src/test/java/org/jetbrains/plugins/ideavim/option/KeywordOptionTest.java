/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.CharacterHelper;
import com.maddyhome.idea.vim.vimscript.Executor;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString;
import com.maddyhome.idea.vim.vimscript.model.options.helpers.KeywordOptionHelper;
import com.maddyhome.idea.vim.vimscript.services.OptionConstants;
import com.maddyhome.idea.vim.vimscript.services.OptionService;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.ArrayList;
import java.util.List;

public class KeywordOptionTest extends VimTestCase {

  private List<String> getValues() {
    return KeywordOptionHelper.INSTANCE.parseValues(getOptionValue());
  }

  private String getOptionValue() {
    return ((VimString)VimPlugin.getOptionService()
      .getOptionValue(OptionService.Scope.GLOBAL.INSTANCE, OptionConstants.iskeywordName, OptionConstants.iskeywordName)).getValue();
  }

  private void setKeyword(String val) {
    VimPlugin.getOptionService()
      .setOptionValue(OptionService.Scope.GLOBAL.INSTANCE, OptionConstants.iskeywordName, new VimString(val), "testToken");
  }

  private void assertIsKeyword(char c) {
    CharacterHelper.CharacterType charType = CharacterHelper.charType(c, false);
    assertSame(CharacterHelper.CharacterType.KEYWORD, charType);
  }

  private void assertIsNotKeyword(char c) {
    CharacterHelper.CharacterType charType = CharacterHelper.charType(c, false);
    assertSame(CharacterHelper.CharacterType.PUNCTUATION, charType);
  }

  public void testSingleCommaIsAValue() {
    setKeyword(",");
    assertEquals(",", getValues().get(0));
  }

  public void testSingleCommaIsAValueAsAppend() {
    Executor.INSTANCE.execute("set iskeyword^=,", false);
    assertTrue(getValues().contains(","));
  }

  public void testSingleNegatedCommaIsAValue() {
    setKeyword("^,");
    assertEquals("^,", getValues().get(0));
  }

  public void testCommaInARangeIsAValue() {
    setKeyword("+-,");
    assertEquals("+-,", getValues().get(0));
  }

  public void testSecondCommaIsASeparator() {
    setKeyword(",,a");
    assertEquals(",", getValues().get(0));
    assertEquals("a", getValues().get(1));
  }

  public void testSingleHyphenIsAValue() {
    setKeyword("-");
    assertEquals("-", getValues().get(0));
  }

  public void testHyphenBetweenCharNumsIsARange() {
    setKeyword("a-b");
    assertEquals("a-b", getValues().get(0));
  }

  public void testRangeInWhichLeftValueIsHigherThanRightValueIsInvalid() {
    try {
      setKeyword("b-a");
      fail("exception missing");
    } catch (Exception e) {
      assertEquals("E474: Invalid argument: testToken", e.getMessage());
    }
    assertDoesntContain(getValues(), new ArrayList<>() {{
      add("b-a");
    }});
  }

  public void testTwoAdjacentLettersAreInvalid() {
    try {
      setKeyword("ab");
      fail("exception missing");
    } catch (Exception e) {
      assertEquals("E474: Invalid argument: testToken", e.getMessage());
    }
    assertDoesntContain(getValues(), new ArrayList<>() {{
      add("ab");
    }});
  }

  public void testAddsACharByChar() {
    setKeyword("-");
    assertIsKeyword('-');
  }

  public void testAddsACharByUnicodeCodePoint() {
    setKeyword("" + (int)'-');
    assertIsKeyword('-');
  }

  public void testAddsARange() {
    setKeyword("a-c");
    assertIsKeyword('a');
    assertIsKeyword('b');
    assertIsKeyword('c');
  }

  public void testAtSignRepresentsAllLetters() {
    setKeyword("@");
    assertIsKeyword('A');
    assertIsKeyword('Ā');
  }

  public void testRangeOfAtSignToAtSignRepresentsAtSign() {
    setKeyword("@-@");
    assertIsKeyword('@');
  }

  public void testCaretRemovesAChar() {
    setKeyword("a");
    Executor.INSTANCE.execute("set iskeyword+=^a", true);
    assertIsNotKeyword('a');
  }

  public void testCaretRemovesARange() {
    setKeyword("a-c");
    Executor.INSTANCE.execute("set iskeyword+=^b-c,d", true);
    assertIsKeyword('a');
    assertIsNotKeyword('b');
    assertIsNotKeyword('c');
  }

  public void testCaretAloneRepresentsACaret() {
    setKeyword("^");
    assertIsKeyword('^');
  }

  public void testMultibyteCharactersAreKeywords() {
    assertIsKeyword('Ź');
  }

  public void testToRegex() {
    setKeyword("-,a-c");
    final List<String> res = KeywordOptionHelper.INSTANCE.toRegex();
    assertEquals(2, res.size());
    assertTrue(res.contains("-"));
    assertTrue(res.contains("[a-c]"));
  }

  public void testAllLettersToRegex() {
    setKeyword("@");
    final List<String> res = KeywordOptionHelper.INSTANCE.toRegex();
    assertEquals(res.get(0), "\\p{L}");
  }
}
