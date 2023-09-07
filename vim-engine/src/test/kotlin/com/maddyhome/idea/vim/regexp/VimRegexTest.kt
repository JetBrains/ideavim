/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.mockEditorFromText
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class VimRegexTest {

  @Test
  fun `test single word contains match in editor`() {
    assertContainsMatchIn(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "dolor",
      true
    )
  }

  @Test
  fun `test single word does not contain match in editor`() {
    assertContainsMatchIn(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "IdeaVim",
      false
    )
  }

  @Test
  fun `test find single word starting at beginning`() {
    assertFind(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem",
      TextRange(0, 5)
    )
  }

  @Test
  fun `test find single word starting from offset`() {
    assertFind(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem",
      TextRange(13, 18),
      1
    )
  }

  @Test
  fun `test find all occurrences of word`() {
    assertFindAll(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem",
      setOf(TextRange(0, 5), TextRange(13, 18))
    )
  }

  @Test
  fun `test find all occurrences of word from offset`() {
    assertFindAll(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem",
      setOf(TextRange(13, 18)),
      10
    )
  }

  @Test
  fun `test find all occurrences of word case insensitive`() {
    assertFindAll(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "lorem\\c",
      setOf(TextRange(0, 5), TextRange(13, 18))
    )
  }

  @Test
  fun `test word matches at index`() {
    assertMatchAt(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem",
      13,
      TextRange(13, 18)
    )
  }

  @Test
  fun `test word does not match at index`() {
    assertMatchAt(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem",
      12,
      null
    )
  }

  @Test
  fun `test pattern matches entire editor`() {
    val text =
      "Lorem Ipsum\n" +
      "\n" +
      "Lorem ipsum dolor sit amet,\n" +
      "consectetur adipiscing elit\n" +
      "Sed in orci mauris.\n" +
      "Cras id tellus in ex imperdiet egestas."

    assertMatchEntire(
        text,
      "\\_.*",
      TextRange(0 , text.length)
    )
  }

  @Test
  fun `test pattern matches string only partially`() {
    assertMatchEntire(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem",
      null
      )
  }

  private fun assertContainsMatchIn(text: CharSequence, pattern: String, expectedResult : Boolean) {
    val editor = mockEditorFromText(text)
    val regex = VimRegex(pattern)
    val matchResult = regex.containsMatchIn(editor)
    assertEquals(expectedResult, matchResult)
  }

  private fun assertFind(text: CharSequence, pattern: String, expectedResult: TextRange, startIndex: Int = 0) {
    val editor = mockEditorFromText(text)
    val regex = VimRegex(pattern)
    val matchResult = regex.find(editor, startIndex)
    when (matchResult) {
      is VimMatchResult.Failure -> fail("Expected to find match")
      is VimMatchResult.Success -> assertEquals(expectedResult, matchResult.range)
    }
  }

  private fun assertFindAll(text: CharSequence, pattern: String, expectedResult: Set<TextRange>, startIndex: Int = 0) {
    val editor = mockEditorFromText(text)
    val regex = VimRegex(pattern)
    val matchResults = regex.findAll(editor, startIndex)
    assertEquals(expectedResult, matchResults
      .map { it.range }
      .toSet()
    )
  }

  private fun assertMatchAt(text: CharSequence, pattern: String, index: Int, expectedResult: TextRange? = null) {
    val editor = mockEditorFromText(text)
    val regex = VimRegex(pattern)
    val matchResult = regex.matchAt(editor, index)
    when (matchResult) {
      is VimMatchResult.Success -> assertEquals(expectedResult, matchResult.range)
      is VimMatchResult.Failure -> assertEquals(expectedResult, null)
    }
  }

  private fun assertMatchEntire(text: CharSequence, pattern: String, expectedResult: TextRange? = null) {
    val editor = mockEditorFromText(text)
    val regex = VimRegex(pattern)
    val matchResult = regex.matchEntire(editor)
    when (matchResult) {
      is VimMatchResult.Success -> assertEquals(expectedResult, matchResult.range)
      is VimMatchResult.Failure -> assertEquals(expectedResult, null)
    }
  }
}