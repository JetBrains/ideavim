/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.NamedFunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.ListExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.statements.loops.ForLoop
import com.maddyhome.idea.vim.vimscript.model.statements.loops.ForLoopWithList
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.jetbrains.plugins.ideavim.productForArguments
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForLoopTests : VimTestCase() {

  companion object {
    @JvmStatic
    val values = listOf("", " ")

    @JvmStatic
    fun arg3(): List<Arguments> = productForArguments(values, values, values)

    @JvmStatic
    fun arg2(): List<Arguments> = productForArguments(values, values)
  }

  @ParameterizedTest
  @MethodSource("arg3")
  fun `for loop`(sp1: String, sp2: String, sp3: String) {
    val script = VimscriptParser.parse(
      """
        for key in keys(mydict)$sp1
            echo key . ':' . mydict(key)$sp2
        endfor$sp3
      """.trimIndent(),
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is ForLoop)
    val f = script.units[0] as ForLoop
    assertEquals(null, f.variable.scope)
    assertEquals(VimString("key"), f.variable.name.evaluate())
    assertTrue(f.iterable is NamedFunctionCallExpression)
    assertEquals(1, f.body.size)
  }

  @ParameterizedTest
  @MethodSource("arg3")
  fun `empty for loop`(sp1: String, sp2: String, sp3: String) {
    val script = VimscriptParser.parse(
      """
        for g:key in [1, 2, 3]$sp1
        endfor$sp2
      """.trimIndent(),
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is ForLoop)
    val f = script.units[0] as ForLoop
    assertEquals(Scope.GLOBAL_VARIABLE, f.variable.scope)
    assertEquals(VimString("key"), f.variable.name.evaluate())
    assertTrue(f.iterable is ListExpression)
    assertEquals(0, f.body.size)
  }

  @ParameterizedTest
  @MethodSource("arg2")
  fun `for loop with list`(sp1: String, sp2: String) {
    val script = VimscriptParser.parse(
      """
        for [key, value] in listOfLists$sp1
        endfor$sp2
      """.trimIndent(),
    )
    assertEquals(1, script.units.size)
    assertEquals(listOf("key", "value"), (script.units[0] as ForLoopWithList).variables)
  }
}
