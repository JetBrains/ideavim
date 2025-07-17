/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.vimscope

import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextSearch : VimTestCase() {
  private lateinit var vimScope: VimScope

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    vimScope = VimScopeImpl(listenerOwner, mappingOwner)

    configureByText("\n")
  }

  private val camelCaseText = "thisIsCamelCase withSomeWords andMoreWords"
  private val snakeCaseText = "this_is_snake_case with_some_words and_more_words"
  private val emptyText = ""

  @Test
  fun `test matches with simple pattern`() {
    assertTrue(vimScope.matches("test", "This is a test string", false))
    assertFalse(vimScope.matches("missing", "This is a test string", false))
  }

  @Test
  fun `test matches with case sensitivity`() {
    assertFalse(vimScope.matches("TEST", "This is a test string", false))
    assertTrue(vimScope.matches("TEST", "This is a TEST string", false))
    assertTrue(vimScope.matches("TEST", "This is a test string", true))
  }

  @Test
  fun `test matches with regex pattern`() {
    assertTrue(vimScope.matches("t..t", "This is a test string", false))
    assertTrue(vimScope.matches("\\w\\+", "word", false))
    assertFalse(vimScope.matches("^test$", "This is a test", false))
    assertTrue(vimScope.matches("^test$", "test", false))
  }

  @Test
  fun `test matches with empty text`() {
    assertFalse(vimScope.matches("pattern", "", false))
  }

  @Test
  fun `test matches with empty pattern`() {
    // invalid pattern - ""
    val exception = assertThrows<VimRegexException> {
      vimScope.matches("", "", false)
    }
    assert(exception.message.contains("E383"))
  }

  @Test
  fun `test getAllMatches with simple pattern`() {
    val text = "one two one three one four"
    val matches = vimScope.getAllMatches(text, "one")

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

    val matches = vimScope.getAllMatches(text, "\\d\\+")

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
    val matches = vimScope.getAllMatches(text, "four")

    assert(matches.isEmpty())
  }

  @Test
  fun `test getAllMatches with empty text`() {
    val matches = vimScope.getAllMatches("", "pattern")
    assert(matches.isEmpty())
  }

  @Test
  fun `test getAllMatches with empty pattern`() {
    val text = "some text"

    assertThrows<VimRegexException> {
      vimScope.getAllMatches(text, "")
    }
  }

  @Test
  fun `test getAllMatches with overlapping patterns`() {
    val text = "abababa"
    val matches = vimScope.getAllMatches(text, "aba")

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
    val result = vimScope.getNextCamelStartOffset(camelCaseText, 1)
    assertNotNull(result)

    val result2 = vimScope.getNextCamelStartOffset(camelCaseText, result + 1)
    result2?.let {
      assert(it > result) { "Expected second boundary to be after first boundary" }
    }
  }

  @Test
  fun `test getNextCamelStartOffset with count parameter`() {
    val result = vimScope.getNextCamelStartOffset(camelCaseText, 0, 3)
    assert(result == 6)
  }

  @Test
  fun `test getNextCamelStartOffset in snake_case text`() {
    val result = vimScope.getNextCamelStartOffset(snakeCaseText, 0, 2)
    assert(result == 5)
  }

  @Test
  fun `test getNextCamelStartOffset at end of text`() {
    val result = vimScope.getNextCamelStartOffset(camelCaseText, camelCaseText.length - 1)
    assert(result == null)
  }

  @Test
  fun `test getNextCamelStartOffset in empty text`() {
    val result = vimScope.getNextCamelStartOffset(emptyText, 0)
    assert(result == null)
  }

  @Test
  fun `test getPreviousCamelStartOffset in camelCase text`() {
    val result = vimScope.getPreviousCamelStartOffset(camelCaseText, 10)
    assert(result == 6)

    val result2 = vimScope.getPreviousCamelStartOffset(camelCaseText, 6)
    assert(result2 == 4)
  }

  @Test
  fun `test getPreviousCamelStartOffset with count parameter`() {
    val result = vimScope.getPreviousCamelStartOffset(camelCaseText, 10, 3)
    assert(result == 0)
  }

  @Test
  fun `test getPreviousCamelStartOffset in snake_case text`() {
    val result = vimScope.getPreviousCamelStartOffset(snakeCaseText, 10)
    assert(result == 8)
  }

  @Test
  fun `test getPreviousCamelStartOffset at start of text`() {
    val result = vimScope.getPreviousCamelStartOffset(camelCaseText, 0)
    assert(result == null)
  }

  @Test
  fun `test getNextCamelEndOffset in camelCase text`() {
    val result = vimScope.getNextCamelEndOffset(camelCaseText, 0)
    assert(result == 3)

    val result2 = vimScope.getNextCamelEndOffset(camelCaseText, 4)
    assert(result2 == 5)
  }

  @Test
  fun `test getNextCamelEndOffset with count parameter`() {
    for (count in 1..3) {
      val result = vimScope.getNextCamelEndOffset(camelCaseText, 0, count)

      assertNotNull(result) { "Expected to find a camel case boundary with count=$count" }

      assert(result > 0 && result < camelCaseText.length) { "Boundary should be within text bounds" }
    }
  }

  @Test
  fun `test getNextCamelEndOffset in snake_case text`() {
    val result = vimScope.getNextCamelEndOffset(snakeCaseText, 0)
    assertNotNull(result) { "Expected to find a camel/snake case boundary" }

    assert(result > 0 && result < snakeCaseText.length) { "Boundary should be within text bounds" }
  }

  @Test
  fun `test getPreviousCamelEndOffset in camelCase text`() {
    val result = vimScope.getPreviousCamelEndOffset(camelCaseText, 10)
    assert(result == 5)

    val result2 = vimScope.getPreviousCamelEndOffset(camelCaseText, 5)
    assert(result2 == 3)
  }

  @Test
  fun `test getPreviousCamelEndOffset with count parameter`() {
    val result = vimScope.getPreviousCamelEndOffset(camelCaseText, 15, 3)
    assert(result == 5)
  }

  @Test
  fun `test getPreviousCamelEndOffset in snake_case text`() {
    val result = vimScope.getPreviousCamelEndOffset(snakeCaseText, 10)
    assert(result == 6)
  }
}
