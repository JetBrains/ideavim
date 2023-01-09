/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option;

import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.helper.CharacterHelper;
import com.maddyhome.idea.vim.options.OptionConstants;
import com.maddyhome.idea.vim.options.OptionScope;
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.ArrayList;
import java.util.List;

public class KeywordOptionTest extends VimTestCase {

  private List<String> getValues() {
    return KeywordOptionHelper.INSTANCE.parseValues(getOptionValue());
  }

  private String getOptionValue() {
    return ((VimString)VimPlugin.getOptionService()
      .getOptionValue(OptionScope.GLOBAL.INSTANCE, OptionConstants.iskeyword, OptionConstants.iskeyword)).getValue();
  }

  private void setKeyword(String val) {
    VimPlugin.getOptionService()
      .setOptionValue(OptionScope.GLOBAL.INSTANCE, OptionConstants.iskeyword, new VimString(val), "testToken");
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
    VimInjectorKt.getInjector().getVimscriptExecutor().execute("set iskeyword^=,", false);
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
    VimInjectorKt.getInjector().getVimscriptExecutor().execute("set iskeyword+=^a", true);
    assertIsNotKeyword('a');
  }

  public void testCaretRemovesARange() {
    setKeyword("a-c");
    VimInjectorKt.getInjector().getVimscriptExecutor().execute("set iskeyword+=^b-c,d", true);
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
