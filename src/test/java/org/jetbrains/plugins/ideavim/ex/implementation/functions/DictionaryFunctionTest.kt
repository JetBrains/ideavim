/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class DictionaryFunctionTest : VimTestCase("\n") {
  @Test
  fun `test self variable in dictionary function assigned in dictionary expression`() {
    addDictionaryFunction("echo self.data")
    enterCommand("let dict = {'data': [1, 2, 3], 'print': function('Print')}")
    assertCommandOutput("call dict.print()", "[1, 2, 3]")
    assertCommandOutput("call dict['print']()", "[1, 2, 3]")
  }

  @Test
  fun `test self variable in dictionary function assigned as dictionary indexed expression`() {
    addDictionaryFunction("echo self.name")
    enterCommand("let dict = {'name': 'dict_name'}")
    enterCommand("let PrintFr = function('Print')")
    enterCommand("let dict.print = PrintFr")
    assertCommandOutput("call dict.print()", "dict_name")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test calling function without dict`() {
    addDictionaryFunction("echo self")
    enterCommand("call Print()")
    assertPluginError(true)
    assertPluginErrorMessage("E725: Calling dict function without Dictionary: Print")
  }

  @Test
  fun `test assigned dictionary function to another dictionary`() {
    addDictionaryFunction("echo self.name")
    enterCommand("let dict = {'name': 'dict', 'print': function('Print')}")
    assertCommandOutput("call dict.print()", "dict")

    // The "print" entry is a function reference, so we just output the name
    assertCommandOutput("echo dict", "{'name': 'dict', 'print': function('Print')}")

    // dict.print evaluates to a partial function, so we output the function name and the captured "dict"
    assertCommandOutput("echo dict.print", "function('Print', {'name': 'dict', 'print': function('Print')})")
    assertCommandOutput("echo dict['print']", "function('Print', {'name': 'dict', 'print': function('Print')})")

    enterCommand("let dict2 = {'name': 'dict2', 'print': dict.print}")
    assertCommandOutput("call dict2.print()", "dict2")

    // The 'print' entry is a partial function, so we output the function name and the captured "dict" dictionary
    assertCommandOutput(
      "echo dict2",
      "{'name': 'dict2', 'print': function('Print', {'name': 'dict', 'print': function('Print')})}"
    )

    // dict2.print is evaluated to a partial function, so we output the function name and the captured "dict2".
    // The print entry in "dict2" is also a partial function (initialised from "dict.print"), so we output the name and
    // the captured "dict" dictionary, which has a simple function reference only containing name.
    assertCommandOutput(
      "echo dict2.print",
      "function('Print', {'name': 'dict2', 'print': function('Print', {'name': 'dict', 'print': function('Print')})})"
    )
    assertCommandOutput(
      "echo dict2['print']",
      "function('Print', {'name': 'dict2', 'print': function('Print', {'name': 'dict', 'print': function('Print')})})"
    )
  }

  @Test
  fun `test self variable not copied when assigning dictionary function to new dictionary entry`() {
    addDictionaryFunction("echo self.name")
    enterCommand("let dict = {'name': 'dict', 'print': function('Print')}")
    enterCommand("let dict2 = {'name': 'dict2'}")
    enterCommand("let dict2.print = dict.print")

    // Self is based on the dictionary used when calling
    assertCommandOutput("call dict2.print()", "dict2")
    assertCommandOutput("call dict2['print']()", "dict2")
    assertCommandOutput("call dict.print()", "dict")
    assertCommandOutput("call dict['print']()", "dict")
  }

  @Test
  fun `test self variable not copied when assigning dictionary function while creating new dictionary`() {
    addDictionaryFunction("echo self.name")
    enterCommand("let dict = {'name': 'dict', 'print': function('Print')}")
    enterCommand("let dict2 = {'name': 'dict2', 'print': dict.print}")

    assertCommandOutput("call dict2.print()", "dict2")
    assertCommandOutput("call dict.print()", "dict")
  }

  @Test
  fun `test partial dictionary function reuses captured dictionary when called from new dictionary`() {
    addDictionaryFunction("echo self.name")
    enterCommand("let dict = {'name': 'dict'}")
    enterCommand("let dict.print = function('Print', dict)")
    assertCommandOutput("call dict.print()", "dict")

    // dict2.print() still uses the captured "dict" dictionary
    enterCommand("let dict2 = {'name': 'dict2', 'print': dict.print}")
    assertCommandOutput("call dict2.print()", "dict")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test self is read-only`() {
    addDictionaryFunction("let self = []")
    enterCommand("let dict = {'name': 'dict', 'print': function('Print')}")
    enterCommand("call dict.print()")
    assertPluginError(true)
    assertPluginErrorMessage("E46: Cannot change read-only variable \"self\"")
  }

  @Test
  fun `test self in inner dictionary`() {
    addDictionaryFunction("echo self.name")
    enterCommand("let dict = {'name': 'dict', 'innerDict': {'name': 'innerDict', 'print': function('Print')}}")
    assertCommandOutput("call dict.innerDict.print()", "innerDict")
  }

  @Test
  fun `test calling dictionary function through variable`() {
    addDictionaryFunction("echo self.name")
    enterCommand("let dict = {'name': 'dict', 'print': function('Print')}")
    enterCommand("let PrintFr = dict.print")
    assertCommandOutput("call PrintFr()", "dict")
  }

  @Test
  fun `test echo dictionary containing dictionary function only prints function name`() {
    addDictionaryFunction("echo self.data")
    // This assigns a simple function reference to the dictionary. The entry in the dictionary in a single function
    // reference, so printing the dictionary prints the function name.
    enterCommand("let dict = {'print': function('Print'), 'data': 'something'}")
    assertCommandOutput("echo dict", "{'print': function('Print'), 'data': 'something'}")
  }

  @Test
  fun `test echo dictionary containing partial dictionary function prints captured dictionary`() {
    addDictionaryFunction("echo self.data")
    // The dictionary entry is a partial function to a new dictionary. Printing the dictionary prints the function
    // reference with name and the captured dictionary.
    enterCommand("let dict = {'print': function('Print', {'data': 'whatever'}), 'data': 'something'}")
    assertCommandOutput("echo dict", "{'print': function('Print', {'data': 'whatever'}), 'data': 'something'}")
  }

  @Test
  fun `test echo dictionary containing partial dictionary function assigned to captured dictionary`() {
    addDictionaryFunction("echo self.data")
    enterCommand("let dict = {'data': 'something'}")
    enterCommand("let dict['print'] = function('Print', dict)")
    assertCommandOutput("echo dict", "{'data': 'something', 'print': function('Print', {...})}")
  }

  @Test
  fun `test echo dictionary function accessed from dictionary treated as partial`() {
    addDictionaryFunction("echo self.data")
    enterCommand("let dict = {'print': function('Print'), 'data': 'something'}")
    // We assign a simple funcref to the dictionary, but when it's accessed as a dictionary indexed expression, it's
    // evaluated to a partial function
    assertCommandOutput("echo dict.print", "function('Print', {'print': function('Print'), 'data': 'something'})")
    assertCommandOutput("echo dict['print']", "function('Print', {'print': function('Print'), 'data': 'something'})")
  }

  private fun addDictionaryFunction(body: String) {
    enterCommand(
      """
         function Print() dict |
           $body |
         endfunction
      """.trimIndent(),
    )
  }
}
