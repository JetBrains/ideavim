/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.sneak

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.extension.sneak.IdeaVimSneakExtension
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class IdeaVimSneakLabelsTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("sneak")
    configureByText("\n")
    executeVimscript("let g:{'sneak#label'} = 1")
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    IdeaVimSneakExtension.stopTimer()
    super.tearDown(testInfo)
  }

  @Test
  fun `test sneak label select third visible match`() {
    val before = "som${c}e text text text text".trimIndent()
    val after = "some text text text te${c}xt".trimIndent()
    doTest("sxtf", before, after, Mode.NORMAL())
  }

  @Test
  fun `test sneak label non-existing char is passed through`() {
    val before = "som${c}e text text".trimIndent()
    val after = "some text text".trimIndent()
    doTest("sxtA", before, after, Mode.INSERT)
  }
}
