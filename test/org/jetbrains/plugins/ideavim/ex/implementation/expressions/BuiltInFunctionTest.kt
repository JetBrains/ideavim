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

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class BuiltInFunctionTest : VimTestCase() {

  fun `test abs`() {
    configureByText("\n")
    typeText(commandToKeys("echo abs(-123) abs(2)"))
    assertExOutput("123 2\n")
  }

  fun `test sin`() {
    configureByText("\n")
    typeText(commandToKeys("echo sin(0) sin(1)"))
    assertExOutput("0.0 0.841471\n")
  }

  fun `test empty`() {
    configureByText("\n")
    typeText(commandToKeys("echo empty(0) empty(1)"))
    assertExOutput("1 0\n")
    typeText(commandToKeys("echo empty(\"123\") empty(\"\")"))
    assertExOutput("0 1\n")
    typeText(commandToKeys("echo empty([1, 2]) empty([])"))
    assertExOutput("0 1\n")
    typeText(commandToKeys("echo empty({1:2}) empty({})"))
    assertExOutput("0 1\n")
  }

  fun `test line`() {
    configureByText("1\n2\n${c}3\n4\n5")
    typeText(commandToKeys("echo line('.')"))
    assertExOutput("3\n")

    typeText(commandToKeys("echo line('$')"))
    assertExOutput("5\n")

    typeText(parseKeys("ma"))
    typeText(commandToKeys("""echo line("'a") line("'x")"""))
    assertExOutput("3 0\n")

    setEditorVisibleSize(screenWidth, 3)
    setPositionAndScroll(2, 2)
    typeText(commandToKeys("""echo line("w0") line("w$")"""))
    assertExOutput("3 5\n")

    // Without selection - current line
    typeText(commandToKeys("""echo line("v")"""))
    assertExOutput("3\n")
    // With selection
    typeText(parseKeys("vj"))
    typeText(commandToKeys("""echo line("v")"""))
    assertExOutput("3\n")
    // Remove selection and check again
    typeText(parseKeys("<esc>"))
    typeText(commandToKeys("""echo line("v")"""))
    assertExOutput("4\n")

    typeText(commandToKeys("""echo line("abs") line(1) line([])"""))
    assertExOutput("0 0 0\n")

    typeText(commandToKeys("""echo line([1, 1]) line(['.', '$']) line(['$', '$'])"""))
    assertExOutput("1 0 0\n")

    typeText(commandToKeys("""echo line([0, 1]) line([1, 1]) line([5, 1]) line([6, 1]) line([5, 2]) line([5, 3])"""))
    assertExOutput("0 1 5 0 5 0\n")
  }

  // XXX virtualedit is not tested
  fun `test col`() {
    configureByText(
      """
  1
  2
  1234${c}567890
  4
  5
      """.trimIndent()
    )
    typeText(commandToKeys("echo col('.')"))
    assertExOutput("5\n")

    typeText(commandToKeys("echo col('$')"))
    assertExOutput("10\n")

    typeText(parseKeys("ma"))
    typeText(commandToKeys("""echo col("'a") col("'z")"""))
    assertExOutput("5 0\n")

    // Without selection - current line
    typeText(commandToKeys("""echo col("v")"""))
    assertExOutput("5\n")
    // With selection
    typeText(parseKeys("vll"))
    typeText(commandToKeys("""echo col("v")"""))
    assertExOutput("5\n")
    // Remove selection and check again
    typeText(parseKeys("<esc>"))
    typeText(commandToKeys("""echo col("v")"""))
    assertExOutput("7\n")

    typeText(commandToKeys("echo col('$')"))
    assertExOutput("10\n")

    typeText(commandToKeys("""echo col("abs") col(1) col([])"""))
    assertExOutput("0 0 0\n")

    typeText(commandToKeys("""echo col([1, 1]) col([3, '$'])  col(['.', '$']) col(['$', '$'])"""))
    assertExOutput("1 11 0 0\n")

    typeText(commandToKeys("""echo col([0, 1]) col([1, 1]) col([5, 1]) col([6, 1]) col([5, 2])"""))
    assertExOutput("0 1 1 0 2\n")
  }

  fun `test exists`() {
    configureByText("\n")
    typeText(commandToKeys("echo exists(5)"))
    assertExOutput("0\n")

    typeText(commandToKeys("echo exists(\"&nu\")"))
    assertExOutput("1\n")

    typeText(commandToKeys("echo exists(\"&unknownOptionName\")"))
    assertExOutput("0\n")
  }

  fun `test len`() {
    configureByText("\n")
    typeText(commandToKeys("echo len(123)"))
    assertExOutput("3\n")

    typeText(commandToKeys("echo len('abcd')"))
    assertExOutput("4\n")

    typeText(commandToKeys("echo len([1])"))
    assertExOutput("1\n")

    typeText(commandToKeys("echo len({})"))
    assertExOutput("0\n")

    typeText(commandToKeys("echo len(#{1: 'one'})"))
    assertExOutput("1\n")

    typeText(commandToKeys("echo len(12 . 4)"))
    assertExOutput("3\n")

    typeText(commandToKeys("echo len(4.2)"))
    assertPluginErrorMessageContains("E701: Invalid type for len()")
  }
}
