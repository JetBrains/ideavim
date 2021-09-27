package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.ListExpression
import com.maddyhome.idea.vim.vimscript.model.statements.loops.ForLoop
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
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
    assertEquals("key", f.variable)
    assertTrue(f.iterable is FunctionCallExpression)
    assertEquals(1, f.body.size)
  }

  @Theory
  fun `empty for loop`(sp1: String, sp2: String, sp3: String) {
    val script = VimscriptParser.parse(
      """
        for key in [1, 2, 3]$sp1
        endfor$sp2
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is ForLoop)
    val f = script.units[0] as ForLoop
    assertEquals("key", f.variable)
    assertTrue(f.iterable is ListExpression)
    assertEquals(0, f.body.size)
  }

  @Theory
  fun `unknown for loop type`(sp1: String, sp2: String, sp3: String) {
    val script = VimscriptParser.parse(
      """
        for [key, value] in dict$sp1
        endfor$sp2
      """.trimIndent()
    )
    // it will be implemented later but for now it's good to ignore such blocks and do not throw any exceptions during parsing
    assertEquals(0, script.units.size)
  }
}
