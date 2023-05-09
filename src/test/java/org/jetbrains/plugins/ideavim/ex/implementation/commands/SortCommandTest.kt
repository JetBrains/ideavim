/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.google.common.collect.Lists
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import javax.swing.KeyStroke

@Suppress("SpellCheckingInspection")
class SortCommandTest : VimTestCase() {
  private fun assertSort(testCase: TestCase) {
    val (content, visualSelect, sortCommand, expected) = testCase
    configureByText(content)
    if (visualSelect.isNotBlank()) {
      val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
      keys.addAll(injector.parser.stringToKeys(visualSelect))
      typeText(keys)
    }
    typeText(commandToKeys(sortCommand))
    assertState(expected)
  }

  data class TestCase(val content: String, val visualSelect: String = "", val sortCommand: String, val expected: String)

  companion object {
    @JvmStatic
    fun defaultSortTestCases(): List<TestCase> {
      return listOf(
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
      """.trimIndent(),
          sortCommand = "sort",
          expected = """
        10
        2
        AB
        IGNORE_CASE_DUPLICATE
        ac
        duplicate
        duplicate
        ignore_case_duplicate
      """.trimIndent()
        ),
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          visualSelect = "$7j",
          sortCommand = "sort",
          expected = """
        10
        2
        AB
        IGNORE_CASE_DUPLICATE
        ac
        duplicate
        duplicate
        ignore_case_duplicate
        a
      """.trimIndent()
        ),
        TestCase(
          content = """
        z
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          sortCommand = "2,9sort",
          expected = """
        z
        10
        2
        AB
        IGNORE_CASE_DUPLICATE
        ac
        duplicate
        duplicate
        ignore_case_duplicate
        a
      """.trimIndent()
        )
      )
    }

    @JvmStatic
    fun numericSortTestCases(): List<TestCase> {
      return listOf(
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
      """.trimIndent(),
          sortCommand = "sort n",
          expected = """
        AB
        IGNORE_CASE_DUPLICATE
        ac
        duplicate
        duplicate
        ignore_case_duplicate
        2
        10
      """.trimIndent()
        ),
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          visualSelect = "$7j",
          sortCommand = "sort n",
          expected = """
        AB
        IGNORE_CASE_DUPLICATE
        ac
        duplicate
        duplicate
        ignore_case_duplicate
        2
        10
        a
      """.trimIndent()
        ),
        TestCase(
          content = """
        z
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          sortCommand = "2,9sort n",
          expected = """
        z
        AB
        IGNORE_CASE_DUPLICATE
        ac
        duplicate
        duplicate
        ignore_case_duplicate
        2
        10
        a
      """.trimIndent()
        )
      )
    }

    @JvmStatic
    fun caseInsensitiveSortTestCases(): List<TestCase> {
      return listOf(
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
      """.trimIndent(),
          sortCommand = "sort i",
          expected = """
        10
        2
        AB
        ac
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
      """.trimIndent()
        ),
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          visualSelect = "$7j",
          sortCommand = "sort i",
          expected = """
        10
        2
        AB
        ac
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent()
        ),
        TestCase(
          content = """
        z
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          sortCommand = "2,9sort i",
          expected = """
        z
        10
        2
        AB
        ac
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent()
        )
      )
    }

    @JvmStatic
    fun reverseSortTestCases(): List<TestCase> {
      return listOf(
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
      """.trimIndent(),
          sortCommand = "sort!",
          expected = """
        ignore_case_duplicate
        duplicate
        duplicate
        ac
        IGNORE_CASE_DUPLICATE
        AB
        2
        10
      """.trimIndent()
        ),
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          visualSelect = "$7j",
          sortCommand = "sort!",
          expected = """
        ignore_case_duplicate
        duplicate
        duplicate
        ac
        IGNORE_CASE_DUPLICATE
        AB
        2
        10
        a
      """.trimIndent()
        ),
        TestCase(
          content = """
        z
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          sortCommand = "2,9sort!",
          expected = """
        z
        ignore_case_duplicate
        duplicate
        duplicate
        ac
        IGNORE_CASE_DUPLICATE
        AB
        2
        10
        a
      """.trimIndent()
        )
      )
    }

    @JvmStatic
    fun uniqueSortTestCases(): List<TestCase> {
      return listOf(
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
      """.trimIndent(),
          sortCommand = "sort u",
          expected = """
        10
        2
        AB
        IGNORE_CASE_DUPLICATE
        ac
        duplicate
        ignore_case_duplicate
      """.trimIndent()
        ),
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          visualSelect = "$7j",
          sortCommand = "sort u",
          expected = """
        10
        2
        AB
        IGNORE_CASE_DUPLICATE
        ac
        duplicate
        ignore_case_duplicate
        a
      """.trimIndent()
        ),
        TestCase(
          content = """
        z
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          sortCommand = "2,9sort u",
          expected = """
        z
        10
        2
        AB
        IGNORE_CASE_DUPLICATE
        ac
        duplicate
        ignore_case_duplicate
        a
      """.trimIndent()
        )
      )
    }

    @JvmStatic
    fun caseInsensitiveUniqueSortTestCases(): List<TestCase> {
      return listOf(
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
      """.trimIndent(),
          sortCommand = "sort iu",
          expected = """
        10
        2
        AB
        ac
        duplicate
        ignore_case_duplicate
      """.trimIndent()
        ),
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          visualSelect = "$7j",
          sortCommand = "sort iu",
          expected = """
        10
        2
        AB
        ac
        duplicate
        ignore_case_duplicate
        a
      """.trimIndent()
        ),
        TestCase(
          content = """
        z
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          sortCommand = "2,9sort iu",
          expected = """
        z
        10
        2
        AB
        ac
        duplicate
        ignore_case_duplicate
        a
      """.trimIndent()
        )
      )
    }

    @JvmStatic
    fun numericCaseInsensitiveReverseUniqueSortTestCases(): List<TestCase> {
      return listOf(
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
      """.trimIndent(),
          sortCommand = "sort! niu",
          expected = """
        10
        2
        ignore_case_duplicate
        duplicate
        ac
        AB
      """.trimIndent()
        ),
        TestCase(
          content = """
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          visualSelect = "$7j",
          sortCommand = "sort! niu",
          expected = """
        10
        2
        ignore_case_duplicate
        duplicate
        ac
        AB
        a
      """.trimIndent()
        ),
        TestCase(
          content = """
        z
        ac
        AB
        10
        2
        duplicate
        duplicate
        ignore_case_duplicate
        IGNORE_CASE_DUPLICATE
        a
      """.trimIndent(),
          sortCommand = "2,9sort! niu",
          expected = """
        z
        10
        2
        ignore_case_duplicate
        duplicate
        ac
        AB
        a
      """.trimIndent()
        )
      )
    }
  }


  @ParameterizedTest
  @MethodSource("defaultSortTestCases")
  fun `test default sort is case sensitive, not numeric, ascending and not unique`(
    testCase: TestCase,
  ) = assertSort(testCase)

  @ParameterizedTest
  @MethodSource("numericSortTestCases")
  fun `test numeric sort is case sensitive, numeric, ascending and not unique`(
    testCase: TestCase,
  ) = assertSort(testCase)

  @ParameterizedTest
  @MethodSource("caseInsensitiveSortTestCases")
  fun `test case insensive sort is case insensitive, not numeric, ascending and not unique`(
    testCase: TestCase,
  ) = assertSort(testCase)

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @ParameterizedTest
  @MethodSource("reverseSortTestCases")
  fun `test reverse sort is case sensitive, not numeric, descending and not unique`(
    testCase: TestCase,
  ) = assertSort(testCase)

  @ParameterizedTest
  @MethodSource("uniqueSortTestCases")
  fun `test unique sort is case sensitive, not numeric, ascending and unique`(
    testCase: TestCase,
  ) = assertSort(testCase)

  @ParameterizedTest
  @MethodSource("caseInsensitiveUniqueSortTestCases")
  fun `test case insensitive unique sort is case insensitive, not numeric, ascending and unique`(
    testCase: TestCase,
  ) = assertSort(testCase)

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @ParameterizedTest
  @MethodSource("numericCaseInsensitiveReverseUniqueSortTestCases")
  fun `test numeric, case insensitive, reverse and unique sort is case insensitive, numeric, descending and unique`(
    testCase: TestCase,
  ) = assertSort(testCase)

  @Test
  fun testSortWithPrecedingWhiteSpace() {
    configureByText(" zee\n c\n a\n b\n whatever")
    typeText(commandToKeys("sort"))
    assertState(" a\n b\n c\n whatever\n zee")
  }
}
