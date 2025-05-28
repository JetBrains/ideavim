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

    @JvmStatic
    fun patternTestCases(): List<TestCase> {
      return listOf(
        TestCase(
          // skip first character
          sortCommand = "sort /./",
          content = """
          '
          a
          ab
          aBc
          a122
          b123
          c121
        """.trimIndent(),
          expected = """
           '
           a
           c121
           a122
           b123
           aBc
           ab
        """.trimIndent()
        ),
        TestCase(
          // skip first character reversed
          sortCommand = "sort! /./",
          content = """
          '
          a
          ab
          abc
          a122
          b123
          c121
        """.trimIndent(),
          expected = """
           abc
           ab
           b123
           a122
           c121
           '
           a
        """.trimIndent()
        ),
        TestCase(
          // skip first character case-insensitive
          sortCommand = "sort /./ i",
          content = """
          '
          a
          ab
          aBc
          a122
          b123
          c121
        """.trimIndent(),
          expected = """
           '
           a
           c121
           a122
           b123
           ab
           aBc
        """.trimIndent()
        ),
        TestCase(
          // skip first character numeric sort
          sortCommand = "sort /./ n",
          content = """
          '
          a
          a122
          b2
          c121
        """.trimIndent(),
          expected = """
           '
           a
           b2
           c121
           a122
        """.trimIndent()
        ),
        TestCase(
          // sort on first character
          sortCommand = "sort /./ r",
          content = """
          '
          baa
          azz
          abb
          aaa
        """.trimIndent(),
          expected = """
           '
           azz
           abb
           aaa
           baa
        """.trimIndent()
        ),
        TestCase(
          // numeric sort skip first digit
          sortCommand = "sort /\\d/ n",
          content = """
           190
           270
           350
           410
        """.trimIndent(),
          expected = """
           410
           350
           270
           190
        """.trimIndent()
        ),
        TestCase(
          // numeric sort on first digit
          sortCommand = "sort /\\d/ nr",
          content = """
           10
           90
           100
           700
        """.trimIndent(),
          expected = """
           10
           100
           700
           90
        """.trimIndent()
        ),
        TestCase(
          // sort on third virtual column
          sortCommand = "sort /.*\\%3v/",
          content = """
            aad
            bbc
            ccb
            dda
        """.trimIndent(),
          expected = """
            dda
            ccb
            bbc
            aad
        """.trimIndent()
        ),
        TestCase(
          // sort on second comma separated field
          sortCommand = "sort /[^,]*/",
          content = """
            aaa,ddd
            bbb,ccc
            ccc,bbb
            ddd,aaa
        """.trimIndent(),
          expected = """
            ddd,aaa
            ccc,bbb
            bbb,ccc
            aaa,ddd
        """.trimIndent()
        ),
        TestCase(
          // sort on first number in line
          sortCommand = "sort /.\\{-}\\ze\\d/ n",
          content = """
            aaaa9
            b10
            ccccc7
            dd3
        """.trimIndent(),
          expected = """
            dd3
            ccccc7
            aaaa9
            b10
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

  @ParameterizedTest
  @MethodSource("patternTestCases")
  fun `test sort with pattern`(
    testCase: TestCase,
  ) = assertSort(testCase)

  @Test
  fun testSortWithPrecedingWhiteSpace() {
    configureByText(" zee\n c\n a\n b\n whatever")
    typeText(commandToKeys("sort"))
    assertState(" a\n b\n c\n whatever\n zee")
  }

  @Test
  fun `test sort and undo`() {
    configureByText(
      """
      |zebra
      |${c}apple
      |banana
      |cherry
      """.trimMargin()
    )

    typeText(commandToKeys("sort"))
    assertState(
      """
      |${c}apple
      |banana
      |cherry
      |zebra
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |zebra
      |${c}apple
      |banana
      |cherry
      """.trimMargin()
    )
  }

  @Test
  fun `test sort with range and undo`() {
    configureByText(
      """
      |header
      |zebra
      |${c}apple
      |banana
      |cherry
      |footer
      """.trimMargin()
    )

    typeText(commandToKeys("2,5sort"))
    assertState(
      """
      |header
      |${c}apple
      |banana
      |cherry
      |zebra
      |footer
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |header
      |zebra
      |${c}apple
      |banana
      |cherry
      |footer
      """.trimMargin()
    )
  }

  @Test
  fun `test sort with options and undo`() {
    configureByText(
      """
      |${c}10
      |2
      |100
      |20
      """.trimMargin()
    )

    typeText(commandToKeys("sort n"))
    assertState(
      """
      |${c}2
      |10
      |20
      |100
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |${c}10
      |2
      |100
      |20
      """.trimMargin()
    )
  }

  @Test
  fun `test reverse sort and undo`() {
    configureByText(
      """
      |${c}apple
      |banana
      |cherry
      |date
      """.trimMargin()
    )

    typeText(commandToKeys("sort!"))
    assertState(
      """
      |${c}date
      |cherry
      |banana
      |apple
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |${c}apple
      |banana
      |cherry
      |date
      """.trimMargin()
    )
  }
}
