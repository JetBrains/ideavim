/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.statements.loops.WhileLoop
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.productForArguments
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhileLoopTests : VimTestCase() {

  companion object {
    val values = listOf("", " ")

    @JvmStatic
    fun arg3(): List<Arguments> = productForArguments(values, values, values)
  }

  @ParameterizedTest
  @MethodSource("arg3")
  fun `while loop`(sp1: String, sp2: String, sp3: String) {
    val script = VimscriptParser.parse(
      """
        while x < 5$sp1
            echo x$sp2
        endwhile$sp3
      """.trimIndent(),
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is WhileLoop)
    val w = script.units[0] as WhileLoop
    assertEquals(1, w.body.size)
    assertTrue(w.body[0] is EchoCommand)
  }
}
