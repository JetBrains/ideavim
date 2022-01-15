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
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import javax.swing.KeyStroke.getKeyStroke
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

  @Test
  fun `test ctrl-char`() {
    assertEquals(StringHelper.parseVimString("\\<C-A>"), 1.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-B>"), 2.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-C>"), 3.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-D>"), 4.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-E>"), 5.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-F>"), 6.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-G>"), 7.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-H>"), 8.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-I>"), 9.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-J>"), 0.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-K>"), 11.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-L>"), 12.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-M>"), 13.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-N>"), 14.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-O>"), 15.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-P>"), 16.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-Q>"), 17.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-R>"), 18.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-S>"), 19.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-T>"), 20.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-U>"), 21.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-V>"), 22.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-W>"), 23.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-X>"), 24.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-Y>"), 25.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-Z>"), 26.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-[>"), 27.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-\\>"), 28.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-]>"), 29.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-^>"), 30.toChar().toString())
    assertEquals(StringHelper.parseVimString("\\<C-_>"), 31.toChar().toString())
  }

  @Test
  fun `test 1-31 keycodes`() {
    assertEquals(listOf(getKeyStroke(65, CTRL_DOWN_MASK)), StringHelper.stringToKeys(1.toChar().toString()))
    assertEquals(listOf(getKeyStroke(66, CTRL_DOWN_MASK)), StringHelper.stringToKeys(2.toChar().toString()))
    assertEquals(listOf(getKeyStroke(67, CTRL_DOWN_MASK)), StringHelper.stringToKeys(3.toChar().toString()))
    assertEquals(listOf(getKeyStroke(68, CTRL_DOWN_MASK)), StringHelper.stringToKeys(4.toChar().toString()))
    assertEquals(listOf(getKeyStroke(69, CTRL_DOWN_MASK)), StringHelper.stringToKeys(5.toChar().toString()))
    assertEquals(listOf(getKeyStroke(70, CTRL_DOWN_MASK)), StringHelper.stringToKeys(6.toChar().toString()))
    assertEquals(listOf(getKeyStroke(71, CTRL_DOWN_MASK)), StringHelper.stringToKeys(7.toChar().toString()))
    assertEquals(listOf(getKeyStroke(72, CTRL_DOWN_MASK)), StringHelper.stringToKeys(8.toChar().toString()))
    assertEquals(listOf(getKeyStroke(73, CTRL_DOWN_MASK)), StringHelper.stringToKeys(9.toChar().toString()))
    assertEquals(listOf(getKeyStroke(74, CTRL_DOWN_MASK)), StringHelper.stringToKeys(0.toChar().toString()))
    assertEquals(listOf(getKeyStroke(75, CTRL_DOWN_MASK)), StringHelper.stringToKeys(11.toChar().toString()))
    assertEquals(listOf(getKeyStroke(76, CTRL_DOWN_MASK)), StringHelper.stringToKeys(12.toChar().toString()))
    assertEquals(listOf(getKeyStroke(77, CTRL_DOWN_MASK)), StringHelper.stringToKeys(13.toChar().toString()))
    assertEquals(listOf(getKeyStroke(78, CTRL_DOWN_MASK)), StringHelper.stringToKeys(14.toChar().toString()))
    assertEquals(listOf(getKeyStroke(79, CTRL_DOWN_MASK)), StringHelper.stringToKeys(15.toChar().toString()))
    assertEquals(listOf(getKeyStroke(80, CTRL_DOWN_MASK)), StringHelper.stringToKeys(16.toChar().toString()))
    assertEquals(listOf(getKeyStroke(81, CTRL_DOWN_MASK)), StringHelper.stringToKeys(17.toChar().toString()))
    assertEquals(listOf(getKeyStroke(82, CTRL_DOWN_MASK)), StringHelper.stringToKeys(18.toChar().toString()))
    assertEquals(listOf(getKeyStroke(83, CTRL_DOWN_MASK)), StringHelper.stringToKeys(19.toChar().toString()))
    assertEquals(listOf(getKeyStroke(84, CTRL_DOWN_MASK)), StringHelper.stringToKeys(20.toChar().toString()))
    assertEquals(listOf(getKeyStroke(85, CTRL_DOWN_MASK)), StringHelper.stringToKeys(21.toChar().toString()))
    assertEquals(listOf(getKeyStroke(86, CTRL_DOWN_MASK)), StringHelper.stringToKeys(22.toChar().toString()))
    assertEquals(listOf(getKeyStroke(87, CTRL_DOWN_MASK)), StringHelper.stringToKeys(23.toChar().toString()))
    assertEquals(listOf(getKeyStroke(88, CTRL_DOWN_MASK)), StringHelper.stringToKeys(24.toChar().toString()))
    assertEquals(listOf(getKeyStroke(89, CTRL_DOWN_MASK)), StringHelper.stringToKeys(25.toChar().toString()))
    assertEquals(listOf(getKeyStroke(90, CTRL_DOWN_MASK)), StringHelper.stringToKeys(26.toChar().toString()))
    assertEquals(listOf(getKeyStroke(91, CTRL_DOWN_MASK)), StringHelper.stringToKeys(27.toChar().toString()))
    assertEquals(listOf(getKeyStroke(92, CTRL_DOWN_MASK)), StringHelper.stringToKeys(28.toChar().toString()))
    assertEquals(listOf(getKeyStroke(93, CTRL_DOWN_MASK)), StringHelper.stringToKeys(29.toChar().toString()))
    assertEquals(listOf(getKeyStroke(94, CTRL_DOWN_MASK)), StringHelper.stringToKeys(30.toChar().toString()))
    assertEquals(listOf(getKeyStroke(95, CTRL_DOWN_MASK)), StringHelper.stringToKeys(31.toChar().toString()))
  }
}
