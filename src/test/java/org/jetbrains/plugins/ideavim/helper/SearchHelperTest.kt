/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.helper

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.findBlockRange
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("SpellCheckingInspection")
class SearchHelperTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindNextWord() {
    val text = "first second"
    configureByText(text)
    val nextWordPosition = injector.searchHelper.findNextWord(fixture.editor.vim, 0, 1, bigWord = true)
    kotlin.test.assertEquals(nextWordPosition, text.indexOf("second"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindSecondNextWord() {
    val text = "first second third"
    configureByText(text)
    val nextWordPosition = injector.searchHelper.findNextWord(fixture.editor.vim, 0, 2, bigWord = true)
    kotlin.test.assertEquals(nextWordPosition, text.indexOf("third"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindAfterLastWord() {
    val text = "first second"
    configureByText(text)
    val nextWordPosition = injector.searchHelper.findNextWord(fixture.editor.vim, 0, 3, bigWord = true)
    kotlin.test.assertEquals(nextWordPosition, text.length)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindPreviousWord() {
    val text = "first second"
    configureByText(text)
    val previousWordPosition =
      injector.searchHelper.findNextWord(fixture.editor.vim, text.indexOf("second"), -1, bigWord = true)
    kotlin.test.assertEquals(previousWordPosition, text.indexOf("first"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindSecondPreviousWord() {
    val text = "first second third"
    configureByText(text)
    val previousWordPosition =
      injector.searchHelper.findNextWord(fixture.editor.vim, text.indexOf("third"), -2, bigWord = true)
    kotlin.test.assertEquals(previousWordPosition, text.indexOf("first"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindBeforeFirstWord() {
    val text = "first second"
    configureByText(text)
    val previousWordPosition =
      injector.searchHelper.findNextWord(fixture.editor.vim, text.indexOf("second"), -3, bigWord = true)
    kotlin.test.assertEquals(previousWordPosition, text.indexOf("first"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindPreviousWordWhenCursorOutOfBound() {
    val text = "first second"
    configureByText(text)
    val previousWordPosition = injector.searchHelper.findNextWord(fixture.editor.vim, text.length, -1, bigWord = true)
    kotlin.test.assertEquals(previousWordPosition, text.indexOf("second"))
  }


  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun testFindAllIgnoreCaseOverwritesSmartCase() {
    val text = "Lorem ipsum lorem ipsum"
    configureByText(text)

    val capitalMatch = TextRange(0, 5)
    val lowerMatch = TextRange(12, 17)

    val search = { ignoreCase: Boolean ->
      injector.searchHelper.findAll(fixture.editor.vim, "\\<Lorem\\>", 0, -1, ignoreCase)
    }

    // Ensure ignore and smart case are off
    enterCommand("set noignorecase")
    enterCommand("set nosmartcase")

    kotlin.test.assertEquals(listOf(capitalMatch), search(false))
    // Even if both are off, ignore case should still ignore cases
    kotlin.test.assertEquals(listOf(capitalMatch, lowerMatch), search(true))

    enterCommand("set smartcase")
    kotlin.test.assertEquals(listOf(capitalMatch), search(false))
    kotlin.test.assertEquals(listOf(capitalMatch, lowerMatch), search(true))
  }

  @Test
  fun testMotionOuterWordAction() {
    doTest(
      "va(",
      "((int) nu<caret>m)",
      "<selection>((int) num)</selection>",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @ParameterizedTest
  @MethodSource("findBlockRangeTestCases")
  fun findBlockRange(testCase: FindBlockRangeTestCase) {
    val (_, text, type, count, isOuter, expected) = (testCase)
    configureByText(text)
    ApplicationManager.getApplication().runReadAction {
      val actual = findBlockRange(fixture.editor.vim, fixture.editor.vim.currentCaret(), type, count, isOuter)
      kotlin.test.assertEquals(expected, actual)
    }
  }

  companion object {
    data class FindBlockRangeTestCase(
      val description: String,
      val text: String,
      val type: Char,
      val count: Int,
      val isOuter: Boolean,
      val expected: TextRange?,
    )

    @JvmStatic
    fun findBlockRangeTestCases(): Stream<FindBlockRangeTestCase> {
      return Stream.of(
        FindBlockRangeTestCase("no inner match when range selection", "${s}aa${se}", '(', 1, isOuter = false, expected = null),
        FindBlockRangeTestCase("no inner match when no range selection", "${s}a${se}", '(', 1, isOuter = false, expected = null),
        FindBlockRangeTestCase("inner match without range selection", "(${c}a)", '(', 1, isOuter = false, expected = TextRange(1, 2)),
        FindBlockRangeTestCase("outer match without range selection", "(${c}a)", '(', 1, isOuter = true, expected = TextRange(0, 3)),
        FindBlockRangeTestCase("inner match with range selection", "(${c}${s}a${se})", '(', 1, isOuter = false, expected = TextRange(1, 2)),
        FindBlockRangeTestCase("outer match with range selection", "(${c}${s}a${se})", '(', 1, isOuter = true, expected = TextRange(0, 3)),
        FindBlockRangeTestCase("caret at begin inner match forward", "(a)${c}(a)", '(', 1, isOuter = false, expected = TextRange(4, 5)),
        FindBlockRangeTestCase("caret at begin outer match forward", "(a)${c}(a)", '(', 1, isOuter = true, expected = TextRange(3, 6)),
        FindBlockRangeTestCase("caret at end inner match backward", "(a${c})(a)", '(', 1, isOuter = false, expected = TextRange(1, 2)),
        FindBlockRangeTestCase("caret at end outer match backward", "(a${c})(a)", '(', 1, isOuter = true, expected = TextRange(0, 3)),
        FindBlockRangeTestCase("inner match first paren", "( (${c}a) )", '(', 1, isOuter = false, expected = TextRange(3, 4)),
        FindBlockRangeTestCase("outer match first paren", "( (${c}a) )", '(', 1, isOuter = true, expected = TextRange(2, 5)),
        FindBlockRangeTestCase("caret at begin inner match first forward", "${c}( (a) )", '(', 1, isOuter = false, expected = TextRange(1, 6)),
        FindBlockRangeTestCase("caret at begin outer match first forward", "${c}( (a) )", '(', 1, isOuter = true, expected = TextRange(0, 7)),
        FindBlockRangeTestCase("inner match multiple occurence", "( (${c}a) )", '(', 2, isOuter = false, expected = TextRange(1, 6)),
        FindBlockRangeTestCase("outer match multiple occurence", "( (${c}a) )", '(', 2, isOuter = true, expected = TextRange(0, 7)),
        FindBlockRangeTestCase("expand inner selection to first occurence", "(${c}${s}a${se}a)", '(', 1, isOuter = false, expected = TextRange(1, 3)),
        FindBlockRangeTestCase("expand outer selection to first occurence", "(${c}${s}a${se}a)", '(', 1, isOuter = true, expected = TextRange(0, 4)),
        FindBlockRangeTestCase("expand inner selection to next occurence", "( (${c}${s}aa${se}) )", '(', 1, isOuter = false, expected = TextRange(1, 7)),
        FindBlockRangeTestCase("expand outer selection to next occurence", "( ${c}${s}(aa)${se} )", '(', 1, isOuter = true, expected = TextRange(0, 8)),
        FindBlockRangeTestCase("paren not in string inner match", "( (\"${c}a\") )", '(', 1, isOuter = false, expected = TextRange(3, 6)),
        FindBlockRangeTestCase("paren not in string outer match", "( (\"${c}a\") )", '(', 1, isOuter = true, expected = TextRange(2, 7)),
        FindBlockRangeTestCase("start end paren in string inner match", "( \"(a${c}a)\" )", '(', 1, isOuter = false, expected = TextRange(4, 6)),
        FindBlockRangeTestCase("start end paren in string outer match", "( \"(a${c}a)\" )", '(', 1, isOuter = true, expected = TextRange(3, 7)),
        FindBlockRangeTestCase("only start paren in string inner match not match paren outside", "\"(a${c}a\")", '(', 1, isOuter = false, expected = null),
        FindBlockRangeTestCase("only start paren in string outer match not match paren outside", "(\"a${c}a)\"", '(', 1, isOuter = true, expected = null),
        FindBlockRangeTestCase("inner match exclude start paren in string when caret at start of quote", "(${c}\"(aa\")", '(', 1, isOuter = false, expected = TextRange(1, 6)),
        FindBlockRangeTestCase("outer match exclude start paren in string when caret at start of quote", "(${c}\"(aa\")", '(', 1, isOuter = true, expected = TextRange(0, 7)),
        FindBlockRangeTestCase("inner match exclude start paren in string when caret at end of quote", "(\"(aa${c}\")", '(', 1, isOuter = false, expected = TextRange(1, 6)),
        FindBlockRangeTestCase("outer match exclude start paren in string when caret at end of quote", "(\"(aa${c}\")", '(', 1, isOuter = true, expected = TextRange(0, 7)),
        FindBlockRangeTestCase("inner match not exclude start paren in string when caret in between quote", "(\"(a${c}a\")", '(', 1, isOuter = false, expected = TextRange(1, 6)), // Vim behavior differs, but we have some PSI magic and can resolve such cases
        FindBlockRangeTestCase("outer match not exclude start paren in string when caret in between quote", "(\"(a${c}a\")", '(', 1, isOuter = true, expected = TextRange(0, 7)), // Vim behavior differs, but we have some PSI magic and can resolve such cases
        FindBlockRangeTestCase("inner match exclude end paren in string when caret at start of quote", "(${c}\"aa)\")", '(', 1, isOuter = false, expected = TextRange(1, 6)),
        FindBlockRangeTestCase("outer match exclude end paren in string when caret at start of quote", "(${c}\"aa)\")", '(', 1, isOuter = true, expected = TextRange(0, 7)),
        FindBlockRangeTestCase("inner match exclude end paren in string when caret at end of quote", "(\"aa)${c}\")", '(', 1, isOuter = false, expected = TextRange(1, 6)),
        FindBlockRangeTestCase("outer match exclude end paren in string when caret at end of quote", "(\"aa)${c}\")", '(', 1, isOuter = true, expected = TextRange(0, 7)),
        FindBlockRangeTestCase("inner match exclude end paren in string when caret in between quote", "(\"a${c}a)\")", '(', 1, isOuter = false, expected = TextRange(1, 6)),
        FindBlockRangeTestCase("outer match exclude end paren in string when caret in between quote", "(\"a${c}a)\")", '(', 1, isOuter = true, expected = TextRange(0, 7)),
        FindBlockRangeTestCase("inner match exclude first line break after start paren", "(\n${c}a)", '(', 1, isOuter = false, expected = TextRange(2, 3)),
        FindBlockRangeTestCase("inner match exclude only first line break after start paren", "(\n\n${c}a)", '(', 1, isOuter = false, expected = TextRange(2, 4)),
        FindBlockRangeTestCase("inner match exclude last line break before end paren", "(${c}a\n)", '(', 1, isOuter = false, expected = TextRange(1, 2)),
        FindBlockRangeTestCase("inner match exclude only last line break before end paren", "(${c}a\n\n)", '(', 1, isOuter = false, expected = TextRange(1, 3)),
        FindBlockRangeTestCase("inner match exclude last line break and whitespace character before end paren", "(${c}a\n   )", '(', 1, isOuter = false, expected = TextRange(1, 2)),
        FindBlockRangeTestCase("inner match expand to next occurence when start end paren has line break", "(a(\n${c}${s}a${se}\n)c)", '(', 1, isOuter = false, expected = TextRange(1, 8)),
        FindBlockRangeTestCase("inner match expand to next occurence when end paren has line break and blank character", "(a(\n${c}${s}a${se}\n )c)", '(', 1, isOuter = false, expected = TextRange(1, 9)),
        FindBlockRangeTestCase("inner match not expand to next occurence when selection not last line", "(a(\n${c}${s}a${se}\n\n )c)", '(', 1, isOuter = false, expected = TextRange(4, 6)),
      )
    }
  }
}
