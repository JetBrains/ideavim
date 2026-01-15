/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.statements

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class FunctionDeclarationTest : VimTestCase() {

  @Test
  fun `test user defined function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function GetHiString(name) |" +
          "  return 'Oh hi ' . a:name | " +
          "endfunction |" +
          "echo GetHiString('Mark')",
      ),
    )
    assertExOutput("Oh hi Mark")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test unknown function`() {
    configureByText("\n")
    typeText(commandToKeys("echo GetHiString('Mark')"))
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: GetHiString")
  }

  @Test
  fun `test function arguments are eagerly evaluated left to right`() {
    configureByText("\n")
    typeText(
      commandToKeys("""
        |function printArg(arg) |
        |    echo "arg: " . a:arg |
        |endfunction |
        |function test(x, y, z) |
        |    echo "Function call done" |
        |endfunction |
      """.trimMargin()
      )
    )
    enterCommand("call test(printArg(1), printArg(2), printArg(3))")
    assertExOutput("""
        |arg: 1
        |arg: 2
        |arg: 3
        |Function call done
      """.trimMargin()
    )
  }

  @Test
  fun `test nested function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  function F2() |" +
          "    return 555 |" +
          "  endfunction |" +
          "  return 10 * F2() |" +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo F1()"))
    assertExOutput("5550")
    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("echo F2()"))
    assertExOutput("555")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test call nested function without calling a container function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  function F2() |" +
          "    return 555 |" +
          "  endfunction |" +
          "  return 10 * F2() |" +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo F2()"))
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: F2")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test defining an existing function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  return 10 |" +
          "endfunction",
      ),
    )
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  return 100 |" +
          "endfunction",
      ),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E122: Function F1 already exists, add ! to replace it")

    typeText(commandToKeys("echo F1()"))
    assertExOutput("10")

    typeText(commandToKeys("delf! F1"))
  }

  @Test
  fun `test redefining an existing function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  return 10 |" +
          "endfunction",
      ),
    )
    typeText(
      commandToKeys(
        "" +
          "function! F1() |" +
          "  return 100 |" +
          "endfunction",
      ),
    )
    assertPluginError(false)
    typeText(commandToKeys("echo F1()"))
    assertExOutput("100")

    typeText(commandToKeys("delf! F1"))
  }

  @Test
  fun `test closure function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  let x = 5 |" +
          "  function F2() closure |" +
          "    return 10 * x |" +
          "  endfunction |" +
          "  return F2() |" +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo F1()"))
    assertExOutput("50")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test outer variable cannot be reached from inner function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  let x = 5 |" +
          "  function F2() |" +
          "    return 10 * x |" +
          "  endfunction |" +
          "  return F2() |" +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: x")
    assertExOutput("0")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @Test
  fun `test call closure function multiple times`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  let x = 0 |" +
          "  function F2() closure |" +
          "    let x += 1 |" +
          "    return x |" +
          "  endfunction |" +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo F1()"))
    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("echo F2()"))
    assertExOutput("1")
    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("echo F2()"))
    assertExOutput("2")
    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("echo F2()"))
    assertExOutput("3")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @Test
  fun `test local variables exist after delfunction command`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  let x = 0 |" +
          "  function F2() closure |" +
          "    let x += 1 |" +
          "    return x |" +
          "  endfunction |" +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo F1()"))
    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("echo F2()"))
    assertExOutput("1")
    typeText(commandToKeys("delf! F1"))
    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("echo F2()"))
    assertExOutput("2")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test outer function does not see inner closure function variable`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  function! F2() closure |" +
          "    let x = 1 |" +
          "    return 10 |" +
          "  endfunction |" +
          "  echo x |" +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: x")

    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("echo F2()"))
    assertExOutput("10")
    assertPluginError(false)

    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: x")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test function without abort flag`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function! F1() |" +
          "  echo unknownVar |" +
          "  let g:x = 10 |" +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: unknownVar")

    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("echo x"))
    assertExOutput("10")
    assertPluginError(false)

    typeText(commandToKeys("delf! F1"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test function with abort flag`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function! F1() abort |" +
          "  echo unknownVar |" +
          "  let g:x = 10 |" +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: unknownVar")

    typeText(commandToKeys("echo x"))
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: x")

    typeText(commandToKeys("delf! F1"))
  }

  @Test
  fun `test function without range flag`() {
    configureByText(
      """
        -----
        ${c}12345
        abcde
        -----
      """.trimIndent(),
    )
    typeText(
      commandToKeys(
        "" +
          "let rangesConcatenation = '' |" +
          "function! F1() |" +
          "  let g:rangesConcatenation .= line('.') |" +
          "endfunction |",
      ),
    )
    typeText(commandToKeys("1,3call F1()"))
    typeText(commandToKeys("echo rangesConcatenation"))
    assertPluginError(false)
    assertExOutput("123")

    assertState(
      """
        -----
        12345
        ${c}abcde
        -----
      """.trimIndent(),
    )
    typeText(commandToKeys("delf! F1"))
  }

  @Test
  fun `test function with range flag`() {
    configureByText(
      """
        -----
        12345
        abcde
        $c-----
      """.trimIndent(),
    )
    typeText(
      commandToKeys(
        "" +
          "let rangesConcatenation = '' |" +
          "function! F1() range |" +
          "  let g:rangesConcatenation .= line('.') |" +
          "endfunction |",
      ),
    )
    typeText(commandToKeys("1,3call F1()"))
    typeText(commandToKeys("echo rangesConcatenation"))
    assertPluginError(false)
    assertExOutput("1")

    assertState(
      """
        $c-----
        12345
        abcde
        -----
      """.trimIndent(),
    )
    typeText(commandToKeys("delf! F1"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test trying to create a function with firstline or lastline argument`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function! F1(firstline) |" +
          "  echo unknownVar |" +
          "  let g:x = 10 |" +
          "endfunction",
      ),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E125: Illegal argument: firstline")

    typeText(
      commandToKeys(
        "" +
          "function! F1(lastline) |" +
          "  echo unknownVar |" +
          "  let g:x = 10 |" +
          "endfunction",
      ),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E125: Illegal argument: lastline")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @Test
  fun `test firstline and lastline default value if no range specified`() {
    configureByText(
      """
        -----
        12${c}345
        abcde
        -----
      """.trimIndent(),
    )
    typeText(
      commandToKeys(
        "" +
          "function! F1() range |" +
          "  echo a:firstline .. ':' .. a:lastline |" +
          "endfunction |",
      ),
    )
    typeText(commandToKeys("call F1()"))
    assertPluginError(false)
    assertExOutput("2:2")
    assertState(
      """
        -----
        12${c}345
        abcde
        -----
      """.trimIndent(),
    )
    typeText(commandToKeys("delf! F1"))
  }

  @Test
  fun `test firstline and lastline default value if range is specified`() {
    configureByText(
      """
        -----
        12${c}345
        abcde
        -----
      """.trimIndent(),
    )
    typeText(
      commandToKeys(
        "" +
          "function! F1() |" +
          "  echo a:firstline .. ':' .. a:lastline |" +
          "endfunction |",
      ),
    )
    typeText(commandToKeys("1,4call F1()"))
    assertPluginError(false)
    assertExOutput(
      """
      1:4
      1:4
      1:4
      1:4
    """.trimIndent()
    )
    assertState(
      """
        -----
        12345
        abcde
        $c-----
      """.trimIndent(),
    )
    typeText(commandToKeys("delf! F1"))
  }

  @Test
  fun `test functions without range flag columns`() {
    configureByText(
      """
        -----
        12345
        abcde
        ---$c--
      """.trimIndent(),
    )
    typeText(
      commandToKeys(
        "" +
          "let columns = '' |" +
          "function! F1() |" +
          "  let g:columns .= col('.') .. ',' |" +
          "endfunction |",
      ),
    )
    typeText(commandToKeys("call F1()"))
    typeText(commandToKeys("echo columns"))
    assertPluginError(false)
    assertExOutput("4,")

    typeText(commandToKeys("let columns = ''"))
    typeText(commandToKeys("1,3call F1()"))
    typeText(commandToKeys("echo columns"))
    assertPluginError(false)
    assertExOutput("1,1,1,")

    typeText(commandToKeys("delf! F1"))
  }

  @Test
  fun `test functions with range flag columns`() {
    configureByText(
      """
        -----
        12345
        abcde
        ---$c--
      """.trimIndent(),
    )
    typeText(
      commandToKeys(
        "" +
          "let columns = '' |" +
          "function! F1() range |" +
          "  let g:columns .= col('.') .. ',' |" +
          "endfunction |",
      ),
    )
    typeText(commandToKeys("call F1()"))
    typeText(commandToKeys("echo columns"))
    assertPluginError(false)
    assertExOutput("4,")

    typeText(commandToKeys("let columns = ''"))
    typeText(commandToKeys("1,3call F1()"))
    typeText(commandToKeys("echo columns"))
    assertPluginError(false)
    assertExOutput("1,")

    typeText(commandToKeys("delf! F1"))
  }

  @Test
  fun `test only optional arguments`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function GetOptionalArgs(...) |" +
          "  return a:000 | " +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo GetOptionalArgs()"))
    assertExOutput("[]")
    typeText(
      commandToKeys(
        "echo GetOptionalArgs(42, 'optional arg')",
      ),
    )
    assertExOutput("[42, 'optional arg']")

    typeText(commandToKeys("delfunction! GetOptionalArgs"))
  }

  @Test
  fun `test only default arguments`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function GetDefaultArgs(a = 10, b = 20) |" +
          "  return [a:a, a:b] | " +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo GetDefaultArgs()"))
    assertExOutput("[10, 20]")
    typeText(commandToKeys("echo GetDefaultArgs(42, 'optional arg')"))
    assertExOutput("[42, 'optional arg']")

    typeText(commandToKeys("delfunction! GetDefaultArgs"))
  }

  @Test
  fun `test optional arguments`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function GetOptionalArgs(name, ...) |" +
          "  return a:000 | " +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo GetOptionalArgs('this arg is not optional')"))
    assertExOutput("[]")
    typeText(
      commandToKeys(
        "echo GetOptionalArgs('this arg is not optional', 42, 'optional arg')",
      ),
    )
    assertExOutput("[42, 'optional arg']")

    typeText(commandToKeys("delfunction! GetOptionalArgs"))
  }

  @Test
  fun `test arguments with default values`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function GetOptionalArgs(name, a = 10, b = 20) |" +
          "  return 'a = ' .. a:a .. ', b = ' .. a:b | " +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo GetOptionalArgs('this arg is not optional')"))
    assertExOutput("a = 10, b = 20")

    typeText(commandToKeys("echo GetOptionalArgs('this arg is not optional', 42)"))
    assertExOutput("a = 42, b = 20")

    typeText(commandToKeys("echo GetOptionalArgs('this arg is not optional', 100, 200)"))
    assertExOutput("a = 100, b = 200")

    typeText(commandToKeys("delfunction! GetOptionalArgs"))
  }

  @Test
  fun `test arguments with default values and optional args`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function GetOptionalArgs(name, a = 10, b = 20, ...) |" +
          "  return {'a': a:a, 'b': a:b, '000': a:000} | " +
          "endfunction",
      ),
    )
    typeText(commandToKeys("echo GetOptionalArgs('this arg is not optional')"))
    assertExOutput("{'a': 10, 'b': 20, '000': []}")

    typeText(commandToKeys("echo GetOptionalArgs('this arg is not optional', 42)"))
    assertExOutput("{'a': 42, 'b': 20, '000': []}")

    typeText(commandToKeys("echo GetOptionalArgs('this arg is not optional', 100, 200)"))
    assertExOutput("{'a': 100, 'b': 200, '000': []}")

    typeText(commandToKeys("echo GetOptionalArgs('this arg is not optional', 100, 200, 300)"))
    assertExOutput("{'a': 100, 'b': 200, '000': [300]}")

    typeText(commandToKeys("delfunction! GetOptionalArgs"))
  }

  @Test
  fun `test finish statement in function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
        let x = 3 |
        function! F() |
          finish |
          let g:x = 10 |
        endfunction |
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("call F()"))
    typeText(commandToKeys("echo x"))
    assertExOutput("3")

    typeText(commandToKeys("delfunction! F"))
  }

  @Test
  fun `test args are passed to function by reference`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
        function! AddNumbers(dict) |
          let a:dict.one = 1 |
          let a:dict['two'] = 2 |
        endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let d = {}"))
    typeText(commandToKeys("call AddNumbers(d)"))
    typeText(commandToKeys("echo d"))
    assertPluginError(false)
    assertExOutput("{'one': 1, 'two': 2}")

    typeText(commandToKeys("delfunction! AddNumbers"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test define script function in command line context`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function s:GetHiString(name) |" +
          "  return 'Oh hi ' . a:name | " +
          "endfunction",
      ),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E81: Using <SID> not in a script context")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test get script function in command line context`() {
    configureByText("\n")
    typeText(
      commandToKeys("echo s:F1()"),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E120: Using <SID> not in a script context: s:F1")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test get built-in function with global scope`() {
    configureByText("\n")
    typeText(commandToKeys("echo g:abs(-10)"))
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: g:abs")
  }

  @Test
  fun `test return with no expression`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function ZeroGenerator() |" +
          "  return | " +
          "endfunction",
      ),
    )
    assertPluginError(false)
    typeText(commandToKeys("echo ZeroGenerator()"))
    assertExOutput("0")
  }
}
