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
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionAccessScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LetCommandTest : VimTestCase() {

  @Test
  fun `test assignment to string`() {
    configureByText("\n")
    enterCommand("let s = \"foo\"")
    assertCommandOutput("echo s", "foo\n")
  }

  @Test
  fun `test assignment to number`() {
    configureByText("\n")
    enterCommand("let s = 100")
    assertCommandOutput("echo s", "100\n")
  }

  @Test
  fun `test assignment to expression`() {
    configureByText("\n")
    enterCommand("let s = 10 + 20 * 4")
    assertCommandOutput("echo s", "90\n")
  }

  @Test
  fun `test adding new pair to dictionary`() {
    configureByText("\n")
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s['key2'] = 2")
    assertCommandOutput("echo s", "{'key1': 1, 'key2': 2}\n")
  }

  @Test
  fun `test editing existing pair in dictionary`() {
    configureByText("\n")
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s['key1'] = 2")
    assertCommandOutput("echo s", "{'key1': 2}\n")
  }

  @Test
  fun `test assignment plus operator`() {
    configureByText("\n")
    enterCommand("let s = 10")
    enterCommand("let s += 5")
    assertCommandOutput("echo s", "15\n")
  }

  @Test
  fun `test changing list item`() {
    configureByText("\n")
    enterCommand("let s = [1, 1]")
    enterCommand("let s[1] = 2")
    assertCommandOutput("echo s", "[1, 2]\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test changing list item with index out of range`() {
    configureByText("\n")
    enterCommand("let s = [1, 1]")
    enterCommand("let s[2] = 2")
    assertPluginError(true)
    assertPluginErrorMessageContains("E684: list index out of range: 2")
  }

  @Test
  fun `test changing list with sublist expression`() {
    configureByText("\n")
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = [5, 4]")
    assertCommandOutput("echo s", "[5, 4, 3]\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test changing list with sublist expression and larger list`() {
    configureByText("\n")
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = [5, 4, 3, 2, 1]")
    assertPluginError(true)
    assertPluginErrorMessageContains("E710: List value has more items than targets")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test changing list with sublist expression and smaller list`() {
    configureByText("\n")
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = [5]")
    assertPluginError(true)
    assertPluginErrorMessageContains("E711: List value does not have enough items")
  }

  @Test
  fun `test changing list with sublist expression and undefined end`() {
    configureByText("\n")
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[1:] = [5, 5, 5, 5]")
    assertCommandOutput("echo s", "[1, 5, 5, 5, 5]\n")
  }

  @Test
  fun `test let option updates toggle option with number value`() {
    configureByText("\n")
    enterCommand("let &incsearch=1")
    assertTrue(options().incsearch)
    enterCommand("let &incsearch = 0")
    assertFalse(options().incsearch)
  }

  @Test
  fun `test let option without scope behaves like set`() {
    configureByText("\n")

    // 'number' is a local-to-window toggle option
    enterCommand("let &number = 12")
    val globalValue = injector.optionGroup.getOptionValue(Options.number, OptionAccessScope.GLOBAL)
    val localValue = injector.optionGroup.getOptionValue(Options.number, OptionAccessScope.LOCAL(fixture.editor.vim))
    assertEquals(12, globalValue.value)
    assertEquals(12, localValue.value)
    assertTrue(options().number)
  }

  @Test
  fun `test let option with local scope behaves like setlocal`() {
    configureByText("\n")

    // 'number' is a local-to-window option
    enterCommand("let &l:number = 12")
    val globalValue = injector.optionGroup.getOptionValue(Options.number, OptionAccessScope.GLOBAL)
    val localValue = injector.optionGroup.getOptionValue(Options.number, OptionAccessScope.LOCAL(fixture.editor.vim))
    assertEquals(0, globalValue.value)
    assertEquals(12, localValue.value)
    assertTrue(options().number)
  }

  @Test
  fun `test let option with global scope behaves like setglobal`() {
    configureByText("\n")

    // 'number' is a local-to-window option
    enterCommand("let &g:number = 12")
    val globalValue = injector.optionGroup.getOptionValue(Options.number, OptionAccessScope.GLOBAL)
    val localValue = injector.optionGroup.getOptionValue(Options.number, OptionAccessScope.LOCAL(fixture.editor.vim))
    assertEquals(12, globalValue.value)
    assertEquals(0, localValue.value)
    assertFalse(options().number)
  }

  @Test
  fun `test let option with operator and no scope`() {
    configureByText("\n")

    // 'scroll' is a local to window number option
    enterCommand("set scroll=42")
    enterCommand("let &scroll+=10")
    val globalValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.GLOBAL)
    val localValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.LOCAL(fixture.editor.vim))
    assertEquals(52, globalValue.value)
    assertEquals(52, localValue.value)
    assertEquals(52, options().scroll)
  }

  @Test
  fun `test let local option with operator`() {
    configureByText("\n")

    enterCommand("setlocal scroll=42")
    enterCommand("let &l:scroll+=10")
    val globalValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.GLOBAL)
    val localValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.LOCAL(fixture.editor.vim))
    assertEquals(0, globalValue.value)
    assertEquals(52, localValue.value)
    assertEquals(52, options().scroll)
  }

  @Test
  fun `test let global option with operator`() {
    configureByText("\n")

    enterCommand("setglobal scroll=42")
    enterCommand("let &g:scroll+=10")
    val globalValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.GLOBAL)
    val localValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.LOCAL(fixture.editor.vim))
    assertEquals(52, globalValue.value)
    assertEquals(0, localValue.value)
    assertEquals(0, options().scroll)
  }

  @Test
  fun `test comment`() {
    configureByText("\n")
    enterCommand("let s = [1, 2, 3] \" my list for storing numbers")
    assertCommandOutput("echo s", "[1, 2, 3]\n")
  }

  @Test
  fun `test vimScriptGlobalEnvironment`() {
    configureByText("\n")
    enterCommand("let g:WhichKey_ShowVimActions = \"true\"")
    assertCommandOutput("echo g:WhichKey_ShowVimActions", "true\n")
    assertEquals("true", VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_ShowVimActions"])
  }

  @Test
  fun `test list is passed by reference`() {
    configureByText("\n")
    enterCommand("let list = [1, 2, 3]")
    enterCommand("let l2 = list")
    enterCommand("let list += [4]")
    assertCommandOutput("echo l2", "[1, 2, 3, 4]\n")
  }

  @Test
  fun `test list is passed by reference 2`() {
    configureByText("\n")
    enterCommand("let list = [1, 2, 3, []]")
    enterCommand("let l2 = list")
    enterCommand("let list[3] += [4]")
    assertCommandOutput("echo l2", "[1, 2, 3, [4]]\n")
  }

  @Test
  fun `test list is passed by reference 3`() {
    configureByText("\n")
    enterCommand("let list = [1, 2, 3, []]")
    enterCommand("let dict = {}")
    enterCommand("let dict.l2 = list")
    enterCommand("let list[3] += [4]")
    assertCommandOutput("echo dict.l2", "[1, 2, 3, [4]]\n")
  }

  @Test
  fun `test list is passed by reference 4`() {
    configureByText("\n")
    enterCommand("let list = [1, 2, 3]")
    enterCommand("let dict = {}")
    enterCommand("let dict.l2 = list")
    enterCommand("let dict.l2 += [4]")
    assertCommandOutput("echo dict.l2", "[1, 2, 3, 4]\n")
  }

  @Test
  fun `test number is passed by value`() {
    configureByText("\n")
    enterCommand("let number = 10")
    enterCommand("let n2 = number")
    enterCommand("let number += 2")
    assertCommandOutput("echo n2", "10\n")
  }

  @Test
  fun `test string is passed by value`() {
    configureByText("\n")
    enterCommand("let string = 'abc'")
    enterCommand("let str2 = string")
    enterCommand("let string .= 'd'")
    assertCommandOutput("echo str2", "abc\n")
  }

  @Test
  fun `test dict is passed by reference`() {
    configureByText("\n")
    enterCommand("let dictionary = {}")
    enterCommand("let dict2 = dictionary")
    enterCommand("let dictionary.one = 1")
    enterCommand("let dictionary['two'] = 2")
    assertCommandOutput("echo dict2", "{'one': 1, 'two': 2}\n")
  }

  @Test
  fun `test dict is passed by reference 2`() {
    configureByText("\n")
    enterCommand("let list = [1, 2, 3, {'a': 'b'}]")
    enterCommand("let dict = list[3]")
    enterCommand("let list[3].key = 'value'")
    assertCommandOutput("echo dict", "{'a': 'b', 'key': 'value'}\n")
  }

  @Test
  fun `test numbered register`() {
    configureByText("\n")
    enterCommand("let @4 = 'inumber register works'")
    assertCommandOutput("echo @4", "inumber register works\n")

    typeText("@4")
    assertState("number register works\n")
  }

  @Test
  fun `test lowercase letter register`() {
    configureByText("\n")
    enterCommand("let @o = 'ilowercase letter register works'")
    assertCommandOutput("echo @o", "ilowercase letter register works\n")

    typeText("@o")
    assertState("lowercase letter register works\n")
  }

  @Test
  fun `test uppercase letter register`() {
    configureByText("\n")
    enterCommand("let @O = 'iuppercase letter register works'")
    assertCommandOutput("echo @O", "iuppercase letter register works\n")

    typeText("@O")
    assertState("uppercase letter register works\n")
    typeText("<Esc>")

    enterCommand("let @O = '!'")
    assertCommandOutput("echo @O", "iuppercase letter register works!\n")
  }

  @Test
  fun `test unnamed register`() {
    configureByText("\n")
    enterCommand("let @\" = 'iunnamed register works'")
    assertCommandOutput("echo @\"", "iunnamed register works\n")

    typeText("@\"")
    assertState("unnamed register works\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test define script variable with command line context`() {
    configureByText("\n")
    enterCommand("let s:my_var = 'oh, hi Mark'")
    assertPluginError(true)
    assertPluginErrorMessageContains("E461: Illegal variable name: s:my_var")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test define local variable with command line context`() {
    configureByText("\n")
    enterCommand("let l:my_var = 'oh, hi Mark'")
    assertPluginError(true)
    assertPluginErrorMessageContains("E461: Illegal variable name: l:my_var")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test define function variable with command line context`() {
    configureByText("\n")
    enterCommand("let a:my_var = 'oh, hi Mark'")
    assertPluginError(true)
    assertPluginErrorMessageContains("E461: Illegal variable name: a:my_var")
  }
}
