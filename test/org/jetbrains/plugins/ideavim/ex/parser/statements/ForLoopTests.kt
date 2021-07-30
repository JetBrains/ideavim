package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.statements.loops.ForLoop
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForLoopTests {

  @Test
  fun `for loop`() {
    val script = VimscriptParser.parse(
      """
            for key in keys(mydict)
                echo key . ':' . mydict(key)
            endfor
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is ForLoop)
    val f = script.units[0] as ForLoop
    assertEquals("key", f.variable)
    assertTrue(f.iterable is FunctionCallExpression)
    assertEquals(1, f.body.size)
  }
}
