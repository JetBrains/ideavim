/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class BuiltinFunctionHandlerTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText(
      """
        |one
        |two
        |th${c}ree
        |four
        |five
        |six
        |seven
        |eight
      """.trimMargin()
    )
  }

  @Test
  fun `test caret not moved when no range specified`() {
    enterCommand("let l=[]")
    enterCommand("call add(l, getline('.'))")
    assertPosition(2, 2)
  }

  @Test
  fun `test function called on current line when no range specified`() {
    enterCommand("let l=[]")
    enterCommand("call add(l, getline('.'))")
    assertCommandOutput("echo l", "['three']")
  }

  @Test
  fun `test caret moved to start of current line when range is current line`() {
    enterCommand("let l=[]")
    typeText("V")
    enterCommand("call add(l, getline('.'))") // This will be :'<,'>call add(...)
    assertPosition(2, 0)
  }

  @Test
  fun `test function called with re-evaluated arguments for each line in range`() {
    enterCommand("let l=[]")
    typeText("Vjj")
    enterCommand("call add(l, getline('.'))")
    assertCommandOutput("echo l", "['three', 'four', 'five']")
  }

  @Test
  fun `test caret moved to start of last line in range once complete`() {
    enterCommand("let l=[]")
    typeText("Vjj")
    enterCommand("call add(l, getline('.'))")
    assertPosition(4, 0)
  }

  @Test
  fun `test builtin function reports invalid range`() {
    enterCommand("let l=[]")
    enterCommand("1000,1010call add(l, getline('.'))")
    assertPluginError(true)
    assertPluginErrorMessage("E16: Invalid range")
  }
}
