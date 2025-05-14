/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.vimscript.model.statements.IfStatement
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExecutableTextRangesTests : VimTestCase() {
  @Test
  fun `test regular script`() {
    val scriptString = """
      set rnu
      if 1
        let x = 42
      endif
    """.trimIndent()
    val script = VimscriptParser.parse(scriptString)
    assertEquals(2, script.units.size)
    val setCommand = script.units.first()
    assertEquals(TextRange(0, 8), setCommand.rangeInScript)
    val ifStatement = script.units.last() as IfStatement
    assertEquals(TextRange(8, 32), ifStatement.rangeInScript)
    val letCommand = ifStatement.conditionToBody.first().second.first()
    assertEquals(TextRange(13, 26), letCommand.rangeInScript)
  }

  @Test
  fun `test script with error`() {
    val scriptString = """
      set rnu
      if 1
        -0§a " some line that parser cannot recognize (it should be ignored)
        let y = 76
      endif
    """.trimIndent()
    val script = VimscriptParser.parse(scriptString)
    assertEquals(2, script.units.size)
    val setCommand = script.units.first()
    assertEquals(TextRange(0, 8), setCommand.rangeInScript)
    val ifStatement = script.units.last() as IfStatement
    assertEquals(TextRange(8, 103), ifStatement.rangeInScript)
    val letCommand = ifStatement.conditionToBody.first().second.first()
    assertEquals(TextRange(84, 97), letCommand.rangeInScript)
  }

  @Test
  fun `test script with ideavim ignore comment`() {
    val scriptString = """
      set rnu
      "   ideavim ignore start
      oh, hi Mark
      "        ideavim ignore end
      if 1
        -0§a " some line that parser cannot recognize (it should be ignored)
        let y = 76
      endif
    """.trimIndent()
    val script = VimscriptParser.parse(scriptString)
    assertEquals(2, script.units.size)
    val setCommand = script.units.first()
    assertEquals(TextRange(0, 8), setCommand.rangeInScript)
    val ifStatement = script.units.last() as IfStatement
    assertEquals(TextRange(73, 168), ifStatement.rangeInScript)
    val letCommand = ifStatement.conditionToBody.first().second.first()
    assertEquals(TextRange(149, 162), letCommand.rangeInScript)
  }
}
