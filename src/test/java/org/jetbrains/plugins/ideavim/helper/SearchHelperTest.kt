/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.helper

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.checkInString
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class SearchHelperTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindNextWord() {
    val text = "first second"
    configureByText(text)
    val nextWordPosition =
      injector.searchHelper.findNextWord(fixture.editor.vim, 0, 1, bigWord = true, spaceWords = false)
    kotlin.test.assertEquals(nextWordPosition, text.indexOf("second"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindSecondNextWord() {
    val text = "first second third"
    configureByText(text)
    val nextWordPosition = injector.searchHelper.findNextWord(fixture.editor.vim, 0, 2, bigWord = true, false)
    kotlin.test.assertEquals(nextWordPosition, text.indexOf("third"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindAfterLastWord() {
    val text = "first second"
    configureByText(text)
    val nextWordPosition = injector.searchHelper.findNextWord(fixture.editor.vim, 0, 3, bigWord = true, false)
    kotlin.test.assertEquals(nextWordPosition, text.length)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindPreviousWord() {
    val text = "first second"
    configureByText(text)
    val previousWordPosition =
      injector.searchHelper.findNextWord(fixture.editor.vim, text.indexOf("second"), -1, bigWord = true, false)
    kotlin.test.assertEquals(previousWordPosition, text.indexOf("first"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindSecondPreviousWord() {
    val text = "first second third"
    configureByText(text)
    val previousWordPosition =
      injector.searchHelper.findNextWord(
        fixture.editor.vim,
        text.indexOf("third"),
        -2,
        bigWord = true,
        spaceWords = false,
      )
    kotlin.test.assertEquals(previousWordPosition, text.indexOf("first"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindBeforeFirstWord() {
    val text = "first second"
    configureByText(text)
    val previousWordPosition =
      injector.searchHelper.findNextWord(
        fixture.editor.vim,
        text.indexOf("second"),
        -3,
        bigWord = true,
        spaceWords = false,
      )
    kotlin.test.assertEquals(previousWordPosition, text.indexOf("first"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindPreviousWordWhenCursorOutOfBound() {
    val text = "first second"
    configureByText(text)
    val previousWordPosition =
      injector.searchHelper.findNextWord(fixture.editor.vim, text.length, -1, bigWord = true, spaceWords = false)
    kotlin.test.assertEquals(previousWordPosition, text.indexOf("second"))
  }

  @Test
  fun testMotionOuterWordAction() {
    doTest(
      "va(",
      "((int) nu<caret>m)",
      "<selection>((int) num)</selection>",
      VimStateMachine.Mode.VISUAL,
      VimStateMachine.SubMode.VISUAL_CHARACTER,
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringInsideDoubleQuotes() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 5, true)
    kotlin.test.assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringWithoutClosingDoubleQuote() {
    val text = "abcdef\"ghi"
    val inString = checkInString(text, 5, true)
    kotlin.test.assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringOnUnpairedSingleQuote() {
    val text = "abc\"d'ef\"ghi"
    val inString = checkInString(text, 5, true)
    kotlin.test.assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringOutsideOfDoubleQuotesPair() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 2, true)
    kotlin.test.assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringEscapedDoubleQuote() {
    val text = "abc\\\"def\"ghi"
    val inString = checkInString(text, 5, true)
    kotlin.test.assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringOddNumberOfDoubleQuotes() {
    val text = "abc\"def\"gh\"i"
    val inString = checkInString(text, 5, true)
    kotlin.test.assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringInsideSingleQuotesPair() {
    val text = "abc\"d'e'f\"ghi"
    val inString = checkInString(text, 6, false)
    kotlin.test.assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringOnOpeningDoubleQuote() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 3, true)
    kotlin.test.assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringOnClosingDoubleQuote() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 7, true)
    kotlin.test.assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringWithoutQuotes() {
    val text = "abcdefghi"
    val inString = checkInString(text, 5, true)
    kotlin.test.assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringDoubleQuoteInsideSingleQuotes() {
    val text = "abc'\"'ef\"ghi"
    val inString = checkInString(text, 5, true)
    kotlin.test.assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringSingleQuotesAreTooFarFromEachOtherToMakePair() {
    val text = "abc'\"de'f\"ghi"
    val inString = checkInString(text, 5, true)
    kotlin.test.assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringDoubleQuoteInsideSingleQuotesIsInsideSingleQuotedString() {
    val text = "abc'\"'def\"ghi"
    val inString = checkInString(text, 4, false)
    kotlin.test.assertTrue(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringAfterClosingDoubleQuote() {
    val text = "abc\"def\"ghi"
    val inString = checkInString(text, 9, true)
    kotlin.test.assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringOnMiddleDoubleQuote() {
    val text = "abc\"def\"gh\"i"
    val inString = checkInString(text, 7, true)
    kotlin.test.assertFalse(inString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testCheckInStringBetweenPairs() {
    val text = "abc\"def\"gh\"ij\"k"
    val inString = checkInString(text, 8, true)
    kotlin.test.assertFalse(inString)
  }
}
