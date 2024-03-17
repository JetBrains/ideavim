/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.statements.IfStatement
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.productForArguments
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IfStatementTests : VimTestCase() {

  companion object {
    @JvmStatic
    val values = listOf("", " ")

    @JvmStatic
    fun arg4(): List<Arguments> = productForArguments(values, values, values, values)
  }

  @ParameterizedTest
  @MethodSource("arg4")
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
      """.trimIndent(),
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
