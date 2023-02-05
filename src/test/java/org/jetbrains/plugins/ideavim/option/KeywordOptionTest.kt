/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper.parseValues
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper.toRegex
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.VimTestCase

@Suppress("SpellCheckingInspection")
class KeywordOptionTest : VimTestCase() {
  private val values: List<String>?
    get() = parseValues(optionsNoEditor().getStringValue(OptionConstants.iskeyword))

  private fun setKeyword(`val`: String) {
    VimPlugin.getOptionService()
      .setOptionValue(OptionScope.GLOBAL, OptionConstants.iskeyword, VimString(`val`), "testToken")
  }

  private fun assertIsKeyword(c: Char) {
    val charType = charType(c, false)
    assertSame(CharacterHelper.CharacterType.KEYWORD, charType)
  }

  private fun assertIsNotKeyword(c: Char) {
    val charType = charType(c, false)
    assertSame(CharacterHelper.CharacterType.PUNCTUATION, charType)
  }

  fun testSingleCommaIsAValue() {
    setKeyword(",")
    assertEquals(",", values!![0])
  }

  fun testSingleCommaIsAValueAsAppend() {
    injector.vimscriptExecutor.execute("set iskeyword^=,", false)
    assertTrue(values!!.contains(","))
  }

  fun testSingleNegatedCommaIsAValue() {
    setKeyword("^,")
    assertEquals("^,", values!![0])
  }

  fun testCommaInARangeIsAValue() {
    setKeyword("+-,")
    assertEquals("+-,", values!![0])
  }

  fun testSecondCommaIsASeparator() {
    setKeyword(",,a")
    assertEquals(",", values!![0])
    assertEquals("a", values!![1])
  }

  fun testSingleHyphenIsAValue() {
    setKeyword("-")
    assertEquals("-", values!![0])
  }

  fun testHyphenBetweenCharNumsIsARange() {
    setKeyword("a-b")
    assertEquals("a-b", values!![0])
  }

  fun testRangeInWhichLeftValueIsHigherThanRightValueIsInvalid() {
    try {
      setKeyword("b-a")
      fail("exception missing")
    } catch (e: Exception) {
      assertEquals("E474: Invalid argument: testToken", e.message)
    }
    assertDoesntContain(
      values!!,
      object : ArrayList<String?>() {
        init {
          add("b-a")
        }
      }
    )
  }

  fun testTwoAdjacentLettersAreInvalid() {
    try {
      setKeyword("ab")
      fail("exception missing")
    } catch (e: Exception) {
      assertEquals("E474: Invalid argument: testToken", e.message)
    }
    assertDoesntContain(
      values!!,
      object : ArrayList<String?>() {
        init {
          add("ab")
        }
      }
    )
  }

  fun testAddsACharByChar() {
    setKeyword("-")
    assertIsKeyword('-')
  }

  fun testAddsACharByUnicodeCodePoint() {
    setKeyword("" + '-'.code)
    assertIsKeyword('-')
  }

  fun testAddsARange() {
    setKeyword("a-c")
    assertIsKeyword('a')
    assertIsKeyword('b')
    assertIsKeyword('c')
  }

  fun testAtSignRepresentsAllLetters() {
    setKeyword("@")
    assertIsKeyword('A')
    assertIsKeyword('Ā')
  }

  fun testRangeOfAtSignToAtSignRepresentsAtSign() {
    setKeyword("@-@")
    assertIsKeyword('@')
  }

  fun testCaretRemovesAChar() {
    setKeyword("a")
    injector.vimscriptExecutor.execute("set iskeyword+=^a", true)
    assertIsNotKeyword('a')
  }

  fun testCaretRemovesARange() {
    setKeyword("a-c")
    injector.vimscriptExecutor.execute("set iskeyword+=^b-c,d", true)
    assertIsKeyword('a')
    assertIsNotKeyword('b')
    assertIsNotKeyword('c')
  }

  fun testCaretAloneRepresentsACaret() {
    setKeyword("^")
    assertIsKeyword('^')
  }

  fun testMultibyteCharactersAreKeywords() {
    assertIsKeyword('Ź')
  }

  fun testToRegex() {
    setKeyword("-,a-c")
    val res = toRegex()
    assertEquals(2, res.size)
    assertTrue(res.contains("-"))
    assertTrue(res.contains("[a-c]"))
  }

  fun testAllLettersToRegex() {
    setKeyword("@")
    val res = toRegex()
    assertEquals(res[0], "\\p{L}")
  }
}
