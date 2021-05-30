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

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class MotionPercentOrMatchActionTest : VimTestCase() {
  fun `test percent match simple`() {
    typeTextInFile(
      parseKeys("%"),
      "foo(b${c}ar)\n"
    )
    assertOffset(3)
  }

  fun `test percent match multi line`() {
    typeTextInFile(
      parseKeys("%"),
      """foo(bar,
                     |baz,
                     |${c}quux)
               """.trimMargin()
    )
    assertOffset(3)
  }

  fun `test percent visual mode match multi line end of line`() {
    typeTextInFile(
      parseKeys("v$%"),
      """${c}foo(
                  |bar)""".trimMargin()
    )
    assertOffset(8)
  }

  fun `test percent visual mode match from start multi line end of line`() {
    typeTextInFile(
      parseKeys("v$%"),
      """$c(
                  |bar)""".trimMargin()
    )
    assertOffset(5)
  }

  fun `test percent visual mode find brackets on the end of line`() {
    typeTextInFile(
      parseKeys("v$%"),
      """foo(${c}bar)"""
    )
    assertOffset(3)
  }

  fun `test percent twice visual mode find brackets on the end of line`() {
    typeTextInFile(
      parseKeys("v$%%"),
      """foo(${c}bar)"""
    )
    assertOffset(7)
  }

  fun `test percent match parens in string`() {
    typeTextInFile(
      parseKeys("%"),
      """foo(bar, "foo(bar", ${c}baz)
               """
    )
    assertOffset(3)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test percent match xml comment start`() {
    configureByXmlText("$c<!-- foo -->")
    typeText(parseKeys("%"))
    assertState("<!-- foo --$c>")
  }

  fun `test percent doesnt match partial xml comment`() {
    configureByXmlText("<!$c-- ")
    typeText(parseKeys("%"))
    assertState("<!$c-- ")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test percent match xml comment end`() {
    configureByXmlText("<!-- foo --$c>")
    typeText(parseKeys("%"))
    assertState("$c<!-- foo -->")
  }

  fun `test percent match java comment start`() {
    configureByJavaText("/$c* foo */")
    typeText(parseKeys("%"))
    assertState("/* foo *$c/")
  }

  fun `test percent doesnt match partial java comment`() {
    configureByJavaText("$c/* ")
    typeText(parseKeys("%"))
    assertState("$c/* ")
  }

  fun `test percent match java comment end`() {
    configureByJavaText("/* foo $c*/")
    typeText(parseKeys("%"))
    assertState("$c/* foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test percent match java doc comment start`() {
    configureByJavaText("/*$c* foo */")
    typeText(parseKeys("%"))
    assertState("/** foo *$c/")
  }

  fun `test percent match java doc comment end`() {
    configureByJavaText("/** foo *$c/")
    typeText(parseKeys("%"))
    assertState("$c/** foo */")
  }

  fun `test percent doesnt match after comment start`() {
    configureByJavaText("/*$c foo */")
    typeText(parseKeys("%"))
    assertState("/*$c foo */")
  }

  fun `test percent doesnt match before comment end`() {
    configureByJavaText("/* foo $c */")
    typeText(parseKeys("%"))
    assertState("/* foo $c */")
  }

  fun `test motion with quote on the way`() {
    doTest(
      "%",
      """
            for (; c!= cj;c = it.next()) $c{
             if (dsa) {
               if (c == '\\') {
                 dsadsakkk
               }
             }
            }
      """.trimIndent(),
      """
            for (; c!= cj;c = it.next()) {
             if (dsa) {
               if (c == '\\') {
                 dsadsakkk
               }
             }
            $c}
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test motion outside text`() {
    doTest(
      "%",
      """
            (
            ""${'"'}
            ""${'"'} + ${c}title("Display")
            ""${'"'}
            ""${'"'}
            )
      """.trimIndent(),
      """
            (
            ""${'"'}
            ""${'"'} + title("Display"$c)
            ""${'"'}
            ""${'"'}
            )
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test motion in text`() {
    doTest(
      "%", """ "I found ${c}it in a (legendary) land" """,
      """ "I found it in a (legendary$c) land" """, CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test motion in text with quotes`() {
    doTest(
      "%", """ "I found ${c}it in \"a (legendary) land" """,
      """ "I found it in \"a (legendary$c) land" """, CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test motion in text with quotes start before quote`() {
    doTest(
      "%", """ $c "I found it in \"a (legendary) land" """,
      """  "I found it in \"a (legendary$c) land" """, CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test motion in text with quotes and double escape`() {
    doTest(
      "%", """ "I found ${c}it in \\\"a (legendary) land" """,
      """ "I found it in \\\"a (legendary$c) land" """, CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test deleting with percent motion backward`() {
    doTest("d%", "(foo bar$c)", c, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test deleting with percent motion`() {
    doTest("d%", "$c(foo bar)", c, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test count percent moves to line as percentage of file height`() {
    configureByLines(100, "    I found it in a legendary land")
    typeText(parseKeys("25%"))
    assertPosition(24, 4)
  }

  fun `test count percent moves to line as percentage of file height 2`() {
    configureByLines(50, "    I found it in a legendary land")
    typeText(parseKeys("25%"))
    assertPosition(12, 4)
  }

  fun `test count percent moves to line as percentage of file height 3`() {
    configureByLines(17, "    I found it in a legendary land")
    typeText(parseKeys("25%"))
    assertPosition(4, 4)
  }

  fun `test count percent keeps same column with nostartline`() {
    OptionsManager.startofline.reset()
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(0, 0, 14)
    typeText(parseKeys("25%"))
    assertPosition(24, 14)
  }

  fun `test count percent handles shorter line with nostartline`() {
    OptionsManager.startofline.reset()
    configureByLines(100, "    I found it in a legendary land")
    typeText(parseKeys("A", " extra text", "<Esc>"))
    typeText(parseKeys("25%"))
    assertPosition(24, 33)
  }
}
