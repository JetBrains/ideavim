/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.commands

import com.maddyhome.idea.vim.ex.ranges.LineNumberRange
import com.maddyhome.idea.vim.ex.ranges.MarkRange
import com.maddyhome.idea.vim.vimscript.model.commands.BufferCommand
import com.maddyhome.idea.vim.vimscript.model.commands.DeleteLinesCommand
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PlugCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SetCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SplitCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SplitType
import com.maddyhome.idea.vim.vimscript.model.commands.SubstituteCommand
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.BinExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Theories::class)
class CommandTests {

  companion object {
    @JvmStatic
    val values = listOf("", " ") @DataPoints get
  }

  @Theory
  fun `let command`() {
    val c = VimscriptParser.parseCommand("let g:catSound='Meow'")
    assertTrue(c is LetCommand)
    assertEquals(Variable(Scope.GLOBAL_VARIABLE, "catSound"), c.variable)
    assertEquals(SimpleExpression("Meow"), c.expression)
  }

  @Theory
  fun `echo command`() {
    val c = VimscriptParser.parseCommand("echo 4 5+7 'hi doggy'")
    assertTrue(c is EchoCommand)
    val expressions = c.args
    assertEquals(3, expressions.size)
    assertTrue(expressions[0] is SimpleExpression)
    assertEquals(VimInt(4), (expressions[0] as SimpleExpression).data)
    assertTrue(expressions[1] is BinExpression)
    assertEquals(VimInt(12), expressions[1].evaluate())
    assertTrue(expressions[2] is SimpleExpression)
    assertEquals(VimString("hi doggy"), (expressions[2] as SimpleExpression).data)
  }

  // VIM-2426
  @Theory
  fun `command with marks in range`(sp: String) {
    val command = VimscriptParser.parseCommand("'a,'b${sp}s/a/b/g")
    assertTrue(command is SubstituteCommand)
    assertEquals("s", command.command)
    assertEquals("/a/b/g", command.argument)
    assertEquals(2, command.ranges.size())
    assertEquals(MarkRange('a', 0, false), command.ranges.ranges[0])
    assertEquals(MarkRange('b', 0, false), command.ranges.ranges[1])
  }

  // https://github.com/JetBrains/ideavim/discussions/386
  @Theory
  fun `no space between command and argument`(sp: String) {
    val command = VimscriptParser.parseCommand("b${sp}1")
    assertTrue(command is BufferCommand)
    assertEquals("1", command.argument)
  }

  // VIM-2445
  @Theory
  fun `spaces in range`(sp1: String, sp2: String, sp3: String) {
    val command = VimscriptParser.parseCommand("10$sp1,${sp2}20${sp3}d")
    assertTrue(command is DeleteLinesCommand)
    assertEquals(2, command.ranges.size())
    assertEquals(LineNumberRange(9, 0, false), command.ranges.ranges[0])
    assertEquals(LineNumberRange(19, 0, false), command.ranges.ranges[1])
  }

  // VIM-2450
  @Theory
  fun `set command`() {
    val command = VimscriptParser.parseCommand("se nonu")
    assertTrue(command is SetCommand)
    assertEquals("nonu", command.argument)
  }

  // VIM-2453
  @Theory
  fun `split command`() {
    val command = VimscriptParser.parseCommand("sp")
    assertTrue(command is SplitCommand)
    assertEquals(SplitType.HORIZONTAL, command.splitType)
  }

  // VIM-2452
  fun `augroup test`() {
    // augusto was recognized as AUGROUP token ('au') and all the lines were ignored
    val script = VimscriptParser.parse(
      """
        Plug 'danilo-augusto/vim-afterglow'
        set nu rnu

        augroup myCmds
        augroup END
      """.trimIndent()
    )
    assertEquals(2, script.units.size)
    assertTrue(script.units[0] is PlugCommand)
    assertTrue(script.units[1] is SetCommand)
  }

  fun `augroup test 2`() {
    val script = VimscriptParser.parse(
      """
        augroup myCmds
          au smthing
        augroup END
        
        Plug 'danilo-augusto/vim-afterglow'
        set nu rnu
      """.trimIndent()
    )
    assertEquals(2, script.units.size)
    assertTrue(script.units[0] is PlugCommand)
    assertTrue(script.units[1] is SetCommand)
  }
}
