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
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.StringListOption
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

class BoundedStringListOptionTest : VimTestCase() {
  private val optionName = "myOpt"
  private val defaultValue = "Monday,Tuesday"
  private val option = StringListOption(
    optionName,
    OptionDeclaredScope.GLOBAL,
    optionName,
    defaultValue,
    setOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
  )

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
    injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)).value

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test set valid list`() {
    enterCommand("set $optionName=Thursday,Friday")
    assertEquals("Thursday,Friday", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test set list with invalid value`() {
    enterCommand("set $optionName=Blue")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: $optionName=Blue")
    assertEquals(defaultValue, getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append single item`() {
    enterCommand("set $optionName+=Wednesday")
    assertEquals("Monday,Tuesday,Wednesday", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append invalid item`() {
    enterCommand("set $optionName+=Blue")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: $optionName+=Blue")
    assertEquals("Monday,Tuesday", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append list`() {
    enterCommand("set $optionName+=Wednesday,Thursday")
    assertEquals("Monday,Tuesday,Wednesday,Thursday", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append list with invalid item`() {
    enterCommand("set $optionName+=Wednesday,Blue")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: $optionName+=Wednesday,Blue")
    assertEquals("Monday,Tuesday", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend item`() {
    enterCommand("set $optionName^=Wednesday")
    assertEquals("Wednesday,Monday,Tuesday", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend invalid item`() {
    enterCommand("set $optionName^=Blue")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: $optionName^=Blue")
    assertEquals("Monday,Tuesday", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend list`() {
    enterCommand("set $optionName^=Wednesday,Thursday")
    assertEquals("Wednesday,Thursday,Monday,Tuesday", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend list with invalid item`() {
    enterCommand("set $optionName^=Wednesday,Blue")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: $optionName^=Wednesday,Blue")
    assertEquals("Monday,Tuesday", getOptionValue())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test remove item`() {
    enterCommand("set $optionName-=Monday")
    assertEquals("Tuesday", getOptionValue())
  }

  @Test
  fun `test remove list`() {
    enterCommand("set $optionName-=Monday,Tuesday")
    assertEquals("", getOptionValue())
  }

  @Test
  fun `test remove list with wrong order`() {
    enterCommand("set $optionName-=Tuesday,Monday")
    assertEquals("Monday,Tuesday", getOptionValue())
  }

  @Test
  fun `test remove list with invalid value`() {
    enterCommand("set $optionName-=Monday,Blue")
    assertEquals("Monday,Tuesday", getOptionValue())
  }
}
