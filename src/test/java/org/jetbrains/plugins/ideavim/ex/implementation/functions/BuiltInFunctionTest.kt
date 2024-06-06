/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class BuiltInFunctionTest : VimTestCase() {

  @Test
  fun `test abs`() {
    configureByText("\n")
    typeText(commandToKeys("echo abs(-123) abs(2)"))
    assertExOutput("123 2")
  }

  @Test
  fun `test sin`() {
    configureByText("\n")
    typeText(commandToKeys("echo sin(0) sin(1)"))
    assertExOutput("0.0 0.841471")
  }

  @Test
  fun `test empty`() {
    configureByText("\n")
    typeText(commandToKeys("echo empty(0) empty(1)"))
    assertExOutput("1 0")
    typeText(commandToKeys("echo empty(\"123\") empty(\"\")"))
    assertExOutput("0 1")
    typeText(commandToKeys("echo empty([1, 2]) empty([])"))
    assertExOutput("0 1")
    typeText(commandToKeys("echo empty({1:2}) empty({})"))
    assertExOutput("0 1")
  }

  @Test
  fun `test line`() {
    configureByText("1\n2\n${c}3\n4\n5")
    typeText(commandToKeys("echo line('.')"))
    assertExOutput("3")

    typeText(commandToKeys("echo line('$')"))
    assertExOutput("5")

    typeText(injector.parser.parseKeys("ma"))
    typeText(commandToKeys("""echo line("'a") line("'x")"""))
    assertExOutput("3 0")

    setEditorVisibleSize(screenWidth, 3)
    setPositionAndScroll(2, 2)
    typeText(commandToKeys("""echo line("w0") line("w$")"""))
    assertExOutput("3 5")

    // Without selection - current line
    typeText(commandToKeys("""echo line("v")"""))
    assertExOutput("3")

    // With selection - make sure to delete the '<,'> that is automatically prepended when entering Command-line mode
    // with a selection
    typeText(injector.parser.parseKeys("vj"))
    typeText(injector.parser.parseKeys(""":<BS><BS><BS><BS><BS>echo line("v")<CR>"""))
    assertExOutput("3")

    // Remove selection and check again - note that exiting Command-line mode removes selection and switches back to
    // Normal. This <esc> does nothing
    typeText(injector.parser.parseKeys("<esc>"))
    typeText(commandToKeys("""echo line("v")"""))
    assertExOutput("3")

    typeText(commandToKeys("""echo line("abs") line(1) line([])"""))
    assertExOutput("0 0 0")

    typeText(commandToKeys("""echo line([1, 1]) line(['.', '$']) line(['$', '$'])"""))
    assertExOutput("1 0 0")

    typeText(commandToKeys("""echo line([0, 1]) line([1, 1]) line([5, 1]) line([6, 1]) line([5, 2]) line([5, 3])"""))
    assertExOutput("0 1 5 0 5 0")
  }

  // XXX virtualedit is not tested
  @Test
  fun `test col`() {
    configureByText(
      """
  1
  2
  1234${c}567890
  4
  5
      """.trimIndent(),
    )
    typeText(commandToKeys("echo col('.')"))
    assertExOutput("5")

    typeText(commandToKeys("echo col('$')"))
    assertExOutput("10")

    typeText(injector.parser.parseKeys("ma"))
    typeText(commandToKeys("""echo col("'a") col("'z")"""))
    assertExOutput("5 0")

    // Without selection - current line
    typeText(commandToKeys("""echo col("v")"""))
    assertExOutput("5")

    // With selection - make sure to delete the '<,'> that is automatically prepended when entering Command-line mode
    // with a selection
    typeText(injector.parser.parseKeys("vll"))
    typeText(injector.parser.parseKeys(""":<BS><BS><BS><BS><BS>echo col("v")<CR>"""))
    assertExOutput("5")

    // Remove selection and check again - note that exiting Command-line mode removes selection and switches back to
    // Normal. This <esc> does nothing
    typeText(injector.parser.parseKeys("<esc>"))
    typeText(commandToKeys("""echo col("v")"""))
    assertExOutput("5")

    typeText(commandToKeys("echo col('$')"))
    assertExOutput("10")

    typeText(commandToKeys("""echo col("abs") col(1) col([])"""))
    assertExOutput("0 0 0")

    typeText(commandToKeys("""echo col([1, 1]) col([3, '$'])  col(['.', '$']) col(['$', '$'])"""))
    assertExOutput("1 11 0 0")

    typeText(commandToKeys("""echo col([0, 1]) col([1, 1]) col([5, 1]) col([6, 1]) col([5, 2])"""))
    assertExOutput("0 1 1 0 2")
  }

  @Test
  fun `test exists`() {
    configureByText("\n")
    typeText(commandToKeys("echo exists(\"&nu\")"))
    assertExOutput("1")

    typeText(commandToKeys("echo exists(\"&unknownOptionName\")"))
    assertExOutput("0")

    typeText(commandToKeys("echo exists(\"g:myVar\")"))
    assertExOutput("0")

    enterCommand("let myVar = 42")
    typeText(commandToKeys("echo exists(\"g:myVar\")"))
    assertExOutput("1")
  }

  @Test
  fun `test len`() {
    configureByText("\n")
    typeText(commandToKeys("echo len(123)"))
    assertExOutput("3")

    typeText(commandToKeys("echo len('abcd')"))
    assertExOutput("4")

    typeText(commandToKeys("echo len([1])"))
    assertExOutput("1")

    typeText(commandToKeys("echo len({})"))
    assertExOutput("0")

    typeText(commandToKeys("echo len(#{1: 'one'})"))
    assertExOutput("1")

    typeText(commandToKeys("echo len(12 . 4)"))
    assertExOutput("3")

    typeText(commandToKeys("echo len(4.2)"))
    assertPluginErrorMessageContains("E701: Invalid type for len()")
  }
}
