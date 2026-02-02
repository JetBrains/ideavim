/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.autocmd

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class AugroupTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
    enterCommand("autocmd!")
  }

  @Test
  fun `should register autocmd inside augroup`() {
    enterCommand("augroup TestGroup")
    enterCommand("autocmd InsertEnter * echo 23")
    enterCommand("augroup END")

    typeText(injector.parser.parseKeys("i"))
    assertExOutput("23")
  }

  @Test
  fun `autocmd bang inside augroup should clear only that group`() {
    enterCommand("augroup G1")
    enterCommand("autocmd InsertEnter * echo 1")
    enterCommand("augroup END")

    enterCommand("augroup G2")
    enterCommand("autocmd InsertEnter * echo 2")
    enterCommand("augroup END")

    enterCommand("augroup G1")
    enterCommand("autocmd!")
    enterCommand("augroup END")

    typeText(injector.parser.parseKeys("i"))
    assertExOutput("2")
  }

  @Test
  fun `augroup bang should remove all handlers from group`() {
    enterCommand("augroup TestGroup")
    enterCommand("autocmd InsertEnter * echo 23")
    enterCommand("augroup END")

    enterCommand("augroup! TestGroup")

    typeText(injector.parser.parseKeys("i"))
    assertNoExOutput()
  }

  @Test
  fun `augroup should allow redefining group without bang (append handlers)`() {
    enterCommand("augroup TestGroup")
    enterCommand("autocmd InsertEnter * echo 1")
    enterCommand("augroup END")

    enterCommand("augroup TestGroup")
    enterCommand("autocmd InsertEnter * echo 2")
    enterCommand("augroup END")

    typeText(injector.parser.parseKeys("i"))
    assertExOutput("1\n2")
  }

  @Test
  fun `augroup bang should redefine group (drop previous handlers)`() {
    enterCommand("augroup TestGroup")
    enterCommand("autocmd InsertEnter * echo 1")
    enterCommand("augroup END")

    enterCommand("augroup! TestGroup")
    enterCommand("augroup TestGroup")
    enterCommand("autocmd InsertEnter * echo 2")
    enterCommand("augroup END")

    typeText(injector.parser.parseKeys("i"))
    assertExOutput("2")
  }

  @Test
  fun `should keep groups independent`() {
    enterCommand("augroup G1")
    enterCommand("autocmd InsertEnter * echo 1")
    enterCommand("augroup END")

    enterCommand("augroup G2")
    enterCommand("autocmd InsertLeave * echo 2")
    enterCommand("augroup END")

    typeText(injector.parser.parseKeys("i"))
    assertExOutput("1")

    typeText(injector.parser.parseKeys("<esc>"))
    assertState(Mode.NORMAL())
    assertExOutput("2")
  }
}