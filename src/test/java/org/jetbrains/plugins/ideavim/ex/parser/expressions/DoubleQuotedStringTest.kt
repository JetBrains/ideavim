/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.vimKeyStroke
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import javax.swing.KeyStroke.getKeyStroke
import kotlin.test.assertEquals

class DoubleQuotedStringTest : VimTestCase() {

  @Test
  fun `test three-digit octal number`() {
    assertEquals(injector.parser.parseVimScriptString("\\162"), "r")
    assertEquals(injector.parser.parseVimScriptString("\\162eturn"), "return")
    assertEquals(injector.parser.parseVimScriptString("t\\162ee"), "tree")
  }

  @Test
  fun `test two-digit octal number`() {
    assertEquals(injector.parser.parseVimScriptString("\\77"), "?")
    assertEquals(injector.parser.parseVimScriptString("\\77!"), "?!")
    assertEquals(injector.parser.parseVimScriptString("\\779"), "?9")
    assertEquals(injector.parser.parseVimScriptString("hi\\77"), "hi?")
  }

  @Test
  fun `test one-digit octal number`() {
    assertEquals(injector.parser.parseVimScriptString("\\7"), "" + 7.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\7!"), 7.toChar() + "!")
    assertEquals(injector.parser.parseVimScriptString("\\79"), 7.toChar() + "9")
    assertEquals(injector.parser.parseVimScriptString("hi\\7"), "hi" + 7.toChar())
  }

  @Test
  fun `test byte specified with two hex numbers`() {
    assertEquals(injector.parser.parseVimScriptString("\\x"), "x")
    assertEquals(injector.parser.parseVimScriptString("\\x1f"), "" + 31.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\x1fz"), 31.toChar() + "z")
    assertEquals(injector.parser.parseVimScriptString("word\\x1f"), "word" + 31.toChar())

    assertEquals(injector.parser.parseVimScriptString("\\X"), "X")
    assertEquals(injector.parser.parseVimScriptString("\\X1f"), "" + 31.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\X1fz"), 31.toChar() + "z")
    assertEquals(injector.parser.parseVimScriptString("word\\X1f"), "word" + 31.toChar())
  }

  @Test
  fun `test byte specified with one hex number`() {
    assertEquals(injector.parser.parseVimScriptString("\\x"), "x")
    assertEquals(injector.parser.parseVimScriptString("\\xf"), "" + 15.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\xfz"), 15.toChar() + "z")
    assertEquals(injector.parser.parseVimScriptString("word\\xf"), "word" + 15.toChar())

    assertEquals(injector.parser.parseVimScriptString("\\X"), "X")
    assertEquals(injector.parser.parseVimScriptString("\\Xf"), "" + 15.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\Xfz"), 15.toChar() + "z")
    assertEquals(injector.parser.parseVimScriptString("word\\Xf"), "word" + 15.toChar())
  }

  @Test
  fun `test up to 4 hex numbers`() {
    assertEquals(injector.parser.parseVimScriptString("\\u"), "u")
    assertEquals(injector.parser.parseVimScriptString("\\u7"), "" + 7.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\u72"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\u072"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\u0072"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\u00072"), 7.toChar() + "2")
  }

  @Test
  fun `test up to 8 hex numbers`() {
    assertEquals(injector.parser.parseVimScriptString("\\U"), "U")
    assertEquals(injector.parser.parseVimScriptString("\\U7"), "" + 7.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\U72"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\U072"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\U0072"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\U00072"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\U000072"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\U0000072"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\U00000072"), "" + 114.toChar())
    assertEquals(injector.parser.parseVimScriptString("\\U000000072"), 7.toChar() + "2")
  }

  @Test
  fun `test escaped chars`() {
    assertEquals(injector.parser.parseVimScriptString("\\r"), "\r")
    assertEquals(injector.parser.parseVimScriptString("\\n"), "\n")
    assertEquals(injector.parser.parseVimScriptString("\\f"), 12.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\e"), 27.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\t"), "\t")
    assertEquals(injector.parser.parseVimScriptString("\\\\"), "\\")
    assertEquals(injector.parser.parseVimScriptString("\\\""), "\"")
    assertEquals(injector.parser.parseVimScriptString("\\b"), 8.toChar().toString())
  }

  @Test
  fun `test invalid char escaped`() {
    assertEquals(injector.parser.parseVimScriptString("oh \\hi Mark"), "oh hi Mark")
  }

  @Test
  fun `test force end of the string`() {
    assertEquals(injector.parser.parseVimScriptString("oh hi Mark\\0blabla"), "oh hi Mark")
    assertEquals(injector.parser.parseVimScriptString("oh hi Mark\\x0 blabla"), "oh hi Mark")
    assertEquals(injector.parser.parseVimScriptString("oh hi Mark\\x00 blabla"), "oh hi Mark")
    assertEquals(injector.parser.parseVimScriptString("oh hi Mark\\X0 blabla"), "oh hi Mark")
    assertEquals(injector.parser.parseVimScriptString("oh hi Mark\\X00 blabla"), "oh hi Mark")
    assertEquals(injector.parser.parseVimScriptString("oh hi Mark\\u0 blabla"), "oh hi Mark")
    assertEquals(injector.parser.parseVimScriptString("oh hi Mark\\U00 blabla"), "oh hi Mark")
  }

  @Test
  fun `test multiple escaped chars in a row`() {
    assertEquals(injector.parser.parseVimScriptString("\\162\\u72\\U72\\x72\\X72\\n"), "rrrrr\n")
  }

  @Test
  fun `test special keys`() {
    assertEquals(injector.parser.parseVimScriptString("\\<Esc>"), 27.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<Esk>"), "<Esk>")
    assertEquals(injector.parser.parseVimScriptString("l\\<Esc>l"), "l" + 27.toChar() + "l")
    assertEquals(injector.parser.parseVimScriptString("\\<Space>"), " ")
  }

  @Test
  fun `test ctrl-char`() {
    assertEquals(injector.parser.parseVimScriptString("\\<C-A>"), 1.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-B>"), 2.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-C>"), 3.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-D>"), 4.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-E>"), 5.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-F>"), 6.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-G>"), 7.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-H>"), 8.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-I>"), 9.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-J>"), 0.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-K>"), 11.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-L>"), 12.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-M>"), 13.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-N>"), 14.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-O>"), 15.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-P>"), 16.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-Q>"), 17.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-R>"), 18.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-S>"), 19.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-T>"), 20.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-U>"), 21.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-V>"), 22.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-W>"), 23.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-X>"), 24.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-Y>"), 25.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-Z>"), 26.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-[>"), 27.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-\\>"), 28.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-]>"), 29.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-^>"), 30.toChar().toString())
    assertEquals(injector.parser.parseVimScriptString("\\<C-_>"), 31.toChar().toString())
  }

  @Test
  fun `test 1-31 keycodes`() {
    assertEquals(listOf(getKeyStroke(65, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(1.toChar().toString()))
    assertEquals(listOf(getKeyStroke(66, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(2.toChar().toString()))
    assertEquals(listOf(getKeyStroke(67, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(3.toChar().toString()))
    assertEquals(listOf(getKeyStroke(68, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(4.toChar().toString()))
    assertEquals(listOf(getKeyStroke(69, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(5.toChar().toString()))
    assertEquals(listOf(getKeyStroke(70, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(6.toChar().toString()))
    assertEquals(listOf(getKeyStroke(71, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(7.toChar().toString()))
    assertEquals(listOf(getKeyStroke(72, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(8.toChar().toString()))
    assertEquals(listOf(getKeyStroke('\t').vimKeyStroke), injector.parser.stringToKeys(9.toChar().toString()))
    assertEquals(listOf(getKeyStroke(74, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(0.toChar().toString()))
    assertEquals(listOf(getKeyStroke(75, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(11.toChar().toString()))
    assertEquals(listOf(getKeyStroke(76, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(12.toChar().toString()))
    assertEquals(listOf(getKeyStroke(77, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(13.toChar().toString()))
    assertEquals(listOf(getKeyStroke(78, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(14.toChar().toString()))
    assertEquals(listOf(getKeyStroke(79, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(15.toChar().toString()))
    assertEquals(listOf(getKeyStroke(80, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(16.toChar().toString()))
    assertEquals(listOf(getKeyStroke(81, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(17.toChar().toString()))
    assertEquals(listOf(getKeyStroke(82, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(18.toChar().toString()))
    assertEquals(listOf(getKeyStroke(83, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(19.toChar().toString()))
    assertEquals(listOf(getKeyStroke(84, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(20.toChar().toString()))
    assertEquals(listOf(getKeyStroke(85, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(21.toChar().toString()))
    assertEquals(listOf(getKeyStroke(86, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(22.toChar().toString()))
    assertEquals(listOf(getKeyStroke(87, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(23.toChar().toString()))
    assertEquals(listOf(getKeyStroke(88, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(24.toChar().toString()))
    assertEquals(listOf(getKeyStroke(89, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(25.toChar().toString()))
    assertEquals(listOf(getKeyStroke(90, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(26.toChar().toString()))
    assertEquals(listOf(getKeyStroke(91, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(27.toChar().toString()))
    assertEquals(listOf(getKeyStroke(92, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(28.toChar().toString()))
    assertEquals(listOf(getKeyStroke(93, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(29.toChar().toString()))
    assertEquals(listOf(getKeyStroke(94, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(30.toChar().toString()))
    assertEquals(listOf(getKeyStroke(95, CTRL_DOWN_MASK).vimKeyStroke), injector.parser.stringToKeys(31.toChar().toString()))
  }
}
