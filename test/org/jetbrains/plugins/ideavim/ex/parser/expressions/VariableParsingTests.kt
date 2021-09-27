package expressions

import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VariableParsingTests {

  @Test
  fun variableTest() {
    val variable = VimscriptParser.parseExpression("variableName")
    assertTrue(variable is Variable)
    assertTrue(variable.scope == null)
    assertEquals("variableName", variable.name)
  }

  @Test
  fun variableTest2() {
    val variable = VimscriptParser.parseExpression("t:variableName")
    assertTrue(variable is Variable)
    assertEquals(Scope.TABPAGE_VARIABLE, variable.scope)
    assertEquals("variableName", variable.name)
  }
}
