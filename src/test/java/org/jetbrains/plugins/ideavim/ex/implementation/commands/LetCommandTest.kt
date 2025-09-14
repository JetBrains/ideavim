/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LetCommandTest : VimTestCase("\n") {
  @Test
  fun `test adding new pair to dictionary`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s['key2'] = 2")
    assertCommandOutput("echo s", "{'key1': 1, 'key2': 2}")
  }

  @Test
  fun `test editing existing pair in dictionary`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s['key1'] = 2")
    assertCommandOutput("echo s", "{'key1': 2}")
  }

  @Test
  fun `test changing list item`() {
    enterCommand("let s = [1, 1]")
    enterCommand("let s[1] = 2")
    assertCommandOutput("echo s", "[1, 2]")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test changing list item with index out of range`() {
    enterCommand("let s = [1, 1]")
    enterCommand("let s[2] = 2")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: 2")
  }

  @Test
  fun `test changing list with sublist expression`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = [5, 4]")
    assertCommandOutput("echo s", "[5, 4, 3]")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test changing list with sublist expression and larger list`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = [5, 4, 3, 2, 1]")
    assertPluginError(true)
    assertPluginErrorMessage("E710: List value has more items than targets")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test changing list with sublist expression and smaller list`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = [5]")
    assertPluginError(true)
    assertPluginErrorMessage("E711: List value does not have enough items")
  }

  @Test
  fun `test changing list with sublist expression and undefined end`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[1:] = [5, 5, 5, 5]")
    assertCommandOutput("echo s", "[1, 5, 5, 5, 5]")
  }

  @Test
  fun `test comment`() {
    enterCommand("let s = [1, 2, 3] \" my list for storing numbers")
    assertCommandOutput("echo s", "[1, 2, 3]")
  }

  @Test
  fun `test vimScriptGlobalEnvironment`() {
    enterCommand("let g:WhichKey_ShowVimActions = \"true\"")
    assertCommandOutput("echo g:WhichKey_ShowVimActions", "true")
    assertEquals("true", VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_ShowVimActions"])
  }

  @Test
  fun `test list is passed by reference`() {
    enterCommand("let list = [1, 2, 3]")
    enterCommand("let l2 = list")
    enterCommand("let list += [4]")
    assertCommandOutput("echo l2", "[1, 2, 3, 4]")
  }

  @Test
  fun `test list is passed by reference 2`() {
    enterCommand("let list = [1, 2, 3, []]")
    enterCommand("let l2 = list")
    enterCommand("let list[3] += [4]")
    assertCommandOutput("echo l2", "[1, 2, 3, [4]]")
  }

  @Test
  fun `test list is passed by reference 3`() {
    enterCommand("let list = [1, 2, 3, []]")
    enterCommand("let dict = {}")
    enterCommand("let dict.l2 = list")
    enterCommand("let list[3] += [4]")
    assertCommandOutput("echo dict.l2", "[1, 2, 3, [4]]")
  }

  @Test
  fun `test list is passed by reference 4`() {
    enterCommand("let list = [1, 2, 3]")
    enterCommand("let dict = {}")
    enterCommand("let dict.l2 = list")
    enterCommand("let dict.l2 += [4]")
    assertCommandOutput("echo dict.l2", "[1, 2, 3, 4]")
  }

  @Test
  fun `test number is passed by value`() {
    enterCommand("let number = 10")
    enterCommand("let n2 = number")
    enterCommand("let number += 2")
    assertCommandOutput("echo n2", "10")
  }

  @Test
  fun `test string is passed by value`() {
    enterCommand("let string = 'abc'")
    enterCommand("let str2 = string")
    enterCommand("let string .= 'd'")
    assertCommandOutput("echo str2", "abc")
  }

  @Test
  fun `test dict is passed by reference`() {
    enterCommand("let dictionary = {}")
    enterCommand("let dict2 = dictionary")
    enterCommand("let dictionary.one = 1")
    enterCommand("let dictionary['two'] = 2")
    assertCommandOutput("echo dict2", "{'one': 1, 'two': 2}")
  }

  @Test
  fun `test dict is passed by reference 2`() {
    enterCommand("let list = [1, 2, 3, {'a': 'b'}]")
    enterCommand("let dict = list[3]")
    enterCommand("let list[3].key = 'value'")
    assertCommandOutput("echo dict", "{'a': 'b', 'key': 'value'}")
  }

  @Test
  fun `test numbered register`() {
    enterCommand("let @4 = 'inumber register works'")
    assertCommandOutput("echo @4", "inumber register works")

    typeText("@4")
    assertState("number register works\n")
  }

  @Test
  fun `test lowercase letter register`() {
    enterCommand("let @o = 'ilowercase letter register works'")
    assertCommandOutput("echo @o", "ilowercase letter register works")

    typeText("@o")
    assertState("lowercase letter register works\n")
  }

  @Test
  fun `test uppercase letter register`() {
    enterCommand("let @O = 'iuppercase letter register works'")
    assertCommandOutput("echo @O", "iuppercase letter register works")

    typeText("@O")
    assertState("uppercase letter register works\n")
    typeText("<Esc>")

    enterCommand("let @O = '!'")
    assertCommandOutput("echo @O", "iuppercase letter register works!")
  }

  @Test
  fun `test unnamed register`() {
    enterCommand("let @\" = 'iunnamed register works'")
    assertCommandOutput("echo @\"", "iunnamed register works")

    typeText("@\"")
    assertState("unnamed register works\n")
  }
}
