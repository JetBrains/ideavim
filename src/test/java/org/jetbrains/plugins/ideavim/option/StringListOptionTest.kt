/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.StringOption
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class StringListOptionTest : VimTestCase() {
  private val optionName = "myOpt"
  private val option = StringOption(optionName, optionName, "", true, null)

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    injector.optionGroup.addOption(option)
    configureByText("\n")
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(super.testInfo)
    injector.optionGroup.removeOption(optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append existing value`() {
    enterCommand("set $optionName+=123")
    enterCommand("set $optionName+=456")
    enterCommand("set $optionName+=123")

    kotlin.test.assertEquals("123,456", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend existing value`() {
    enterCommand("set $optionName+=456")
    enterCommand("set $optionName+=123")
    enterCommand("set $optionName^=123")

    kotlin.test.assertEquals("456,123", options().getStringValue(option))
  }
}
