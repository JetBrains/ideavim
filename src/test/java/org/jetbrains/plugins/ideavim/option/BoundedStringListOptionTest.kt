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

class BoundedStringListOptionTest : VimTestCase() {
  private val optionName = "myOpt"
  private val defaultValue = "Monday,Tuesday"
  private val option = StringOption(
    optionName,
    optionName,
    defaultValue,
    true,
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

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test set valid list`() {
    enterCommand("set $optionName=Thursday,Friday")
    kotlin.test.assertEquals("Thursday,Friday", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test set list with invalid value`() {
    enterCommand("set $optionName=Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    kotlin.test.assertEquals(defaultValue, options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append single item`() {
    enterCommand("set $optionName+=Wednesday")
    kotlin.test.assertEquals("Monday,Tuesday,Wednesday", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append invalid item`() {
    enterCommand("set $optionName+=Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    kotlin.test.assertEquals("Monday,Tuesday", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append list`() {
    enterCommand("set $optionName+=Wednesday,Thursday")
    kotlin.test.assertEquals("Monday,Tuesday,Wednesday,Thursday", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test append list with invalid item`() {
    enterCommand("set $optionName+=Wednesday,Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    kotlin.test.assertEquals("Monday,Tuesday", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend item`() {
    enterCommand("set $optionName^=Wednesday")
    kotlin.test.assertEquals("Wednesday,Monday,Tuesday", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend invalid item`() {
    enterCommand("set $optionName^=Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    kotlin.test.assertEquals("Monday,Tuesday", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend list`() {
    enterCommand("set $optionName^=Wednesday,Thursday")
    kotlin.test.assertEquals("Wednesday,Thursday,Monday,Tuesday", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test prepend list with invalid item`() {
    enterCommand("set $optionName^=Wednesday,Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    kotlin.test.assertEquals("Monday,Tuesday", options().getStringValue(option))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test remove item`() {
    enterCommand("set $optionName-=Monday")
    kotlin.test.assertEquals("Tuesday", options().getStringValue(option))
  }

  @Test
  fun `test remove list`() {
    enterCommand("set $optionName-=Monday,Tuesday")
    kotlin.test.assertEquals("", options().getStringValue(option))
  }

  @Test
  fun `test remove list with wrong order`() {
    enterCommand("set $optionName-=Tuesday,Monday")
    kotlin.test.assertEquals("Monday,Tuesday", options().getStringValue(option))
  }

  @Test
  fun `test remove list with invalid value`() {
    enterCommand("set $optionName-=Monday,Blue")
    kotlin.test.assertEquals("Monday,Tuesday", options().getStringValue(option))
  }
}
