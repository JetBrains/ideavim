package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.expressions.EnvVariableExpression
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvVariableTests {

  @Test
  fun `environment variable test`() {
    val ex = VimscriptParser.parseExpression("\$JAVA_HOME")
    assertTrue(ex is EnvVariableExpression)
    assertEquals("JAVA_HOME", ex.variableName)
  }
}
