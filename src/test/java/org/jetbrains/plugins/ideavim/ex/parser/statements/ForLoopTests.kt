/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.ListExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.statements.loops.ForLoop
import com.maddyhome.idea.vim.vimscript.model.statements.loops.ForLoopWithList
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Theories::class)
class ForLoopTests {

  companion object {
    @JvmStatic
    val values = listOf("", " ") @DataPoints get
  }

  @Theory
  fun `for loop`(sp1: String, sp2: String, sp3: String) {
    val script = VimscriptParser.parse(
      """
        for key in keys(mydict)$sp1
            echo key . ':' . mydict(key)$sp2
        endfor$sp3
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is ForLoop)
    val f = script.units[0] as ForLoop
    assertEquals(null, f.variable.scope)
    assertEquals(VimString("key"), f.variable.name.evaluate())
    assertTrue(f.iterable is FunctionCallExpression)
    assertEquals(1, f.body.size)
  }

  @Theory
  fun `empty for loop`(sp1: String, sp2: String, sp3: String) {
    val script = VimscriptParser.parse(
      """
        for g:key in [1, 2, 3]$sp1
        endfor$sp2
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is ForLoop)
    val f = script.units[0] as ForLoop
    assertEquals(Scope.GLOBAL_VARIABLE, f.variable.scope)
    assertEquals(VimString("key"), f.variable.name.evaluate())
    assertTrue(f.iterable is ListExpression)
    assertEquals(0, f.body.size)
  }

  @Theory
  fun `for loop with list`(sp1: String, sp2: String) {
    val script = VimscriptParser.parse(
      """
        for [key, value] in listOfLists$sp1
        endfor$sp2
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertEquals(listOf("key", "value"), (script.units[0] as ForLoopWithList).variables)
  }
}
