/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class MotionPercentOrMatchActionTest : VimTestCase() {
  fun `test percent match simple`() {
    typeTextInFile(
      "%",
      "foo(b${c}ar)\n"
    )
    assertOffset(3)
  }

  fun `test percent match multi line`() {
    typeTextInFile(
      "%",
      """
        |foo(bar,
        |baz,
        |${c}quux)
      """.trimMargin()
    )
    assertOffset(3)
  }

  fun `test percent visual mode match multi line end of line`() {
    typeTextInFile(
      "v$%",
      """
        |${c}foo(
        |bar)
      """.trimMargin()
    )
    assertOffset(8)
  }

  fun `test percent visual mode match from start multi line end of line`() {
    typeTextInFile(
      "v$%",
      """
        |$c(
        |bar)
      """.trimMargin()
    )
    assertOffset(5)
  }

  fun `test percent visual mode find brackets on the end of line`() {
    typeTextInFile(
      "v$%",
      """foo(${c}bar)"""
    )
    assertOffset(3)
  }

  fun `test percent twice visual mode find brackets on the end of line`() {
    typeTextInFile(
      "v$%%",
      """foo(${c}bar)"""
    )
    assertOffset(7)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN, description = "Matchit plugin affects neovim")
  fun `test percent match parens in string`() {
    typeTextInFile(
      "%",
      """foo(bar, "foo(bar", ${c}baz)
               """
    )
    assertOffset(3)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test percent match xml comment start`() {
    configureByXmlText("$c<!-- foo -->")
    typeText("%")
    assertState("<!-- foo --$c>")
  }

  fun `test percent doesnt match partial xml comment`() {
    configureByXmlText("<!$c-- ")
    typeText("%")
    assertState("<!$c-- ")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test percent match xml comment end`() {
    configureByXmlText("<!-- foo --$c>")
    typeText("%")
    assertState("$c<!-- foo -->")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN, description = "Matchit plugin affects neovim")
  fun `test percent match java comment start`() {
    configureByJavaText("/$c* foo */")
    typeText("%")
    assertState("/* foo *$c/")
  }

  fun `test percent doesnt match partial java comment`() {
    configureByJavaText("$c/* ")
    typeText("%")
    assertState("$c/* ")
  }

  fun `test percent match java comment end`() {
    configureByJavaText("/* foo $c*/")
    typeText("%")
    assertState("$c/* foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test percent match java doc comment start`() {
    configureByJavaText("/*$c* foo */")
    typeText("%")
    assertState("/** foo *$c/")
  }

  fun `test percent match java doc comment end`() {
    configureByJavaText("/** foo *$c/")
    typeText("%")
    assertState("$c/** foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN, description = "Matchit plugin affects neovim")
  fun `test percent doesnt match after comment start`() {
    configureByJavaText("/*$c foo */")
    typeText("%")
    assertState("/*$c foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  fun `test percent doesnt match before comment end`() {
    configureByJavaText("/* foo $c */")
    typeText("%")
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion in text`() {
    doTest(
      "%", """ "I found ${c}it in a (legendary) land" """,
      """ "I found it in a (legendary$c) land" """, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion in text with quotes`() {
    doTest(
      "%", """ "I found ${c}it in \"a (legendary) land" """,
      """ "I found it in \"a (legendary$c) land" """, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion in text with quotes start before quote`() {
    doTest(
      "%", """ $c "I found it in \"a (legendary) land" """,
      """  "I found it in \"a (legendary$c) land" """, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion in text with quotes and double escape`() {
    doTest(
      "%", """ "I found ${c}it in \\\"a (legendary) land" """,
      """ "I found it in \\\"a (legendary$c) land" """, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion in text with escape (outer forward)`() {
    doTest(
      "%", """ debugPrint$c(\(var)) """,
      """ debugPrint(\(var)$c) """, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion in text with escape (outer backward)`() {
    doTest(
      "%", """ debugPrint(\(var)$c) """,
      """ debugPrint$c(\(var)) """, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion in text with escape (inner forward)`() {
    doTest(
      "%", """ debugPrint(\$c(var)) """,
      """ debugPrint(\(var$c)) """, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion in text with escape (inner backward)`() {
    doTest(
      "%", """ debugPrint(\$c(var)) """,
      """ debugPrint(\(var$c)) """, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN, description = "Matchit plugin affects neovim")
  fun `test deleting with percent motion backward`() {
    doTest("d%", "(foo bar$c)", c, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN, description = "Matchit plugin affects neovim")
  fun `test deleting with percent motion`() {
    doTest("d%", "$c(foo bar)", c, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test count percent moves to line as percentage of file height`() {
    configureByLines(100, "    I found it in a legendary land")
    typeText("25%")
    assertPosition(24, 4)
  }

  fun `test count percent moves to line as percentage of file height 2`() {
    configureByLines(50, "    I found it in a legendary land")
    typeText("25%")
    assertPosition(12, 4)
  }

  fun `test count percent moves to line as percentage of file height 3`() {
    configureByLines(17, "    I found it in a legendary land")
    typeText("25%")
    assertPosition(4, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test count percent keeps same column with nostartline`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(0, 0, 14)
    typeText("25%")
    assertPosition(24, 14)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test count percent handles shorter line with nostartline`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    typeText("A", " extra text", "<Esc>")
    typeText("25%")
    assertPosition(24, 33)
  }
}
