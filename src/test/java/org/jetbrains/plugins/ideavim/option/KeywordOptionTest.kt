/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KeywordOptionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private val values: List<String>
    get() = KeywordOptionHelper.parseValues(options().iskeyword.value)!!

  private fun setKeyword(value: String) {
    enterCommand("set iskeyword=$value")
  }

  private fun assertIsKeyword(c: Char) {
    val charType = charType(fixture.editor.vim, c, false)
    assertSame(CharacterHelper.CharacterType.KEYWORD, charType)
  }

  private fun assertIsNotKeyword(c: Char) {
    val charType = charType(fixture.editor.vim, c, false)
    assertSame(CharacterHelper.CharacterType.PUNCTUATION, charType)
  }

  @Test
  fun testSingleCommaIsAValue() {
    setKeyword(",")
    assertEquals(",", values[0])
  }

  @Test
  fun testSingleCommaIsAValueAsAppend() {
    enterCommand("set iskeyword^=,")
    assertTrue(values.contains(","))
  }

  @Test
  fun testSingleNegatedCommaIsAValue() {
    setKeyword("^,")
    assertEquals("^,", values[0])
  }

  @Test
  fun testCommaInARangeIsAValue() {
    setKeyword("+-,")
    assertEquals("+-,", values[0])
  }

  @Test
  fun testSecondCommaIsASeparator() {
    setKeyword(",,a")
    assertEquals(",", values[0])
    assertEquals("a", values[1])
  }

  @Test
  fun testSingleHyphenIsAValue() {
    setKeyword("-")
    assertEquals("-", values[0])
  }

  @Test
  fun testHyphenBetweenCharNumsIsARange() {
    setKeyword("a-b")
    assertEquals("a-b", values[0])
  }

  @Test
  fun testRangeInWhichLeftValueIsHigherThanRightValueIsInvalid() {
    setKeyword("b-a")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: iskeyword=b-a")
    assertTrue("b-a" !in values)
  }

  @Test
  fun testTwoAdjacentLettersAreInvalid() {
    setKeyword("ab")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: iskeyword=ab")
    assertTrue("ab" !in values)
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

  @Suppress("DEPRECATION")
  @Test
  fun testToRegex() {
    setKeyword("-,a-c")
    val res = KeywordOptionHelper.toRegex()
    assertEquals(2, res.size)
    assertTrue(res.contains("-"))
    assertTrue(res.contains("[a-c]"))
  }

  @Suppress("DEPRECATION")
  @Test
  fun testAllLettersToRegex() {
    setKeyword("@")
    val res = KeywordOptionHelper.toRegex()
    assertEquals(res[0], "\\p{L}")
  }
}
