/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.helper.StringHelper
import org.junit.Test
import kotlin.test.assertEquals

class DoubleQuotedStringTest {

  @Test
  fun `test three-digit octal number`() {
    assertEquals(StringHelper.parseVimString("\\162"), "r")
    assertEquals(StringHelper.parseVimString("\\162eturn"), "return")
    assertEquals(StringHelper.parseVimString("t\\162ee"), "tree")
  }

  @Test
  fun `test two-digit octal number`() {
    assertEquals(StringHelper.parseVimString("\\77"), "?")
    assertEquals(StringHelper.parseVimString("\\77!"), "?!")
    assertEquals(StringHelper.parseVimString("\\779"), "?9")
    assertEquals(StringHelper.parseVimString("hi\\77"), "hi?")
  }

  @Test
  fun `test one-digit octal number`() {
    assertEquals(StringHelper.parseVimString("\\7"), "" + 7.toChar())
    assertEquals(StringHelper.parseVimString("\\7!"), 7.toChar() + "!")
    assertEquals(StringHelper.parseVimString("\\79"), 7.toChar() + "9")
    assertEquals(StringHelper.parseVimString("hi\\7"), "hi" + 7.toChar())
  }

  @Test
  fun `test byte specified with two hex numbers`() {
    assertEquals(StringHelper.parseVimString("\\x"), "x")
    assertEquals(StringHelper.parseVimString("\\x1f"), "" + 31.toChar())
    assertEquals(StringHelper.parseVimString("\\x1fz"), 31.toChar() + "z")
    assertEquals(StringHelper.parseVimString("word\\x1f"), "word" + 31.toChar())

    assertEquals(StringHelper.parseVimString("\\X"), "X")
    assertEquals(StringHelper.parseVimString("\\X1f"), "" + 31.toChar())
    assertEquals(StringHelper.parseVimString("\\X1fz"), 31.toChar() + "z")
    assertEquals(StringHelper.parseVimString("word\\X1f"), "word" + 31.toChar())
  }

  @Test
  fun `test byte specified with one hex number`() {
    assertEquals(StringHelper.parseVimString("\\x"), "x")
    assertEquals(StringHelper.parseVimString("\\xf"), "" + 15.toChar())
    assertEquals(StringHelper.parseVimString("\\xfz"), 15.toChar() + "z")
    assertEquals(StringHelper.parseVimString("word\\xf"), "word" + 15.toChar())

    assertEquals(StringHelper.parseVimString("\\X"), "X")
    assertEquals(StringHelper.parseVimString("\\Xf"), "" + 15.toChar())
    assertEquals(StringHelper.parseVimString("\\Xfz"), 15.toChar() + "z")
    assertEquals(StringHelper.parseVimString("word\\Xf"), "word" + 15.toChar())
  }

  @Test
  fun `test up to 4 hex numbers`() {
    assertEquals(StringHelper.parseVimString("\\u"), "u")
    assertEquals(StringHelper.parseVimString("\\u7"), "" + 7.toChar())
    assertEquals(StringHelper.parseVimString("\\u72"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\u072"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\u0072"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\u00072"), 7.toChar() + "2")
  }

  @Test
  fun `test up to 8 hex numbers`() {
    assertEquals(StringHelper.parseVimString("\\U"), "U")
    assertEquals(StringHelper.parseVimString("\\U7"), "" + 7.toChar())
    assertEquals(StringHelper.parseVimString("\\U72"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\U072"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\U0072"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\U00072"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\U000072"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\U0000072"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\U00000072"), "" + 114.toChar())
    assertEquals(StringHelper.parseVimString("\\U000000072"), 7.toChar() + "2")
  }

  @Test
  fun `test escaped chars`() {
    assertEquals(StringHelper.parseVimString("\\r"), "\r")
    assertEquals(StringHelper.parseVimString("\\n"), "\n")
    assertEquals(StringHelper.parseVimString("\\f"), 12.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\e"), 27.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\t"), "\t")
    assertEquals(StringHelper.parseVimString("\\\\"), "\\")
    assertEquals(StringHelper.parseVimString("\\\""), "\"")
    assertEquals(StringHelper.parseVimString("\\b"), 8.toChar().toString())
  }

  @Test
  fun `test invalid char escaped`() {
    assertEquals(StringHelper.parseVimString("oh \\hi Mark"), "oh hi Mark")
  }

  @Test
  fun `test force end of the string`() {
    assertEquals(StringHelper.parseVimString("oh hi Mark\\0blabla"), "oh hi Mark")
    assertEquals(StringHelper.parseVimString("oh hi Mark\\x0 blabla"), "oh hi Mark")
    assertEquals(StringHelper.parseVimString("oh hi Mark\\x00 blabla"), "oh hi Mark")
    assertEquals(StringHelper.parseVimString("oh hi Mark\\X0 blabla"), "oh hi Mark")
    assertEquals(StringHelper.parseVimString("oh hi Mark\\X00 blabla"), "oh hi Mark")
    assertEquals(StringHelper.parseVimString("oh hi Mark\\u0 blabla"), "oh hi Mark")
    assertEquals(StringHelper.parseVimString("oh hi Mark\\U00 blabla"), "oh hi Mark")
  }

  @Test
  fun `test multiple escaped chars in a row`() {
    assertEquals(StringHelper.parseVimString("\\162\\u72\\U72\\x72\\X72\\n"), "rrrrr\n")
  }

  @Test
  fun `test special keys`() {
    assertEquals(StringHelper.parseVimString("\\<Esc>"), 27.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<Esk>"), "<Esk>")
    assertEquals(StringHelper.parseVimString("l\\<Esc>l"), "l" + 27.toChar() + "l")
    assertEquals(StringHelper.parseVimString("\\<Space>"), " ")
  }
}
