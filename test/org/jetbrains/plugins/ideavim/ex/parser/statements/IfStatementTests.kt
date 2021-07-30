package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.statements.IfStatement
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IfStatementTests {

  @Test
  fun ifTest() {
    val script = VimscriptParser.parse(
      """
            if char == "\<LeftMouse>"
              " empty block
            elseif char == "\<RightMouse>"
              " one echo
              echo 1
            else
              " two echos
              echo 1
              echo 1
            endif
            
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
