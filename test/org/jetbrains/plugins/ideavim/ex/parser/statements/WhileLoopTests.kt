package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.statements.loops.WhileLoop
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhileLoopTests {

  @Test
  fun `while loop`() {
    val script = VimscriptParser.parse(
      """
            while x < 5
                echo x
            endwhile
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is WhileLoop)
    val w = script.units[0] as WhileLoop
    assertEquals(1, w.body.size)
    assertTrue(w.body[0] is EchoCommand)
  }
}
