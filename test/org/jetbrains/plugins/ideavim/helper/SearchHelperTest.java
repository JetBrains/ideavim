/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.helper;

import com.maddyhome.idea.vim.helper.SearchHelper;
import com.maddyhome.idea.vim.helper.SearchHelperKtKt;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

public class SearchHelperTest extends VimTestCase {
  public void testFindNextWord() {
    String text = "first second";
    int nextWordPosition = SearchHelper.findNextWord(text, 0, text.length(), 1, true, false);

    assertEquals(nextWordPosition, text.indexOf("second"));
  }

  public void testFindSecondNextWord() {
    String text = "first second third";
    int nextWordPosition = SearchHelper.findNextWord(text, 0, text.length(), 2, true, false);

    assertEquals(nextWordPosition, text.indexOf("third"));
  }

  public void testFindAfterLastWord() {
    String text = "first second";
    int nextWordPosition = SearchHelper.findNextWord(text, 0, text.length(), 3, true, false);

    assertEquals(nextWordPosition, text.length());
  }

  public void testFindPreviousWord() {
    String text = "first second";
    int previousWordPosition = SearchHelper.findNextWord(text, text.indexOf("second"), text.length(), -1, true, false);

    assertEquals(previousWordPosition, text.indexOf("first"));
  }

  public void testFindSecondPreviousWord() {
    String text = "first second third";
    int previousWordPosition = SearchHelper.findNextWord(text, text.indexOf("third"), text.length(), -2, true, false);

    assertEquals(previousWordPosition, text.indexOf("first"));
  }

  public void testFindBeforeFirstWord() {
    String text = "first second";
    int previousWordPosition = SearchHelper.findNextWord(text, text.indexOf("second"), text.length(), -3, true, false);

    assertEquals(previousWordPosition, text.indexOf("first"));
  }

  public void testFindPreviousWordWhenCursorOutOfBound() {
    String text = "first second";
    int previousWordPosition = SearchHelper.findNextWord(text, text.length(), text.length(), -1, true, false);

    assertEquals(previousWordPosition, text.indexOf("second"));
  }

  public void testMotionOuterWordAction() {
    typeTextInFile(parseKeys("v", "a("), "((int) nu<caret>m)");
    myFixture.checkResult("<selection>((int) num)</selection>");
  }

  public void testCheckInStringInsideDoubleQuotes() {
    String text = "abc\"def\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 5, true);
    assertTrue(inString);
  }

  public void testCheckInStringWithoutClosingDoubleQuote() {
    String text = "abcdef\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 5, true);
    assertFalse(inString);
  }

  public void testCheckInStringOnUnpairedSingleQuote() {
    String text = "abc\"d'ef\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 5, true);
    assertTrue(inString);
  }

  public void testCheckInStringOutsideOfDoubleQuotesPair() {
    String text = "abc\"def\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 2, true);
    assertFalse(inString);
  }

  public void testCheckInStringEscapedDoubleQuote() {
    String text = "abc\\\"def\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 5, true);
    assertFalse(inString);
  }

  public void testCheckInStringOddNumberOfDoubleQuotes() {
    String text = "abc\"def\"gh\"i";
    boolean inString = SearchHelperKtKt.checkInString(text, 5, true);
    assertFalse(inString);
  }

  public void testCheckInStringInsideSingleQuotesPair() {
    String text = "abc\"d'e'f\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 6, false);
    assertTrue(inString);
  }

  public void testCheckInStringOnOpeningDoubleQuote() {
    String text = "abc\"def\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 3, true);
    assertTrue(inString);
  }

  public void testCheckInStringOnClosingDoubleQuote() {
    String text = "abc\"def\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 7, true);
    assertTrue(inString);
  }

  public void testCheckInStringWithoutQuotes() {
    String text = "abcdefghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 5, true);
    assertFalse(inString);
  }

  public void testCheckInStringDoubleQuoteInsideSingleQuotes() {
    String text = "abc'\"'ef\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 5, true);
    assertFalse(inString);
  }

  public void testCheckInStringSingleQuotesAreTooFarFromEachOtherToMakePair() {
    String text = "abc'\"de'f\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 5, true);
    assertTrue(inString);
  }

  public void testCheckInStringDoubleQuoteInsideSingleQuotesIsInsideSingleQuotedString() {
    String text = "abc'\"'def\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 4, false);
    assertTrue(inString);
  }

  public void testCheckInStringAfterClosingDoubleQuote() {
    String text = "abc\"def\"ghi";
    boolean inString = SearchHelperKtKt.checkInString(text, 9, true);
    assertFalse(inString);
  }

  public void testCheckInStringOnMiddleDoubleQuote() {
    String text = "abc\"def\"gh\"i";
    boolean inString = SearchHelperKtKt.checkInString(text, 7, true);
    assertFalse(inString);
  }

  public void testCheckInStringBetweenPairs() {
    String text = "abc\"def\"gh\"ij\"k";
    boolean inString = SearchHelperKtKt.checkInString(text, 8, true);
    assertFalse(inString);
  }
}
