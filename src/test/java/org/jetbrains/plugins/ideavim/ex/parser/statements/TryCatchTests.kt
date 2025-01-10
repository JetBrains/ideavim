/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.statements

import com.maddyhome.idea.vim.vimscript.model.statements.ThrowStatement
import com.maddyhome.idea.vim.vimscript.model.statements.TryStatement
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.productForArguments
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TryCatchTests : VimTestCase() {

  companion object {
    @JvmStatic
    val spaces = listOf("", " ")

    @JvmStatic
    val tryNames = listOf("try")

    @JvmStatic
    val catchNames = listOf("cat", "catc", "catch")

    @JvmStatic
    val finallyNames = listOf("fina", "final", "finall", "finally")

    @JvmStatic
    val endtryNames = listOf("endt", "endtr", "endtry")

    @JvmStatic
    fun combinations(): List<Arguments> =
      productForArguments(tryNames, catchNames, finallyNames, endtryNames, spaces, spaces, spaces, spaces)
  }

  @ParameterizedTest
  @MethodSource("combinations")
  fun `try catch finally with different names`(
    tryAlias: String,
    catchAlias: String,
    finallyAlias: String,
    endtryAlias: String,
    sp1: String,
    sp2: String,
    sp3: String,
    sp4: String,
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
      """.trimIndent(),
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
      """.trimIndent(),
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
      """.trimIndent(),
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
      """.trimIndent(),
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
      """.trimIndent(),
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
      """.trimIndent(),
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
      """.trimIndent(),
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
      """.trimIndent(),
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
      """.trimIndent(),
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
