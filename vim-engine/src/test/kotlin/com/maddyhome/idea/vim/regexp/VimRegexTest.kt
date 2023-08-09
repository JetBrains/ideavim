/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
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
      0 until 5
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
      13 until 18,
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
      setOf(0 until 5, 13 until 18)
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
      setOf(13 until 18),
      10
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
      13 until 18
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

  private fun assertContainsMatchIn(text: CharSequence, pattern: String, expectedResult : Boolean) {
    val editor = buildEditor(text)
    val regex = VimRegex(pattern)
    val matchResult = regex.containsMatchIn(editor)
    assertEquals(expectedResult, matchResult)
  }

  private fun assertFind(text: CharSequence, pattern: String, expectedResult: IntRange, startIndex: Int = 0) {
    val editor = buildEditor(text)
    val regex = VimRegex(pattern)
    val matchResult = regex.find(editor, startIndex)
    when (matchResult) {
      is VimMatchResult.Failure -> fail("Expected to find match")
      is VimMatchResult.Success -> assertEquals(expectedResult, matchResult.range)
    }
  }

  private fun assertFindAll(text: CharSequence, pattern: String, expectedResult: Set<IntRange>, startIndex: Int = 0) {
    val editor = buildEditor(text)
    val regex = VimRegex(pattern)
    val matchResults = regex.findAll(editor, startIndex)
    assertEquals(expectedResult, matchResults
      .map { it.range }
      .toSet()
    )
  }

  private fun assertMatchAt(text: CharSequence, pattern: String, index: Int, expectedResult: IntRange? = null) {
    val editor = buildEditor(text)
    val regex = VimRegex(pattern)
    val matchResult = regex.matchAt(editor, index)
    when (matchResult) {
      is VimMatchResult.Success -> assertEquals(expectedResult, matchResult.range)
      is VimMatchResult.Failure -> assertEquals(expectedResult, null)
    }
  }

  private fun buildEditor(text: CharSequence, carets: List<Int> = emptyList()) : VimEditor {
    val editorMock = Mockito.mock<VimEditor>()
    whenever(editorMock.text()).thenReturn(text)

    val trueCarets = ArrayList<VimCaret>()
    for (caret in carets) {
      val caretMock = Mockito.mock<VimCaret>()
      whenever(caretMock.offset).thenReturn(Offset(caret))
      trueCarets.add(caretMock)
    }
    whenever(editorMock.carets()).thenReturn(trueCarets)
    return editorMock
  }
}