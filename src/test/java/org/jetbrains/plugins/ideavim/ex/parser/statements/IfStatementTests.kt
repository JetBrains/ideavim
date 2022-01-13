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

import com.maddyhome.idea.vim.vimscript.model.statements.IfStatement
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Theories::class)
class IfStatementTests {

  companion object {
    @JvmStatic
    val values = listOf("", " ") @DataPoints get
  }

  @Theory
  fun ifTest(sp1: String, sp2: String, sp3: String, sp4: String) {
    val script = VimscriptParser.parse(
      """
        if char == "\<LeftMouse>"$sp1
          " empty block
        elseif char == "\<RightMouse>"$sp2
          " one echo
          echo 1
        else$sp3
          " two echos
          echo 1
          echo 1
        endif$sp4
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is IfStatement)
    val s = script.units[0] as IfStatement
    assertEquals(3, s.conditionToBody.size)
    assertEquals(0, s.conditionToBody[0].second.size)
    assertEquals(1, s.conditionToBody[1].second.size)
    assertEquals(2, s.conditionToBody[2].second.size)
  }
}
