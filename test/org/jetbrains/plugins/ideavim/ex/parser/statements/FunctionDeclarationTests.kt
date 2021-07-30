package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.ReturnStatement
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FunctionDeclarationTests {

  @Test
  fun `function with no arguments`() {
    val script = VimscriptParser.parse(
      """
            function helloWorld()
                echo 'hello world'
            endfunction
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is FunctionDeclaration)
    val f = script.units[0] as FunctionDeclaration
    assertNull(f.scope)
    assertEquals("helloWorld", f.name)
    assertEquals(0, f.args.size)
    assertEquals(1, f.body.size)
    assertFalse(f.replaceExisting)
    assertTrue(f.body[0] is EchoCommand)
  }

  @Test
  fun `function with arguments`() {
    val script = VimscriptParser.parse(
      """
            " prefix with s: for local script-only functions
            function! s:Initialize(cmd, args)
                " a: prefix for arguments
                echo "Command: " . a:cmd

                return 'true'
            endfunction
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is FunctionDeclaration)
    val f = script.units[0] as FunctionDeclaration
    assertEquals(Scope.SCRIPT_VARIABLE, f.scope)
    assertEquals("Initialize", f.name)
    assertEquals(listOf("cmd", "args"), f.args)
    assertEquals(2, f.body.size)
    assertTrue(f.replaceExisting)
    assertTrue(f.body[0] is EchoCommand)
    assertTrue(f.body[1] is ReturnStatement)
  }
}
