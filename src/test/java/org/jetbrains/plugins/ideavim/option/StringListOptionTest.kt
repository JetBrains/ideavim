/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringListOption
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

class StringListOptionTest : VimTestCase() {
  private val optionName = "myOpt"
  private val option = StringListOption(optionName, optionName, "", null)

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

  private fun getOptionValue() =
    injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(fixture.editor.vim)).value

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append existing value`() {
    enterCommand("set $optionName+=123")
    enterCommand("set $optionName+=456")
    enterCommand("set $optionName+=123")

    assertEquals("123,456", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend existing value`() {
    enterCommand("set $optionName+=456")
    enterCommand("set $optionName+=123")
    enterCommand("set $optionName^=123")

    assertEquals("456,123", getOptionValue())
  }
}
