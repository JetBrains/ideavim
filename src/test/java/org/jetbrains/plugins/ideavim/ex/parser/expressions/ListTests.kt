/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import kotlin.test.assertEquals

class ListTests : VimTestCase() {

  @Test
  fun `empty list test`() {
    val expression = VimscriptParser.parseExpression("[]")!!.evaluate()
    assertInstanceOf<VimList>(expression)
    assertEquals(0, expression.values.size)
  }

  @Test
  fun `list of simple types test`() {
    val expression = VimscriptParser.parseExpression("[1, 4.6, 'bla bla', [5, 9], {'key' : 'value'}]")!!.evaluate()
    assertInstanceOf<VimList>(expression)
    assertEquals(VimInt(1), expression[0])
    assertEquals(VimFloat(4.6), expression[1])
    assertEquals(VimString("bla bla"), expression[2])
    val list = assertInstanceOf<VimList>(expression[3])
    assertEquals(VimInt(5), list[0])
    assertEquals(VimInt(9), list[1])
    val dictionary = assertInstanceOf<VimDictionary>(expression[4])
    assertEquals(VimString("value"), dictionary.dictionary[VimString("key")])
  }

  @Test
  fun `comma at the list end test`() {
    val expression = VimscriptParser.parseExpression("[1, 2,]")!!.evaluate()
    assertInstanceOf<VimList>(expression)
    assertEquals(VimInt(1), expression[0])
    assertEquals(VimInt(2), expression[1])
  }
}
