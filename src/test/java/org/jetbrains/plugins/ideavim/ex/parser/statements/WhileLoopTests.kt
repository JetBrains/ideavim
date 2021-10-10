package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.statements.loops.WhileLoop
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Theories::class)
class WhileLoopTests {

  companion object {
    @JvmStatic
    val values = listOf("", " ") @DataPoints get
  }

  @Theory
  fun `while loop`(sp1: String, sp2: String, sp3: String) {
    val script = VimscriptParser.parse(
      """
        while x < 5$sp1
            echo x$sp2
        endwhile$sp3
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is WhileLoop)
    val w = script.units[0] as WhileLoop
    assertEquals(1, w.body.size)
    assertTrue(w.body[0] is EchoCommand)
  }
}
