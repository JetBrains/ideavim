/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.newapi.initInjector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.jetbrains.plugins.ideavim.ex.parser.ParserTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DictionaryTests : ParserTest() {

  @BeforeEach
  fun setUp() {
    initInjector()
  }

  @Test
  fun `empty dictionary test`() {
    val expression = VimscriptParser.parseExpression("{}")!!.evaluate()
    assertInstanceOf<VimDictionary>(expression)
    assertTrue(expression.dictionary.isEmpty())
  }

  @Test
  fun `dictionary of simple types test`() {
    val dictString = "{$ZERO_OR_MORE_SPACES" +
      "'a'$ZERO_OR_MORE_SPACES:$ZERO_OR_MORE_SPACES'string expression'$ZERO_OR_MORE_SPACES,$ZERO_OR_MORE_SPACES" +
      "'b'$ZERO_OR_MORE_SPACES:$ZERO_OR_MORE_SPACES[1, 2]$ZERO_OR_MORE_SPACES" +
      "}"
    for (s in getTextWithAllSpacesCombinations(dictString)) {
      val expression = VimscriptParser.parseExpression(s)!!.evaluate()
      assertInstanceOf<VimDictionary>(expression)
      assertEquals(VimString("string expression"), expression.dictionary[VimString("a")])
      val list = assertInstanceOf<VimList>(expression.dictionary[VimString("b")])
      assertEquals(VimInt(1), list[0])
      assertEquals(VimInt(2), list[1])
    }
  }

  @Test
  fun `dictionary of simple types test 2`() {
    val dictString = "{$ZERO_OR_MORE_SPACES" +
      "'c'$ZERO_OR_MORE_SPACES:$ZERO_OR_MORE_SPACES{'key':'value'},$ZERO_OR_MORE_SPACES" +
      "'d'$ZERO_OR_MORE_SPACES:${ZERO_OR_MORE_SPACES}5$ZERO_OR_MORE_SPACES" +
      "}"
    for (s in getTextWithAllSpacesCombinations(dictString)) {
      val expression = VimscriptParser.parseExpression(s)!!.evaluate()
      assertInstanceOf<VimDictionary>(expression)
      val innerDictionary = assertInstanceOf<VimDictionary>(expression.dictionary[VimString("c")])
      assertEquals(VimString("value"), innerDictionary.dictionary[VimString("key")])
      assertEquals(VimInt(5), expression.dictionary[VimString("d")])
    }
  }

  @Test
  fun `dictionary of simple types test 3`() {
    val dictString = "{$ZERO_OR_MORE_SPACES" +
      "'e'$ZERO_OR_MORE_SPACES:${ZERO_OR_MORE_SPACES}4.2$ZERO_OR_MORE_SPACES" +
      "}"
    for (s in getTextWithAllSpacesCombinations(dictString)) {
      val expression = VimscriptParser.parseExpression(s)!!.evaluate()
      assertInstanceOf<VimDictionary>(expression)
      assertEquals(VimFloat(4.2), expression.dictionary[VimString("e")])
    }
  }

  @Test
  fun `empty literal dictionary test`() {
    val expression = VimscriptParser.parseExpression("#{}")!!.evaluate()
    assertInstanceOf<VimDictionary>(expression)
    assertTrue(expression.dictionary.isEmpty())
  }

  @Test
  fun `literal dictionary of simple types test`() {
    val dictString =
      "#{${ZERO_OR_MORE_SPACES}test$ZERO_OR_MORE_SPACES:${ZERO_OR_MORE_SPACES}12$ZERO_OR_MORE_SPACES," +
        "${ZERO_OR_MORE_SPACES}2-1$ZERO_OR_MORE_SPACES:$ZERO_OR_MORE_SPACES'string value'$ZERO_OR_MORE_SPACES}"
    for (s in getTextWithAllSpacesCombinations(dictString)) {
      val expression = VimscriptParser.parseExpression(s)!!.evaluate()
      assertInstanceOf<VimDictionary>(expression)
      assertEquals(VimInt(12), expression.dictionary[VimString("test")])
      assertEquals(VimString("string value"), expression.dictionary[VimString("2-1")])
    }
  }

  @Test
  fun `comma at dictionary end test`() {
    val expression = VimscriptParser.parseExpression("{'one': 1,}")!!.evaluate()
    assertInstanceOf<VimDictionary>(expression)
    assertEquals(VimInt.ONE, expression.dictionary[VimString("one")])
  }

  @Test
  fun `comma at literal dictionary end test`() {
    val expression = VimscriptParser.parseExpression("#{one: 1,}")!!.evaluate()
    assertInstanceOf<VimDictionary>(expression)
    assertEquals(VimInt.ONE, expression.dictionary[VimString("one")])
  }
}
