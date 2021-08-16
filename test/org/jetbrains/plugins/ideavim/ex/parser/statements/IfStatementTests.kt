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
