/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import com.intellij.testFramework.assertInstanceOf
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FalsyOperatorTest : VimTestCase() {

  @Test
  fun `test non-zero Number treated as truthy`() {
    assertEquals(VimInt(42), VimscriptParser.parseExpression("42 ?? 999")!!.evaluate())
  }

  @Test
  fun `test non-zero negative Number treated as truthy`() {
    assertEquals(VimInt(-1), VimscriptParser.parseExpression("-1 ?? 999")!!.evaluate())
  }

  @Test
  fun `test Number 0 is treated as falsy`() {
    assertEquals(VimInt(42), VimscriptParser.parseExpression("0 ?? 42")!!.evaluate())
  }

  @Test
  fun `test zero Float is treated as falsy`() {
    assertEquals(VimInt(42), VimscriptParser.parseExpression("0.0 ?? 42")!!.evaluate())
  }

  @Test
  fun `test non-zero Float treated as truthy`() {
    assertEquals(VimFloat(42.0), VimscriptParser.parseExpression("42.0 ?? 999")!!.evaluate())
  }

  @Test
  fun `test empty String treated as falsy`() {
    assertEquals(VimString("string is empty"), VimscriptParser.parseExpression("'' ?? 'string is empty'")!!.evaluate())
  }

  @Test
  fun `test String '0' treated as falsy`() {
    assertEquals(VimString("string is empty"), VimscriptParser.parseExpression("'0' ?? 'string is empty'")!!.evaluate())
  }

  @Test
  fun `test non-empty String treated as truthy`() {
    assertEquals(VimString("string is not empty"), VimscriptParser.parseExpression("'string is not empty' ?? 'string is empty'")!!.evaluate())
  }

  @Test
  fun `test empty list treated as falsy`() {
    assertEquals(VimString("list is empty"), VimscriptParser.parseExpression("[] ?? 'list is empty'")!!.evaluate())
  }

  @Test
  fun `test non-empty List treated as truthy`() {
    val expression = VimscriptParser.parseExpression("[1, 2, 3] ?? 'list is empty'")!!.evaluate()
    assertInstanceOf<VimList>(expression)
  }

  @Test
  fun `test empty Dictionary treated as falsy`() {
    assertEquals(VimString("dict is empty"), VimscriptParser.parseExpression("{} ?? 'dict is empty'")!!.evaluate())
  }

  @Test
  fun `test non-empty Dictionary treated as truthy`() {
    val expression = VimscriptParser.parseExpression("{'1': 1} ?? 'dict is empty'")!!.evaluate()
    assertInstanceOf<VimDictionary>(expression)
  }
}
