/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

class VimStringTest {

  @Test
  fun `string as number`() {
    assertEquals(0, VimString("oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with zero as number`() {
    assertEquals(0, VimString("0oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with minus`() {
    assertEquals(0, VimString("-oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with minus zero`() {
    assertEquals(0, VimString("-0oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with float`() {
    assertEquals(4, VimString("4.67oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with digit`() {
    assertEquals(5, VimString("5oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with integer`() {
    assertEquals(53, VimString("53oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with negative integer`() {
    assertEquals(-1464, VimString("-1464 oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with octal number`() {
    assertEquals(83, VimString("0123 oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with negative octal number`() {
    assertEquals(-83, VimString("-0123 oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with octal number with multiple leading zeros`() {
    assertEquals(83, VimString("000123 oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with hex number`() {
    assertEquals(17, VimString("0x11 oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with negative hex number`() {
    assertEquals(-17, VimString("-0x11 oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string with hex number with multiple leading zeroes`() {
    assertEquals(0, VimString("000x11 oh, hi Mark").toVimNumber().value)
  }

  @Test
  fun `string as boolean`() {
    assertEquals(false, VimString("oh, hi Mark").toVimNumber().booleanValue)
  }

  @Test
  fun `string as boolean2`() {
    assertEquals(true, VimString("3oh, hi Mark").toVimNumber().booleanValue)
  }

  @Test
  fun `test string has value semantics`() {
    assertEquals(VimString("foo"), VimString("foo"))
    assertEquals(VimString("foo").hashCode(), VimString("foo").hashCode())

    assertNotEquals(VimString("foo"), VimString("bar"))
    assertNotEquals(VimString("foo").hashCode(), VimString("bar").hashCode())

    assertNotEquals(VimString("foo"), VimString("FOO"))
    assertNotEquals(VimString("foo").hashCode(), VimString("FOO").hashCode())
  }

  @Test
  fun `test copy returns new instance with same value`() {
    // We get a new instance of the VimString, but it's fine to have the same instance of the String
    val value = VimString("hello world")
    val copy = value.copy()
    assertNotSame(value, copy)
    assertEquals(value.value, copy.value)
  }
}
