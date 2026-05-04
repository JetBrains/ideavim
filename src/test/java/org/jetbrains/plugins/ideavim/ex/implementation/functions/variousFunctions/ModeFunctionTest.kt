/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.variousFunctions

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class ModeFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test mode in normal mode returns n`() {
    assertCommandOutput("echo mode()", "n")
  }

  @Test
  fun `test mode with zero argument in normal mode returns n`() {
    assertCommandOutput("echo mode(0)", "n")
  }

  @Test
  fun `test mode with truthy argument in normal mode returns n`() {
    assertCommandOutput("echo mode(1)", "n")
  }

  @Test
  fun `test mode with string argument in normal mode returns n`() {
    assertCommandOutput("echo mode('x')", "n")
  }

  @Test
  fun `test mode reports too many arguments`() {
    enterCommand("echo mode(0, 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: mode")
  }

  @Test
  fun `test mode in insert returns i`() {
    configureByText("\n")
    enterCommand("inoremap <expr> q mode()")
    typeText(injector.parser.parseKeys("iq<esc>"))
    assertState("i\n")
  }

  @Test
  fun `test mode in replace returns R`() {
    configureByText("a\n")
    enterCommand("inoremap <expr> q mode()")
    typeText(injector.parser.parseKeys("Rq<esc>"))
    assertState("R\n")
  }

  @Test
  fun `test mode in visual char-wise returns v`() {
    configureByText("abc\n")
    enterCommand("vmap <expr> q '<Esc>A - mode='.mode().'<Esc>'")
    typeText(injector.parser.parseKeys("vq"))
    assertState("abc - mode=v\n")
  }

  @Test
  fun `test mode in visual line-wise returns V`() {
    configureByText("abc\n")
    enterCommand("vmap <expr> q '<Esc>A - mode='.mode().'<Esc>'")
    typeText(injector.parser.parseKeys("Vq"))
    assertState("abc - mode=V\n")
  }
}
