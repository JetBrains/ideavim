/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.vimscope

import com.intellij.vim.api.VimApi
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.thinapi.VimApiImpl
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextSearch : VimTestCase() {
  private lateinit var myVimApi: VimApi

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    myVimApi = VimApiImpl(listenerOwner, mappingOwner)

    configureByText("\n")
  }

  private val camelCaseText = "thisIsCamelCase withSomeWords andMoreWords"
  private val snakeCaseText = "this_is_snake_case with_some_words and_more_words"
  private val emptyText = ""

  @Test
  fun `test matches with simple pattern`() {
    assertTrue(myVimApi.matches("test", "This is a test string", false))
    assertFalse(myVimApi.matches("missing", "This is a test string", false))
  }

  @Test
  fun `test matches with case sensitivity`() {
    assertFalse(myVimApi.matches("TEST", "This is a test string", false))
    assertTrue(myVimApi.matches("TEST", "This is a TEST string", false))
    assertTrue(myVimApi.matches("TEST", "This is a test string", true))
  }

  @Test
  fun `test matches with regex pattern`() {
    assertTrue(myVimApi.matches("t..t", "This is a test string", false))
    assertTrue(myVimApi.matches("\\w\\+", "word", false))
    assertFalse(myVimApi.matches("^test$", "This is a test", false))
    assertTrue(myVimApi.matches("^test$", "test", false))
  }

  @Test
  fun `test matches with empty text`() {
    assertFalse(myVimApi.matches("pattern", "", false))
  }

  @Test
  fun `test matches with empty pattern`() {
    // invalid pattern - ""
    val exception = assertThrows<VimRegexException> {
      myVimApi.matches("", "", false)
    }
    assert(exception.message.contains("E383"))
  }

  @Test
  fun `test getAllMatches with simple pattern`() {
    val text = "one two one three one four"
    val matches = myVimApi.getAllMatches(text, "one")

    val expectedBoundaries = mapOf(
      0 to 3, 8 to 11, 18 to 21
    )
    assert(matches.size == 3)

    for ((start, end) in matches) {
      assertTrue(expectedBoundaries.containsKey(start))
      assertTrue(expectedBoundaries[start] == end)
    }
  }

  @Test
  fun `test getAllMatches with regex pattern`() {
    val text = "one 123 two 456 three 789"

    val matches = myVimApi.getAllMatches(text, "\\d\\+")

    val expectedBoundaries = mapOf(
      4 to 7, 12 to 15, 22 to 25
    )
    assert(matches.size == 3)

    for ((start, end) in matches) {
      assertTrue(expectedBoundaries.containsKey(start))
      assertTrue(expectedBoundaries[start] == end)
    }
  }

  @Test
  fun `test getAllMatches with no matches`() {
    val text = "one two three"
    val matches = myVimApi.getAllMatches(text, "four")

    assert(matches.isEmpty())
  }

  @Test
  fun `test getAllMatches with empty text`() {
    val matches = myVimApi.getAllMatches("", "pattern")
    assert(matches.isEmpty())
  }

  @Test
  fun `test getAllMatches with empty pattern`() {
    val text = "some text"

    assertThrows<VimRegexException> {
      myVimApi.getAllMatches(text, "")
    }
  }

  @Test
  fun `test getAllMatches with overlapping patterns`() {
    val text = "abababa"
    val matches = myVimApi.getAllMatches(text, "aba")

    val expectedBoundaries = mapOf(
      0 to 3, 4 to 7
    )
    assert(matches.size == 2)

    for ((start, end) in matches) {
      assertTrue(expectedBoundaries.containsKey(start))
      assertTrue(expectedBoundaries[start] == end)
    }
  }

  @Test
  fun `test getNextCamelStartOffset in camelCase text`() {
    val result = myVimApi.getNextCamelStartOffset(camelCaseText, 1)
    assertNotNull(result)

    val result2 = myVimApi.getNextCamelStartOffset(camelCaseText, result + 1)
    result2?.let {
      assert(it > result) { "Expected second boundary to be after first boundary" }
    }
  }

  @Test
  fun `test getNextCamelStartOffset with count parameter`() {
    val result = myVimApi.getNextCamelStartOffset(camelCaseText, 0, 3)
    assert(result == 6)
  }

  @Test
  fun `test getNextCamelStartOffset in snake_case text`() {
    val result = myVimApi.getNextCamelStartOffset(snakeCaseText, 0, 2)
    assert(result == 5)
  }

  @Test
  fun `test getNextCamelStartOffset at end of text`() {
    val result = myVimApi.getNextCamelStartOffset(camelCaseText, camelCaseText.length - 1)
    assert(result == null)
  }

  @Test
  fun `test getNextCamelStartOffset in empty text`() {
    val result = myVimApi.getNextCamelStartOffset(emptyText, 0)
    assert(result == null)
  }

  @Test
  fun `test getPreviousCamelStartOffset in camelCase text`() {
    val result = myVimApi.getPreviousCamelStartOffset(camelCaseText, 10)
    assert(result == 6)

    val result2 = myVimApi.getPreviousCamelStartOffset(camelCaseText, 6)
    assert(result2 == 4)
  }

  @Test
  fun `test getPreviousCamelStartOffset with count parameter`() {
    val result = myVimApi.getPreviousCamelStartOffset(camelCaseText, 10, 3)
    assert(result == 0)
  }

  @Test
  fun `test getPreviousCamelStartOffset in snake_case text`() {
    val result = myVimApi.getPreviousCamelStartOffset(snakeCaseText, 10)
    assert(result == 8)
  }

  @Test
  fun `test getPreviousCamelStartOffset at start of text`() {
    val result = myVimApi.getPreviousCamelStartOffset(camelCaseText, 0)
    assert(result == null)
  }

  @Test
  fun `test getNextCamelEndOffset in camelCase text`() {
    val result = myVimApi.getNextCamelEndOffset(camelCaseText, 0)
    assert(result == 3)

    val result2 = myVimApi.getNextCamelEndOffset(camelCaseText, 4)
    assert(result2 == 5)
  }

  @Test
  fun `test getNextCamelEndOffset with count parameter`() {
    for (count in 1..3) {
      val result = myVimApi.getNextCamelEndOffset(camelCaseText, 0, count)

      assertNotNull(result) { "Expected to find a camel case boundary with count=$count" }

      assert(result > 0 && result < camelCaseText.length) { "Boundary should be within text bounds" }
    }
  }

  @Test
  fun `test getNextCamelEndOffset in snake_case text`() {
    val result = myVimApi.getNextCamelEndOffset(snakeCaseText, 0)
    assertNotNull(result) { "Expected to find a camel/snake case boundary" }

    assert(result > 0 && result < snakeCaseText.length) { "Boundary should be within text bounds" }
  }

  @Test
  fun `test getPreviousCamelEndOffset in camelCase text`() {
    val result = myVimApi.getPreviousCamelEndOffset(camelCaseText, 10)
    assert(result == 5)

    val result2 = myVimApi.getPreviousCamelEndOffset(camelCaseText, 5)
    assert(result2 == 3)
  }

  @Test
  fun `test getPreviousCamelEndOffset with count parameter`() {
    val result = myVimApi.getPreviousCamelEndOffset(camelCaseText, 15, 3)
    assert(result == 5)
  }

  @Test
  fun `test getPreviousCamelEndOffset in snake_case text`() {
    val result = myVimApi.getPreviousCamelEndOffset(snakeCaseText, 10)
    assert(result == 6)
  }
}
