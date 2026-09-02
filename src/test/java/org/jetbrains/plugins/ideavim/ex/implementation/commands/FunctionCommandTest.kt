/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class FunctionCommandTest : VimTestCase("\n") {
  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `test function command with no user defined functions shows nothing`() {
    assertCommandOutput("function", "")
  }

  @Test
  fun `test function command with no parameters lists single user defined function without body`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function", "function Foo()")
  }

  @Test
  fun `test function command does not list builtin function`() {
    enterCommand("function strlen")
    assertPluginError(true)
    assertPluginErrorMessage("E128: Function name must start with a capital or \"s:\": strlen")
  }

  @Test
  fun `test function command ignores bang modifier`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function!", "function Foo()")
  }

  @Test
  @VimBehaviorDiffers(description = "Vim doesn't appear to have a defined order")
  fun `test function command lists user defined functions in alphabetical order`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function Bar() | echo 'hi' | endfunction")
    assertCommandOutput(
      "function",
      """
        |function Bar()
        |function Foo()
      """.trimMargin()
    )
  }

  @VimBehaviorDiffers(description = "Vim doesn't appear to have a defined order")
  @Test
  fun `test function command sorts uppercase names before lowercase names`() {
    enterCommand("function foo#bar() | echo 'hi' | endfunction")
    enterCommand("function Zoo() | echo 'hi' | endfunction")
    assertCommandOutput(
      "function",
      """
        |function Zoo()
        |function foo#bar()
      """.trimMargin()
    )
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function with command prints function with matching name`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function Bar() | echo 'hi' | endfunction")
    assertCommandOutput("function Foo", "function Foo()")
  }

  @Test
  fun `test function command with unknown function name reports error`() {
    enterCommand("function FooBar() | echo 'hi' | endfunction")
    enterCommand("function Foo")
    assertPluginError(true)
    assertPluginErrorMessage("E123: Undefined function: Foo")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command lists single function argument`() {
    enterCommand("function Foo(a) | echo 'hi' | endfunction")
    assertCommandOutput("function Foo", "function Foo(a)")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command lists function arguments`() {
    enterCommand("function Foo(a, b) | echo 'hi' | endfunction")
    assertCommandOutput("function Foo", "function Foo(a, b)")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command lists single default argument`() {
    enterCommand("function Foo(a = 1) | echo 'hi' | endfunction")
    assertCommandOutput("function Foo", "function Foo(a = 1)")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command lists default arguments`() {
    enterCommand("function Foo(a = 1, b = 'foo') | echo 'hi' | endfunction")
    assertCommandOutput("function Foo", "function Foo(a = 1, b = 'foo')")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command lists optional arguments`() {
    enterCommand("function Foo(...) | echo 'hi' | endfunction")
    assertCommandOutput("function Foo", "function Foo(...)")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command lists all arguments together`() {
    enterCommand("function Foo(a, b = 1, ...) | echo 'hi' | endfunction")
    assertCommandOutput("function Foo", "function Foo(a, b = 1, ...)")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command lists range flag`() {
    enterCommand("function Foo() range | echo 'hi' | endfunction")
    assertCommandOutput("function", "function Foo() range")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command lists abort flag`() {
    enterCommand("function Foo() abort | echo 'hi' | endfunction")
    assertCommandOutput("function", "function Foo() abort")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command lists dict flag`() {
    enterCommand("function Foo() dict | echo 'hi' | endfunction")
    assertCommandOutput("function", "function Foo() dict")
  }

  // Note that the 'closure' flag is only applicable on nested functions, which aren't listed by `:function`

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function lists flags in correct order`() {
    executeVimscript("""
      |function Foo() range abort dict
      |  echo "hi"
      |endfunction
    """.trimMargin())
    assertPluginError(false)
    assertCommandOutput("function", "function Foo() abort range dict")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function only lists global functions`() {
    executeVimscript("""
      |function Foo()
      |  function Bar() closure
      |    echo 'hi'
      |  endfunction
      |endfunction
    """.trimMargin())
    assertPluginError(false)
    assertCommandOutput("function", "function Foo()")
  }

  @VimBehaviorDiffers(description = "Vim lists script-local functions, using the `<SNR>{id}_` prefix")
  @Test
  fun `test function does not list script-local functions`() {
    val file = tempDir.resolve("foo.vim")
    file.writeText("""
      |function s:Foo()
      |  echo 'hi'
      |endfunction
    """.trimMargin())
    enterCommand("source $file")
    assertPluginError(false)
    assertCommandOutput("function", "")
  }

  @Test
  fun `test function with too many names throws error`() {
    enterCommand("function Foo Bar")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters:  Bar") // Note extra whitespace
  }

  @Test
  fun `test function command with three names throws error`() {
    enterCommand("function Foo Bar Baz")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters:  Bar Baz") // Note extra whitespace
  }

  @Test
  fun `test function with invalid function name throws error`() {
    enterCommand("function 123")
    assertPluginError(true)
    assertPluginErrorMessage("E129: Function name required")
  }

  @Test
  fun `test function name starting with lowercase throws error`() {
    enterCommand("function foo")
    assertPluginError(true)
    assertPluginErrorMessage("E128: Function name must start with a capital or \"s:\": foo")
  }

  @Test
  fun `test function name starting with script-local scope throws error`() {
    enterCommand("function s:Foo")
    assertPluginError(true)
    assertPluginErrorMessage("E81: Using <SID> not in a script context")
  }

  @Test
  fun `test function name with SID prefix throws error`() {
    enterCommand("function <SID>Foo")
    assertPluginError(true)
    assertPluginErrorMessage("E81: Using <SID> not in a script context")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function name starting with global scope prints function`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function g:Foo", "function Foo()")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function name with global scope starting with lowercase throws error`() {
    enterCommand("function g:foo")
    assertPluginError(true)
    assertPluginErrorMessage("E128: Function name must start with a capital or \"s:\": g:foo")
  }

  @Test
  fun `test function name with other scope throws error`() {
    enterCommand("function l:Foo")
    assertPluginError(true)
    assertPluginErrorMessage("E884: Function name cannot contain a colon: l:Foo")
  }

  @Test
  fun `test function name with global scope and no name throws error`() {
    enterCommand("function g:")
    assertPluginError(true)
    assertPluginErrorMessage("E129: Function name required")
  }

  @Test
  fun `test unknown function with autoload prefix throws undefined function error`() {
    enterCommand("function foo#Bar")
    assertPluginError(true)
    assertPluginErrorMessage("E123: Undefined function: foo#Bar")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with autoload name prints function`() {
    // Note that this line doesn't work in Vim (E746: Function name does not match script name)
    enterCommand("function foo#bar() | echo 'hi' | endfunction")
    assertCommandOutput("function foo#bar", "function foo#bar()")
  }

  @Test
  fun `test function name starting with autoload separator reports error`() {
    enterCommand("function #Foo")
    assertPluginError(true)
    assertPluginErrorMessage("E129: Function name required")
  }

  @Test
  fun `test function command with invalid autoload name`() {
    enterCommand("function foo#")
    assertPluginError(true)
    assertPluginErrorMessage("E123: Undefined function: foo#")
  }

  @Test
  fun `test function name is not evaluated as an expression`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("let nm = 'Foo'")
    enterCommand("function nm")
    assertPluginError(true)
    assertPluginErrorMessage("E128: Function name must start with a capital or \"s:\": nm")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with curly braces name prints function`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function Fo{'o'}", "function Foo()")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with curly braces name for whole name prints function`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function {'Foo'}", "function Foo()")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with multiple curly braces name parts prints function`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function F{'o'}{'o'}", "function Foo()")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with curly braces name using variable prints function`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("let nm = 'oo'")
    assertCommandOutput("function F{nm}", "function Foo()")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with curly braces name evaluating to autoload name prints function`() {
    enterCommand("function foo#bar() | echo 'hi' | endfunction")
    assertCommandOutput("function f{'oo'}#bar", "function foo#bar()")
  }

  @Test
  fun `test function command with curly braces name evaluating to empty lists all functions`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function Bar() | echo 'hi' | endfunction")
    assertCommandOutput(
      "function {''}",
      """
        |function Bar()
        |function Foo()
      """.trimMargin()
    )
  }

  @Test
  fun `test function command with curly braces name for unknown function reports error`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function B{'ar'}")
    assertPluginError(true)
    assertPluginErrorMessage("E123: Undefined function: Bar")
  }

  @Test
  fun `test function command with curly braces name evaluating to lowercase throws error`() {
    enterCommand("function {'foo'}")
    assertPluginError(true)
    assertPluginErrorMessage("E128: Function name must start with a capital or \"s:\": {'foo'}")
  }

  @Test
  fun `test function command with pattern lists matching functions`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function FooBar() | echo 'hi' | endfunction")
    assertCommandOutput(
      "function /Foo", """
      |function Foo()
      |function FooBar()
    """.trimMargin()
    )
  }

  @Test
  fun `test function command with pattern without leading whitespace lists matching functions`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function FooBar() | echo 'hi' | endfunction")
    assertCommandOutput(
      "function/Foo", """
      |function Foo()
      |function FooBar()
    """.trimMargin()
    )
  }

  @VimBehaviorDiffers(description = "Vim reports 'E54: Unmatched \\('")
  @Test
  fun `test function command with invalid pattern reports error`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function /\\(")
    assertPluginError(true)
    assertPluginErrorMessage("E383: Invalid search string: \\(")
  }

  @Test
  fun `test function command with regex pattern containing bar is not treated as a line separator`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function Bar() | echo 'hi' | endfunction")
    assertCommandOutput(
      "function /Foo\\|Bar", """
      |function Bar()
      |function Foo()
    """.trimMargin()
    )
  }

  @Test
  fun `test function command with regex pattern lists matching functions`() {
    enterCommand("function FooBar() | echo 'hi' | endfunction")
    enterCommand("function BarBaz() | echo 'hi' | endfunction")
    enterCommand("function QuuxBar() | echo 'hi' | endfunction")
    assertCommandOutput(
      "function /Bar$", """
      |function FooBar()
      |function QuuxBar()
    """.trimMargin()
    )
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with regex pattern matches autoload function`() {
    // Note that Vim fails with E746: Function name does not match script name
    enterCommand("function foo#bar() | echo 'hi' | endfunction")
    assertCommandOutput("function /foo#", "function foo#bar()")
  }

  @Test
  fun `test function command with empty pattern lists all functions`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function Bar() | echo 'hi' | endfunction")
    assertCommandOutput(
      "function /",
      """
        |function Bar()
        |function Foo()
      """.trimMargin()
    )
  }

  @Test
  fun `test function command with pattern matching nothing shows nothing`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function /Zzz", "")
    assertPluginError(false)
  }

  @Test
  fun `test function command pattern can contain whitespace`() {
    // The whole of the rest of the line is the pattern, so this is not a "trailing characters" error
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function /Foo Bar", "")
    assertPluginError(false)
  }

  @Test
  fun `test function command with pattern is case sensitive by default`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function FooBar() | echo 'hi' | endfunction")
    assertCommandOutput("function /foo", "")
  }

  @Test
  fun `test function command with pattern lists matching functions ignoring case when 'ignorecase' set`() {
    enterCommand("set ignorecase")
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function FooBar() | echo 'hi' | endfunction")
    assertCommandOutput(
      "function /foo", """
      |function Foo()
      |function FooBar()
    """.trimMargin()
    )
  }

  @Test
  fun `test function command with pattern ignores smartcase`() {
    enterCommand("set ignorecase smartcase")
    enterCommand("function FooBar() | echo 'hi' | endfunction")
    assertCommandOutput("function /fooBAR", "function FooBar()")
  }

  @Test
  fun `test function command with pattern with backslash c ignores case`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function /\\cfoo", "function Foo()")
  }

  @Test
  fun `test function command with pattern with backslash C matches case`() {
    enterCommand("set ignorecase")
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function /\\CFOO", "")
  }

  @VimBehaviorDiffers(description = "Vim lists lambdas when given a pattern: `function <lambda>1(x, ...)`")
  @Test
  fun `test function command does not list lambdas`() {
    enterCommand("let Lambda = {x -> x + 1}")
    assertCommandOutput("function /lambda", "")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with dict function`() {
    executeVimscript("""
      |function Mylen() dict
      |  return len(self.data)
      |endfunction
      |let mydict = {'data': [0, 1, 2, 3], 'len': function("Mylen")}
    """.trimMargin())
    assertCommandOutput("function mydict.len", "function Mylen() dict")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with dict function evaluated from expression`() {
    executeVimscript("""
      |function Mylen() dict
      |  return len(self.data)
      |endfunction
      |let mydict = {'data': [0, 1, 2, 3], 'len': function("Mylen")}
      |let foo = mydict
    """.trimMargin())
    assertCommandOutput("function foo.len", "function Mylen() dict")
  }

  @VimBehaviorDiffers(description = "Vim shows the body of the function")
  @Test
  fun `test function command with dict function evaluated from complex expression`() {
    executeVimscript("""
      |function Mylen() dict
      |  return len(self.data)
      |endfunction
      |let mydict = {'data': [0, 1, 2, 3], 'len': function("Mylen")}
      |let mydict2 = {'mydict': mydict}
    """.trimMargin())
    assertCommandOutput("function mydict2.mydict.len", "function Mylen() dict")
  }

  @Test
  fun `test function command with dict function defined by curly braces name`() {
    executeVimscript("""
      |function Mylen() dict
      |  return len(self.data)
      |endfunction
      |let mydict = {'data': [0, 1, 2, 3], 'len': function("Mylen")}
    """.trimMargin())
    assertCommandOutput("function {'mydict'}.len", "function Mylen() dict")
    assertCommandOutput("function {'myd'}ict.len", "function Mylen() dict")
  }

  @Test
  fun `test function command with dict function defined by curly braces name throws error with partial curly braces name key`() {
    executeVimscript("""
      |function Mylen() dict
      |  return len(self.data)
      |endfunction
      |let mydict = {'data': [0, 1, 2, 3], 'len': function("Mylen")}
    """.trimMargin())
    enterCommand("function mydict.l{'en'}")
    assertPluginError(true)
    assertPluginErrorMessage("E716: Key not present in Dictionary: \"l\"")
  }

  @Test
  fun `test function command with dict function reports about trailing curly braces name`() {
    executeVimscript("""
      |function Mylen() dict
      |  return len(self.data)
      |endfunction
      |let mydict = {'data': [0, 1, 2, 3], 'len': function("Mylen"), 'l': function('Mylen')}
    """.trimMargin())
    enterCommand("function mydict.l{'en'}")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: {'en'}")
  }

  @Test
  fun `test function command with dict function defined by curly braces name throws error with curly braces name key`() {
    executeVimscript("""
      |function Mylen() dict
      |  return len(self.data)
      |endfunction
      |let mydict = {'data': [0, 1, 2, 3], 'len': function("Mylen")}
    """.trimMargin())
    enterCommand("function mydict.{'len'}")
    assertPluginError(true)
    assertPluginErrorMessage("E713: Cannot use empty key for Dictionary")
  }

  @VimBehaviorDiffers(
    originalVimAfter = "E488: Trailing characters: .len",
    description = "Maybe we can update the parser to handle a function command that looks like the start of the function definition, but I couldn't make it work"
  )
  @Test
  fun `test function command with dict function evaluated from function call expression throws error`() {
    executeVimscript("""
      |function Mylen() dict
      |  return len(self.data)
      |endfunction
      |let mydict = {'data': [0, 1, 2, 3], 'len': function("Mylen")}
      |function GetDict()
      |  return g:mydict
      |endfunction
    """.trimMargin())
    // This is the same syntax as function definition, so Vim fails
    enterCommand("function GetDict().len")
    assertPluginError(true)
    assertPluginErrorMessage("line 1:18 mismatched input '.'")
  }

  @Test
  fun `test function command with trailing whitespace lists matching function`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function Foo ", "function Foo()")
  }

  @Test
  fun `test function command with extra leading whitespace lists matching function`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function  Foo", "function Foo()")
  }

  @Test
  fun `test function command with comment lists all functions`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function \" comment", "function Foo()")
  }

  @Test
  fun `test function command with name and comment lists matching function`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    assertCommandOutput("function Foo \" comment", "function Foo()")
  }

  @Test
  fun `test function command treats bar as part of trailing comment`() {
    enterCommand("let g:marker = 0")
    enterCommand("function \" comment |let g:marker = 42")
    assertPluginError(false)
    assertCommandOutput("echo g:marker", "0")
  }

  @Test
  fun `test function command with no argument correctly handles line separator`() {
    enterCommand("let g:marker = 0")
    enterCommand("function|let g:marker = 42")
    assertPluginError(false)
    assertCommandOutput("echo g:marker", "42")
  }

  @Test
  fun `test function command correctly handles line separator`() {
    // The marker proves the command after the bar was executed as a separate command
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("let g:marker = 0")
    enterCommand("function Foo|let g:marker = 42")
    assertPluginError(false)
    assertCommandOutput("echo g:marker", "42")
  }

  @Test
  fun `test function command correctly handles line separator 2`() {
    // The marker proves the command after the bar was executed as a separate command
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("let g:marker = 0")
    enterCommand("function Foo |let g:marker = 42")
    assertPluginError(false)
    assertCommandOutput("echo g:marker", "42")
  }

  @Test
  fun `test function command does not list deleted function`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function Bar() | echo 'hi' | endfunction")
    enterCommand("delfunction Foo")
    assertPluginError(false)
    assertCommandOutput("function", "function Bar()")
  }

  @Test
  fun `test function command lists redefined function once`() {
    enterCommand("function Foo() | echo 'hi' | endfunction")
    enterCommand("function! Foo() abort | echo 'bye' | endfunction")
    assertPluginError(false)
    assertCommandOutput("function", "function Foo() abort")
  }

  @Test
  fun `test function command lists functions exported by extensions`() {
    // Extensions export script functions via VimExtensionFacade, which stores them as global functions
    enableExtensions("surround")
    assertCommandOutput("function", "function SurroundOperatorFunc(type)")
  }

  @Test
  fun `test function command does not list numbered dict functions`() {
    // See :help numbered-function
    executeVimscript("""
      |let g:d = {}
      |function g:d.init(arg)
      |  echo a:arg
      |endfunction
    """.trimMargin())
    assertPluginError(false)
    assertCommandOutput("function", "")
  }

  @Test
  fun `test function command does lists numbered dict function when requested directly`() {
    // See :help numbered-function
    executeVimscript("""
      |let g:d = {}
      |function g:d.init(arg)
      |  echo a:arg
      |endfunction
    """.trimMargin())
    assertPluginError(false)
    assertCommandOutput("function d.init", "function 1(arg) dict")
  }

  @VimBehaviorDiffers(
    originalVimAfter = "E126: Missing :endfunction",
    description = "Maybe we can update the parser to handle a function command that looks like the start of the function definition, but I couldn't make it work"
  )
  @Test
  fun `test function command with parentheses throws error`() {
    // `:function Foo()` starts a function definition, so it is not a listing, and there is no body or `endfunction`
    enterCommand("function Foo()")
    assertPluginError(true)
    assertPluginErrorMessage("line 2:0 extraneous input '<EOF>'")
  }
}
