/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.option

import com.intellij.testFramework.UsefulTestCase.assertDoesntContain
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper.parseValues
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper.toRegex
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertSame

class KeywordOptionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private val values: List<String>?
    get() = parseValues(options().getStringValue(Options.iskeyword))

  private fun setKeyword(value: String) {
    enterCommand("set iskeyword=$value")
  }

  private fun assertIsKeyword(c: Char) {
    val charType = charType(c, false)
    assertSame(CharacterHelper.CharacterType.KEYWORD, charType)
  }

  private fun assertIsNotKeyword(c: Char) {
    val charType = charType(c, false)
    assertSame(CharacterHelper.CharacterType.PUNCTUATION, charType)
  }

  @Test
  fun testSingleCommaIsAValue() {
    setKeyword(",")
    kotlin.test.assertEquals(",", values!![0])
  }

  @Test
  fun testSingleCommaIsAValueAsAppend() {
    enterCommand("set iskeyword^=,")
    kotlin.test.assertTrue(values!!.contains(","))
  }

  @Test
  fun testSingleNegatedCommaIsAValue() {
    setKeyword("^,")
    kotlin.test.assertEquals("^,", values!![0])
  }

  @Test
  fun testCommaInARangeIsAValue() {
    setKeyword("+-,")
    kotlin.test.assertEquals("+-,", values!![0])
  }

  @Test
  fun testSecondCommaIsASeparator() {
    setKeyword(",,a")
    kotlin.test.assertEquals(",", values!![0])
    kotlin.test.assertEquals("a", values!![1])
  }

  @Test
  fun testSingleHyphenIsAValue() {
    setKeyword("-")
    kotlin.test.assertEquals("-", values!![0])
  }

  @Test
  fun testHyphenBetweenCharNumsIsARange() {
    setKeyword("a-b")
    kotlin.test.assertEquals("a-b", values!![0])
  }

  @Test
  fun testRangeInWhichLeftValueIsHigherThanRightValueIsInvalid() {
    setKeyword("b-a")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: iskeyword=b-a")
    assertDoesntContain(
      values!!,
      object : ArrayList<String?>() {
        init {
          add("b-a")
        }
      },
    )
  }

  @Test
  fun testTwoAdjacentLettersAreInvalid() {
    setKeyword("ab")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: iskeyword=ab")
    assertDoesntContain(
      values!!,
      object : ArrayList<String?>() {
        init {
          add("ab")
        }
      },
    )
  }

  @Test
  fun testAddsACharByChar() {
    setKeyword("-")
    assertIsKeyword('-')
  }

  @Test
  fun testAddsACharByUnicodeCodePoint() {
    setKeyword("" + '-'.code)
    assertIsKeyword('-')
  }

  @Test
  fun testAddsARange() {
    setKeyword("a-c")
    assertIsKeyword('a')
    assertIsKeyword('b')
    assertIsKeyword('c')
  }

  @Test
  fun testAtSignRepresentsAllLetters() {
    setKeyword("@")
    assertIsKeyword('A')
    assertIsKeyword('Ā')
  }

  @Test
  fun testRangeOfAtSignToAtSignRepresentsAtSign() {
    setKeyword("@-@")
    assertIsKeyword('@')
  }

  @Test
  fun testCaretRemovesAChar() {
    setKeyword("a")
    enterCommand("set iskeyword+=^a")
    assertIsNotKeyword('a')
  }

  @Test
  fun testCaretRemovesARange() {
    setKeyword("a-c")
    enterCommand("set iskeyword+=^b-c,d")
    assertIsKeyword('a')
    assertIsNotKeyword('b')
    assertIsNotKeyword('c')
  }

  @Test
  fun testCaretAloneRepresentsACaret() {
    setKeyword("^")
    assertIsKeyword('^')
  }

  @Test
  fun testMultibyteCharactersAreKeywords() {
    assertIsKeyword('Ź')
  }

  @Test
  fun testToRegex() {
    setKeyword("-,a-c")
    val res = toRegex()
    kotlin.test.assertEquals(2, res.size)
    kotlin.test.assertTrue(res.contains("-"))
    kotlin.test.assertTrue(res.contains("[a-c]"))
  }

  @Test
  fun testAllLettersToRegex() {
    setKeyword("@")
    val res = toRegex()
    kotlin.test.assertEquals(res[0], "\\p{L}")
  }
}
