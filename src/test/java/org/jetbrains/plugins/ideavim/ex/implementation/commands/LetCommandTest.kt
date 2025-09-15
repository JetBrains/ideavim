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
}
