/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class LetCommandTest : VimTestCase() {

  @Test
  fun `test assignment to string`() {
    configureByText("\n")
    typeText(commandToKeys("let s = \"foo\""))
    typeText(commandToKeys("echo s"))
    assertExOutput("foo\n")
  }

  @Test
  fun `test assignment to number`() {
    configureByText("\n")
    typeText(commandToKeys("let s = 100"))
    typeText(commandToKeys("echo s"))
    assertExOutput("100\n")
  }

  @Test
  fun `test assignment to expression`() {
    configureByText("\n")
    typeText(commandToKeys("let s = 10 + 20 * 4"))
    typeText(commandToKeys("echo s"))
    assertExOutput("90\n")
  }

  @Test
  fun `test adding new pair to dictionary`() {
    configureByText("\n")
    typeText(commandToKeys("let s = {'key1' : 1}"))
    typeText(commandToKeys("let s['key2'] = 2"))
    typeText(commandToKeys("echo s"))
    assertExOutput("{'key1': 1, 'key2': 2}\n")
  }

  @Test
  fun `test editing existing pair in dictionary`() {
    configureByText("\n")
    typeText(commandToKeys("let s = {'key1' : 1}"))
    typeText(commandToKeys("let s['key1'] = 2"))
    typeText(commandToKeys("echo s"))
    assertExOutput("{'key1': 2}\n")
  }

  @Test
  fun `test assignment plus operator`() {
    configureByText("\n")
    typeText(commandToKeys("let s = 10"))
    typeText(commandToKeys("let s += 5"))
    typeText(commandToKeys("echo s"))
    assertExOutput("15\n")
  }

  @Test
  fun `test changing list item`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 1]"))
    typeText(commandToKeys("let s[1] = 2"))
    typeText(commandToKeys("echo s"))
    assertExOutput("[1, 2]\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test changing list item with index out of range`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 1]"))
    typeText(commandToKeys("let s[2] = 2"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E684: list index out of range: 2")
  }

  @Test
  fun `test changing list with sublist expression`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3]"))
    typeText(commandToKeys("let s[0:1] = [5, 4]"))
    typeText(commandToKeys("echo s"))
    assertExOutput("[5, 4, 3]\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test changing list with sublist expression and larger list`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3]"))
    typeText(commandToKeys("let s[0:1] = [5, 4, 3, 2, 1]"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E710: List value has more items than targets")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test changing list with sublist expression and smaller list`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3]"))
    typeText(commandToKeys("let s[0:1] = [5]"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E711: List value does not have enough items")
  }

  @Test
  fun `test changing list with sublist expression and undefined end`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3]"))
    typeText(commandToKeys("let s[1:] = [5, 5, 5, 5]"))
    typeText(commandToKeys("echo s"))
    assertExOutput("[1, 5, 5, 5, 5]\n")
  }

  @Test
  fun `test let option`() {
    configureByText("\n")
    typeText(commandToKeys("set noincsearch"))
    kotlin.test.assertFalse(options().isSet(Options.incsearch))
    typeText(commandToKeys("let &incsearch = 12"))
    kotlin.test.assertTrue(options().isSet(Options.incsearch))
    typeText(commandToKeys("set noincsearch"))
    kotlin.test.assertFalse(options().isSet(Options.incsearch))
  }

  @Test
  fun `test let option2`() {
    configureByText("\n")
    typeText(commandToKeys("set incsearch"))
    kotlin.test.assertTrue(options().isSet(Options.incsearch))
    typeText(commandToKeys("let &incsearch = 0"))
    kotlin.test.assertFalse(options().isSet(Options.incsearch))
  }

  @Test
  fun `test comment`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3] \" my list for storing numbers"))
    typeText(commandToKeys("echo s"))
    assertExOutput("[1, 2, 3]\n")
  }

  @Test
  fun `test vimScriptGlobalEnvironment`() {
    configureByText("\n")
    typeText(commandToKeys("let g:WhichKey_ShowVimActions = \"true\""))
    typeText(commandToKeys("echo g:WhichKey_ShowVimActions"))
    assertExOutput("true\n")
    kotlin.test.assertEquals("true", VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_ShowVimActions"])
  }

  @Test
  fun `test list is passed by reference`() {
    configureByText("\n")
    typeText(commandToKeys("let list = [1, 2, 3]"))
    typeText(commandToKeys("let l2 = list"))
    typeText(commandToKeys("let list += [4]"))
    typeText(commandToKeys("echo l2"))

    assertExOutput("[1, 2, 3, 4]\n")
  }

  @Test
  fun `test list is passed by reference 2`() {
    configureByText("\n")
    typeText(commandToKeys("let list = [1, 2, 3, []]"))
    typeText(commandToKeys("let l2 = list"))
    typeText(commandToKeys("let list[3] += [4]"))
    typeText(commandToKeys("echo l2"))

    assertExOutput("[1, 2, 3, [4]]\n")
  }

  @Test
  fun `test list is passed by reference 3`() {
    configureByText("\n")
    typeText(commandToKeys("let list = [1, 2, 3, []]"))
    typeText(commandToKeys("let dict = {}"))
    typeText(commandToKeys("let dict.l2 = list"))
    typeText(commandToKeys("let list[3] += [4]"))
    typeText(commandToKeys("echo dict.l2"))

    assertExOutput("[1, 2, 3, [4]]\n")
  }

  @Test
  fun `test list is passed by reference 4`() {
    configureByText("\n")
    typeText(commandToKeys("let list = [1, 2, 3]"))
    typeText(commandToKeys("let dict = {}"))
    typeText(commandToKeys("let dict.l2 = list"))
    typeText(commandToKeys("let dict.l2 += [4]"))
    typeText(commandToKeys("echo dict.l2"))

    assertExOutput("[1, 2, 3, 4]\n")
  }

  @Test
  fun `test number is passed by value`() {
    configureByText("\n")
    typeText(commandToKeys("let number = 10"))
    typeText(commandToKeys("let n2 = number"))
    typeText(commandToKeys("let number += 2"))
    typeText(commandToKeys("echo n2"))

    assertExOutput("10\n")
  }

  @Test
  fun `test string is passed by value`() {
    configureByText("\n")
    typeText(commandToKeys("let string = 'abc'"))
    typeText(commandToKeys("let str2 = string"))
    typeText(commandToKeys("let string .= 'd'"))
    typeText(commandToKeys("echo str2"))

    assertExOutput("abc\n")
  }

  @Test
  fun `test dict is passed by reference`() {
    configureByText("\n")
    typeText(commandToKeys("let dictionary = {}"))
    typeText(commandToKeys("let dict2 = dictionary"))
    typeText(commandToKeys("let dictionary.one = 1"))
    typeText(commandToKeys("let dictionary['two'] = 2"))
    typeText(commandToKeys("echo dict2"))

    assertExOutput("{'one': 1, 'two': 2}\n")
  }

  @Test
  fun `test dict is passed by reference 2`() {
    configureByText("\n")
    typeText(commandToKeys("let list = [1, 2, 3, {'a': 'b'}]"))
    typeText(commandToKeys("let dict = list[3]"))
    typeText(commandToKeys("let list[3].key = 'value'"))
    typeText(commandToKeys("echo dict"))

    assertExOutput("{'a': 'b', 'key': 'value'}\n")
  }

  @Test
  fun `test numbered register`() {
    configureByText("\n")
    typeText(commandToKeys("let @4 = 'inumber register works'"))
    typeText(commandToKeys("echo @4"))
    assertExOutput("inumber register works\n")

    typeText(injector.parser.parseKeys("@4"))
    assertState("number register works\n")
  }

  @Test
  fun `test lowercase letter register`() {
    configureByText("\n")
    typeText(commandToKeys("let @o = 'ilowercase letter register works'"))
    typeText(commandToKeys("echo @o"))
    assertExOutput("ilowercase letter register works\n")

    typeText(injector.parser.parseKeys("@o"))
    assertState("lowercase letter register works\n")
  }

  @Test
  fun `test uppercase letter register`() {
    configureByText("\n")
    typeText(commandToKeys("let @O = 'iuppercase letter register works'"))
    typeText(commandToKeys("echo @O"))
    assertExOutput("iuppercase letter register works\n")

    typeText(injector.parser.parseKeys("@O"))
    assertState("uppercase letter register works\n")
    typeText(injector.parser.parseKeys("<Esc>"))

    typeText(commandToKeys("let @O = '!'"))
    typeText(commandToKeys("echo @O"))
    assertExOutput("iuppercase letter register works!\n")
  }

  @Test
  fun `test unnamed register`() {
    configureByText("\n")
    typeText(commandToKeys("let @\" = 'iunnamed register works'"))
    typeText(commandToKeys("echo @\""))
    assertExOutput("iunnamed register works\n")

    typeText(injector.parser.parseKeys("@\""))
    assertState("unnamed register works\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test define script variable with command line context`() {
    configureByText("\n")
    typeText(commandToKeys("let s:my_var = 'oh, hi Mark'"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E461: Illegal variable name: s:my_var")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test define local variable with command line context`() {
    configureByText("\n")
    typeText(commandToKeys("let l:my_var = 'oh, hi Mark'"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E461: Illegal variable name: l:my_var")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test define function variable with command line context`() {
    configureByText("\n")
    typeText(commandToKeys("let a:my_var = 'oh, hi Mark'"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E461: Illegal variable name: a:my_var")
  }
}
