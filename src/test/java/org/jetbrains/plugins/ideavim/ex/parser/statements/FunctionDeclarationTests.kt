/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag
import com.maddyhome.idea.vim.vimscript.model.statements.ReturnStatement
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import com.maddyhome.idea.vim.vimscript.parser.errors.IdeavimErrorListener
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.productForArguments
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FunctionDeclarationTests : VimTestCase() {

  companion object {
    @JvmStatic
    val spaces = listOf("", " ")

    @JvmStatic
    val functionAlias = listOf("fu", "fun", "func", "funct", "functi", "functio", "function")

    @JvmStatic
    val endfunctionAlias =
      listOf("endf", "endfu", "endfun", "endfunc", "endfunct", "endfuncti", "endfunctio", "endfunction")

    @JvmStatic
    val flagAlias = listOf("range", "abort", "dict", "closure")

    @JvmStatic
    fun spaces5(): List<Arguments> = productForArguments(spaces, spaces, spaces, spaces, spaces)

    @JvmStatic
    fun spaces9(): List<Arguments> =
      productForArguments(spaces, spaces, spaces, spaces, spaces, spaces, spaces, spaces, spaces)

    @JvmStatic
    fun function(): List<Arguments> = productForArguments(functionAlias, endfunctionAlias, spaces, spaces)

    @JvmStatic
    fun flags(): List<Arguments> = productForArguments(flagAlias, spaces, spaces, spaces)

    @JvmStatic
    fun flagsFlags(): List<Arguments> = productForArguments(flagAlias, flagAlias, spaces, spaces, spaces)
  }

  @ParameterizedTest
  @MethodSource("spaces5")
  fun `function with no arguments`(sp1: String, sp2: String, sp3: String, sp4: String, sp5: String) {
    val script = VimscriptParser.parse(
      """
        function helloWorld$sp1($sp2)$sp3
            echo 'hello world'$sp4
        endfunction$sp5
      """.trimIndent(),
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

  @ParameterizedTest
  @MethodSource("spaces9")
  fun `function with arguments and replace flag`(
    sp1: String,
    sp2: String,
    sp3: String,
    sp4: String,
    sp5: String,
    sp6: String,
    sp7: String,
    sp8: String,
    sp9: String,
  ) {
    val script = VimscriptParser.parse(
      """
        " prefix with s: for local script-only functions
        function! s:Initialize$sp1(${sp2}cmd$sp3,${sp4}args$sp5)$sp6
            " a: prefix for arguments
            echo "Command: " . a:cmd
            $sp7
            return 'true'$sp8
        endfunction$sp9
      """.trimIndent(),
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

  @ParameterizedTest
  @MethodSource("function")
  fun `function keyword test`(
    functionAlias: String,
    endfunctionAlias: String,
    sp1: String,
    sp2: String,
  ) {
    val script = VimscriptParser.parse(
      """
        $functionAlias F1()$sp1
        $endfunctionAlias$sp2
      """.trimIndent(),
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is FunctionDeclaration)
  }

  @ParameterizedTest
  @MethodSource("flags")
  fun `function flag test`(
    flag1: String,
    sp1: String,
    sp2: String,
    sp3: String,
  ) {
    val script = VimscriptParser.parse(
      """
        fun F1()$sp1$flag1$sp2
        endf$sp3
      """.trimIndent(),
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is FunctionDeclaration)
    val f = script.units[0] as FunctionDeclaration
    assertEquals(f.flags, setOf(FunctionFlag.getByName(flag1)))
  }

  @ParameterizedTest
  @MethodSource("flagsFlags")
  fun `function with multiple flags test`(
    flag1: String,
    flag2: String,
    sp1: String,
    sp2: String,
    sp3: String,
  ) {
    val script = VimscriptParser.parse(
      """
        fun F1()$sp1$flag1 $flag2$sp2
        endf$sp3
      """.trimIndent(),
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is FunctionDeclaration)
    val f = script.units[0] as FunctionDeclaration
    assertEquals(f.flags, setOf(FunctionFlag.getByName(flag1), FunctionFlag.getByName(flag2)))
  }

  @org.junit.jupiter.api.Test
  fun `dictionary function`() {
    val script = VimscriptParser.parse(
      """
        " prefix with s: for local script-only functions
        function! s:dict.something.Initialize()
            return 'true'
        endfunction
      """.trimIndent(),
    )
    assertEquals(1, script.units.size)
  }

  // https://youtrack.jetbrains.com/issue/VIM-2654
  @Test
  fun `return with omitted expression`() {
    VimscriptParser.parse(
      """
        func! Paste_on_off()
           if g:paste_mode == 0
              set paste
              let g:paste_mode = 1
           else
              set nopaste
              let g:paste_mode = 0
           endif
           return
        endfunc
      """.trimIndent(),
    )
    assertEmpty(IdeavimErrorListener.testLogger)
  }
}
