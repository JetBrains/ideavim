/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.helper

import com.maddyhome.idea.vim.api.VimSearchHelperBase.Companion.findNextWord
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.checkInString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

@Suppress("SpellCheckingInspection")
class SearchHelperTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testFindNextWord() {
    val text = "first second"
    val nextWordPosition = findNextWord(text, 0, text.length.toLong(), 1, bigWord = true, spaceWords = false).toInt()
    assertEquals(nextWordPosition, text.indexOf("second"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testFindSecondNextWord() {
    val text = "first second third"
    val nextWordPosition = findNextWord(text, 0, text.length.toLong(), 2, bigWord = true, false).toInt()
    assertEquals(nextWordPosition, text.indexOf("third"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testFindAfterLastWord() {
    val text = "first second"
    val nextWordPosition = findNextWord(text, 0, text.length.toLong(), 3, bigWord = true, false).toInt()
    assertEquals(nextWordPosition, text.length)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testFindPreviousWord() {
    val text = "first second"
    val previousWordPosition =
      findNextWord(text, text.indexOf("second").toLong(), text.length.toLong(), -1, bigWord = true, false).toInt()
    assertEquals(previousWordPosition, text.indexOf("first"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testFindSecondPreviousWord() {
    val text = "first second third"
    val previousWordPosition =
      findNextWord(
        text,
        text.indexOf("third").toLong(),
        text.length.toLong(),
        -2,
        bigWord = true,
        spaceWords = false
      ).toInt()
    assertEquals(previousWordPosition, text.indexOf("first"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testFindBeforeFirstWord() {
    val text = "first second"
    val previousWordPosition =
      findNextWord(
        text,
        text.indexOf("second").toLong(),
        text.length.toLong(),
        -3,
        bigWord = true,
        spaceWords = false
      ).toInt()
    assertEquals(previousWordPosition, text.indexOf("first"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testFindPreviousWordWhenCursorOutOfBound() {
    val text = "first second"
    val previousWordPosition =
      findNextWord(text, text.length.toLong(), text.length.toLong(), -1, bigWord = true, spaceWords = false).toInt()
    assertEquals(previousWordPosition, text.indexOf("second"))
  }

  fun testMotionOuterWordAction() {
    doTest(
      "va(", "((int) nu<caret>m)", "<selection>((int) num)</selection>", VimStateMachine.Mode.VISUAL,
      VimStateMachine.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringInsideDoubleQuotes() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 5, true)
    assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringWithoutClosingDoubleQuote() {
    val text = "abcdef\"ghi"
    val inString = checkInString(text, 5, true)
    assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringOnUnpairedSingleQuote() {
    val text = "abc\"d'ef\"ghi"
    val inString = checkInString(text, 5, true)
    assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringOutsideOfDoubleQuotesPair() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 2, true)
    assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringEscapedDoubleQuote() {
    val text = "abc\\\"def\"ghi"
    val inString = checkInString(text, 5, true)
    assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringOddNumberOfDoubleQuotes() {
    val text = "abc\"def\"gh\"i"
    val inString = checkInString(text, 5, true)
    assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringInsideSingleQuotesPair() {
    val text = "abc\"d'e'f\"ghi"
    val inString = checkInString(text, 6, false)
    assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringOnOpeningDoubleQuote() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 3, true)
    assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringOnClosingDoubleQuote() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 7, true)
    assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringWithoutQuotes() {
    val text = "abcdefghi"
    val inString = checkInString(text, 5, true)
    assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringDoubleQuoteInsideSingleQuotes() {
    val text = "abc'\"'ef\"ghi"
    val inString = checkInString(text, 5, true)
    assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringSingleQuotesAreTooFarFromEachOtherToMakePair() {
    val text = "abc'\"de'f\"ghi"
    val inString = checkInString(text, 5, true)
    assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringDoubleQuoteInsideSingleQuotesIsInsideSingleQuotedString() {
    val text = "abc'\"'def\"ghi"
    val inString = checkInString(text, 4, false)
    assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringAfterClosingDoubleQuote() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 9, true)
    assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringOnMiddleDoubleQuote() {
    val text = "abc\"def\"gh\"i"
    val inString = checkInString(text, 7, true)
    assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun testCheckInStringBetweenPairs() {
    val text = "abc\"def\"gh\"ij\"k"
    val inString = checkInString(text, 8, true)
    assertFalse(inString)
  }
}
