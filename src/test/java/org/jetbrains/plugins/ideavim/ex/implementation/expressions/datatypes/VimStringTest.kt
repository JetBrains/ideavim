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

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.junit.Test
import kotlin.test.assertEquals

class VimStringTest {

  @Test
  fun `string as number`() {
    assertEquals(0.0, VimString("oh, hi Mark").asDouble())
  }

  @Test
  fun `string with zero as number`() {
    assertEquals(0.0, VimString("0oh, hi Mark").asDouble())
  }

  @Test
  fun `string with minus`() {
    assertEquals(0.0, VimString("-oh, hi Mark").asDouble())
  }

  @Test
  fun `string with minus zero`() {
    assertEquals(0.0, VimString("-0oh, hi Mark").asDouble())
  }

  @Test
  fun `string with float`() {
    assertEquals(4.0, VimString("4.67oh, hi Mark").asDouble())
  }

  @Test
  fun `string with digit`() {
    assertEquals(5.0, VimString("5oh, hi Mark").asDouble())
  }

  @Test
  fun `string with integer`() {
    assertEquals(53.0, VimString("53oh, hi Mark").asDouble())
  }

  @Test
  fun `string with negative integer`() {
    assertEquals(-1464.0, VimString("-1464 oh, hi Mark").asDouble())
  }

  @Test
  fun `string with octal number`() {
    assertEquals(83.0, VimString("0123 oh, hi Mark").asDouble())
  }

  @Test
  fun `string with negative octal number`() {
    assertEquals(-83.0, VimString("-0123 oh, hi Mark").asDouble())
  }

  @Test
  fun `string with octal number with multiple leading zeros`() {
    assertEquals(83.0, VimString("000123 oh, hi Mark").asDouble())
  }

  @Test
  fun `string with hex number`() {
    assertEquals(17.0, VimString("0x11 oh, hi Mark").asDouble())
  }

  @Test
  fun `string with negative hex number`() {
    assertEquals(-17.0, VimString("-0x11 oh, hi Mark").asDouble())
  }

  @Test
  fun `string with hex number with multiple leading zeroz`() {
    assertEquals(0.0, VimString("000x11 oh, hi Mark").asDouble())
  }

  @Test
  fun `string as boolean`() {
    assertEquals(false, VimString("oh, hi Mark").asBoolean())
  }

  @Test
  fun `string as boolean2`() {
    assertEquals(true, VimString("3oh, hi Mark").asBoolean())
  }
}
