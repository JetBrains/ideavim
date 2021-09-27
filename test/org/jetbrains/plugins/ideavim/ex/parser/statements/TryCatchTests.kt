/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.statements.ThrowStatement
import com.maddyhome.idea.vim.vimscript.model.statements.TryStatement
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.FromDataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(Theories::class)
class TryCatchTests {

  companion object {
    @JvmStatic
    val spaces = listOf("", " ")
      @DataPoints("spaces") get

    @JvmStatic
    val tryNames = listOf("try")
      @DataPoints("try") get

    @JvmStatic
    val catchNames = listOf("cat", "catc", "catch")
      @DataPoints("catch") get

    @JvmStatic
    val finallyNames = listOf("fina", "final", "finall", "finally")
      @DataPoints("finally") get

    @JvmStatic
    val endtryNames = listOf("endt", "endtr", "endtry")
      @DataPoints("endtry") get
  }

  @Theory
  fun `try catch finally with different names`(
    @FromDataPoints("try") tryAlias: String,
    @FromDataPoints("catch") catchAlias: String,
    @FromDataPoints("finally") finallyAlias: String,
    @FromDataPoints("endtry") endtryAlias: String,
    @FromDataPoints("spaces") sp1: String,
    @FromDataPoints("spaces") sp2: String,
    @FromDataPoints("spaces") sp3: String,
    @FromDataPoints("spaces") sp4: String
  ) {
    val script = VimscriptParser.parse(
      """
        $tryAlias$sp1
          throw 'something'
        $catchAlias$sp2
          echo 'caught'
        $finallyAlias$sp3
          echo 'finalizing'
        $endtryAlias$sp4
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is TryStatement)
    val ts = script.units[0] as TryStatement
    assertEquals(1, ts.tryBlock.body.size)
    assertEquals(1, ts.catchBlocks.size)
    assertEquals(1, ts.catchBlocks[0].body.size)
    assertNotNull(ts.finallyBlock)
    assertEquals(1, ts.finallyBlock!!.body.size)
  }

  @Test
  fun `try block only`() {
    val script = VimscriptParser.parse(
      """
        try
          throw 'something'
        endtry
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is TryStatement)
    val ts = script.units[0] as TryStatement
    assertEquals(1, ts.tryBlock.body.size)
    assertTrue(ts.tryBlock.body[0] is ThrowStatement)
    assertTrue(ts.catchBlocks.isEmpty())
    assertNull(ts.finallyBlock)
  }

  @Test
  fun `try and catch block only`() {
    val script = VimscriptParser.parse(
      """
        try
          throw 'something'
        catch /some/
          echo 'caught'
        endtry
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is TryStatement)
    val ts = script.units[0] as TryStatement
    assertEquals(1, ts.tryBlock.body.size)
    assertTrue(ts.tryBlock.body[0] is ThrowStatement)
    assertEquals(1, ts.catchBlocks.size)
    assertEquals("some", ts.catchBlocks[0].pattern)
    assertEquals(1, ts.catchBlocks[0].body.size)
    assertNull(ts.finallyBlock)
  }

  @Test
  fun `catch block without pattern`() {
    val script = VimscriptParser.parse(
      """
        try
          throw 'something'
        catch
          echo 'caught'
        endtry
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is TryStatement)
    val ts = script.units[0] as TryStatement
    assertEquals(1, ts.tryBlock.body.size)
    assertTrue(ts.tryBlock.body[0] is ThrowStatement)
    assertEquals(1, ts.catchBlocks.size)
    assertEquals(".", ts.catchBlocks[0].pattern)
    assertEquals(1, ts.catchBlocks[0].body.size)
    assertNull(ts.finallyBlock)
  }

  @Test
  fun `try and finally block only`() {
    val script = VimscriptParser.parse(
      """
        try
          throw 'something'
        finally
          echo 'caught'
        endtry
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is TryStatement)
    val ts = script.units[0] as TryStatement
    assertEquals(1, ts.tryBlock.body.size)
    assertTrue(ts.catchBlocks.isEmpty())
    assertTrue(ts.tryBlock.body[0] is ThrowStatement)
    assertNotNull(ts.finallyBlock)
    assertEquals(1, ts.finallyBlock!!.body.size)
  }

  @Test
  fun `multiple catch blocks`() {
    val script = VimscriptParser.parse(
      """
        try
          throw 'something'
        catch /1/
          echo 'caught1'
        catch /2/
          echo 'caught2'
          echo 'caught2'
        catch /3/
          echo 'caught3'
          echo 'caught3'
          echo 'caught3'
        endtry
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is TryStatement)
    val ts = script.units[0] as TryStatement
    assertEquals(1, ts.tryBlock.body.size)
    assertTrue(ts.tryBlock.body[0] is ThrowStatement)
    assertEquals(3, ts.catchBlocks.size)
    assertEquals("1", ts.catchBlocks[0].pattern)
    assertEquals(1, ts.catchBlocks[0].body.size)
    assertEquals("2", ts.catchBlocks[1].pattern)
    assertEquals(2, ts.catchBlocks[1].body.size)
    assertEquals("3", ts.catchBlocks[2].pattern)
    assertEquals(3, ts.catchBlocks[2].body.size)
    assertNull(ts.finallyBlock)
  }

  @Test
  fun `empty try block`() {
    val script = VimscriptParser.parse(
      """
        try
        endtry
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is TryStatement)
    val ts = script.units[0] as TryStatement
    assertEquals(0, ts.tryBlock.body.size)
    assertTrue(ts.catchBlocks.isEmpty())
    assertNull(ts.finallyBlock)
  }

  @Test
  fun `empty catch block`() {
    val script = VimscriptParser.parse(
      """
        try
          throw 'something'
        catch
        endtry
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is TryStatement)
    val ts = script.units[0] as TryStatement
    assertEquals(1, ts.tryBlock.body.size)
    assertTrue(ts.tryBlock.body[0] is ThrowStatement)
    assertEquals(1, ts.catchBlocks.size)
    assertEquals(".", ts.catchBlocks[0].pattern)
    assertEquals(0, ts.catchBlocks[0].body.size)
    assertNull(ts.finallyBlock)
  }

  @Test
  fun `empty finally block`() {
    val script = VimscriptParser.parse(
      """
        try
          throw 'something'
        finally
        endtry
      """.trimIndent()
    )
    assertEquals(1, script.units.size)
    assertTrue(script.units[0] is TryStatement)
    val ts = script.units[0] as TryStatement
    assertEquals(1, ts.tryBlock.body.size)
    assertTrue(ts.catchBlocks.isEmpty())
    assertTrue(ts.tryBlock.body[0] is ThrowStatement)
    assertNotNull(ts.finallyBlock)
    assertEquals(0, ts.finallyBlock!!.body.size)
  }
}
