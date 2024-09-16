/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

class ClipboardOptionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(super.testInfo)
  }

  private fun getOptionValue() = injector.globalOptions().clipboard

  @Test
  fun `test appended to exclude values are ignored`() {
    enterCommand("set clipboard=autoselect,exclude:cons\\|linux")
    enterCommand("set clipboard+=unnamed")
    assertEquals(listOf("autoselect", "exclude:cons\\|linux,unnamed"), getOptionValue())
  }

  @Test
  fun `test prepend value`() {
    enterCommand("set clipboard=autoselect,exclude:cons\\|linux")
    enterCommand("set clipboard^=unnamed")
    assertEquals(listOf("unnamed", "autoselect", "exclude:cons\\|linux"), getOptionValue())
  }
}