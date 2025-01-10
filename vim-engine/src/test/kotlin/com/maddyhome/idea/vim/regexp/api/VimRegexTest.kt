/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.api

import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexOptions
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.END
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.START
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.getMatchRanges
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.mockEditorFromText
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.fail

class VimRegexTest {
  @Nested
  inner class ContainsMatchInTest {
    @Test
    fun `test single word contains match in editor`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "dolor",
        true
      )
    }

    @Test
    fun `test single word does not contain match in editor`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "IdeaVim",
        false
      )
    }

    private fun doTest(
      text: CharSequence,
      pattern: String, expectedResult : Boolean
    ) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.containsMatchIn(editor)
      assertEquals(expectedResult, matchResult)
    }
  }

  @Nested
  inner class FindNextTest {
    @Test
    fun `test find single word starting at beginning`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |${START}Lorem${END} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
      )
    }

    @Test
    fun `test find single word does not wrap around`() {
      // Wrapscan is handled by the caller
      assertFailure(
        """
      	|${START}Lorem${END} Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        40,
        noneOfEnum()
      )
    }

    @Test
    fun `test find single word doesn't wrap around`() {
      assertFailure(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        40
      )
    }

    @Test
    fun `test find word case insensitive prevails`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |${START}Lorem${END} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "\\ClO\\crEm\\C"
      )
    }

    private fun doTest(
      text: CharSequence,
      pattern: String,
      startIndex: Int = 0,
      options: EnumSet<VimRegexOptions> = noneOfEnum()
    ) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.findNext(editor, startIndex, options)
      when (matchResult) {
        is VimMatchResult.Failure -> fail("Expected to find match")
        is VimMatchResult.Success -> assertEquals(getMatchRanges(text).firstOrNull(), matchResult.range)
      }
    }

    private fun assertFailure(
      text: CharSequence,
      pattern: String,
      startIndex: Int = 0,
      options: EnumSet<VimRegexOptions> = noneOfEnum()
    ) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.findNext(editor, startIndex, options)
      if (matchResult is VimMatchResult.Success) fail("Expected to not find any match, but one was found at ${matchResult.range}")
    }
  }

  @Nested
  inner class FindPreviousTest {
    @Test
    fun `test find previous single word starting from offset`() {
      doTest(
        """
      	|${START}Lorem${END} Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        1
      )
    }

    @Test
    fun `test find previous single word does not wrap around`() {
      // Wrapscan is handled by caller
      assertFailure(
        """
      	|Lorem Ipsum
        |
        |${START}Lorem${END} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        options = noneOfEnum()
      )
    }

    @Test
    fun `test find previous single word doesn't warp around`() {
      assertFailure(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem"
      )
    }

    private fun doTest(
      text: CharSequence,
      pattern: String,
      startIndex: Int = 0,
      options: EnumSet<VimRegexOptions> = noneOfEnum()
    ) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.findPrevious(editor, startIndex, options)
      when (matchResult) {
        is VimMatchResult.Failure -> fail("Expected to find match")
        is VimMatchResult.Success -> assertEquals(getMatchRanges(text).firstOrNull(), matchResult.range)
      }
    }

    private fun assertFailure(
      text: CharSequence,
      pattern: String,
      startIndex: Int = 0,
      options: EnumSet<VimRegexOptions> = noneOfEnum()
    ) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.findPrevious(editor, startIndex, options)
      if (matchResult is VimMatchResult.Success) fail("Expected to not find any match, but one was found at ${matchResult.range}")
    }
  }

  @Nested
  inner class FindAllTest {
    @Test
    fun `test find all occurrences of word`() {
      doTest(
        """
      	|${START}Lorem${END} Ipsum
        |
        |${START}Lorem${END} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem"
      )
    }

    @Test
    fun `test find all occurrences of word from offset`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |${START}Lorem${END} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        10
      )
    }

    @Test
    fun `test find all occurrences of word case insensitive`() {
      doTest(
        """
      	|${START}Lorem${END} Ipsum
        |
        |${START}Lorem${END} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "lorem\\c",
      )
    }

    @Test
    fun `test find all occurrences of word with smartcase ignores case`() {
      doTest(
        """
      	|${START}Lorem Ipsum${END}
        |
        |${START}Lorem ipsum${END} dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "lorem ipsum",
        options = enumSetOf(VimRegexOptions.IGNORE_CASE, VimRegexOptions.SMART_CASE)
      )
    }

    @Test
    fun `test find all occurrences of word with smartcase doesn't ignore case`() {
      doTest(
        """
      	|${START}Lorem Ipsum${END}
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem Ipsum",
        options = enumSetOf(VimRegexOptions.IGNORE_CASE, VimRegexOptions.SMART_CASE)
      )
    }

    private fun doTest(
      text: CharSequence,
      pattern: String,
      startIndex: Int = 0,
      options: EnumSet<VimRegexOptions> = enumSetOf()
    ) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResults = regex.findAll(editor, startIndex, options=options)
      assertEquals(
        getMatchRanges(text).toSet(), matchResults
        .map { it.range }
        .toSet()
      )
    }
  }

  @Nested
  inner class MatchAtTest {
    @Test
    fun `test word matches at index`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |${START}Lorem${END} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        13,
      )
    }

    @Test
    fun `test word does not match at index`() {
      assertFailure(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        12
      )
    }

    private fun doTest(
      text: CharSequence,
      pattern: String,
      index: Int
      ) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.matchAt(editor, index)
      when (matchResult) {
        is VimMatchResult.Success -> assertEquals(getMatchRanges(text).firstOrNull(), matchResult.range)
        is VimMatchResult.Failure -> fail("Expected to find match.")
      }
    }

    private fun assertFailure(
      text: CharSequence,
      pattern: String,
      index: Int
    ) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.matchAt(editor, index)
      if (matchResult is VimMatchResult.Success)
        fail("Expected to not find any matches but instead found match at ${matchResult.range}")
    }
  }

  @Nested
  inner class MatchEntireTest {
    @Test
    fun `test pattern matches entire editor`() {
      doTest(
        """
      	|${START}Lorem Ipsum
      	|
      	|Lorem ipsum dolor sit amet,
      	|consectetur adipiscing elit
      	|Sed in orci mauris.
      	|Cras id tellus in ex imperdiet egestas.${END}
      """.trimMargin(),
        "\\_.*",
      )
    }

    @Test
    fun `test pattern matches string only partially`() {
      assertFailure(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
      )
    }

    private fun doTest(
      text: CharSequence,
      pattern: String) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.matchEntire(editor)
      when (matchResult) {
        is VimMatchResult.Success -> assertEquals(getMatchRanges(text).firstOrNull(), matchResult.range)
        is VimMatchResult.Failure -> fail("Expected to find match.")
      }
    }

    private fun assertFailure(
      text: CharSequence,
      pattern: String
    ) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.matchEntire(editor)
      if (matchResult is VimMatchResult.Success)
        fail("Expected to not find any matches but instead found match at ${matchResult.range}")
    }
  }
}
