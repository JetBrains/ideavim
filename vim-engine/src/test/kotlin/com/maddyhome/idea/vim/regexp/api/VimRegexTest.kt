/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.api

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.mockEditorFromText
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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

    private fun doTest(text: CharSequence, pattern: String, expectedResult : Boolean) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.containsMatchIn(editor)
      assertEquals(expectedResult, matchResult)
    }
  }

  @Nested
  inner class FindTest {
    @Test
    fun `test find single word starting at beginning`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        TextRange(0, 5)
      )
    }

    @Test
    fun `test find single word starting from offset`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        TextRange(13, 18),
        1
      )
    }

    private fun doTest(text: CharSequence, pattern: String, expectedResult: TextRange, startIndex: Int = 0) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.find(editor, startIndex)
      when (matchResult) {
        is VimMatchResult.Failure -> fail("Expected to find match")
        is VimMatchResult.Success -> assertEquals(expectedResult, matchResult.range)
      }
    }
  }

  @Nested
  inner class FindAllTest {
    @Test
    fun `test find all occurrences of word`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        setOf(TextRange(0, 5), TextRange(13, 18))
      )
    }

    @Test
    fun `test find all occurrences of word from offset`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        setOf(TextRange(13, 18)),
        10
      )
    }

    @Test
    fun `test find all occurrences of word case insensitive`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "lorem\\c",
        setOf(TextRange(0, 5), TextRange(13, 18))
      )
    }

    private fun doTest(text: CharSequence, pattern: String, expectedResult: Set<TextRange>, startIndex: Int = 0) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResults = regex.findAll(editor, startIndex)
      assertEquals(expectedResult, matchResults
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
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        13,
        TextRange(13, 18)
      )
    }

    @Test
    fun `test word does not match at index`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        12,
        null
      )
    }

    private fun doTest(text: CharSequence, pattern: String, index: Int, expectedResult: TextRange? = null) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.matchAt(editor, index)
      when (matchResult) {
        is VimMatchResult.Success -> assertEquals(expectedResult, matchResult.range)
        is VimMatchResult.Failure -> assertEquals(expectedResult, null)
      }
    }
  }

  @Nested
  inner class MatchEntireTest {
    @Test
    fun `test pattern matches entire editor`() {
      val text =
        """
      	|Lorem Ipsum
      	|
      	|Lorem ipsum dolor sit amet,
      	|consectetur adipiscing elit
      	|Sed in orci mauris.
      	|Cras id tellus in ex imperdiet egestas
      """.trimMargin()

      doTest(
        text,
        "\\_.*",
        TextRange(0 , text.length)
      )
    }

    @Test
    fun `test pattern matches string only partially`() {
      doTest(
        """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
        "Lorem",
        null
      )
    }

    private fun doTest(text: CharSequence, pattern: String, expectedResult: TextRange? = null) {
      val editor = mockEditorFromText(text)
      val regex = VimRegex(pattern)
      val matchResult = regex.matchEntire(editor)
      when (matchResult) {
        is VimMatchResult.Success -> assertEquals(expectedResult, matchResult.range)
        is VimMatchResult.Failure -> assertEquals(expectedResult, null)
      }
    }
  }
}