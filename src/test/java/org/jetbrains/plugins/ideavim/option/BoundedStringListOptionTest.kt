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

class BoundedStringListOptionTest : VimTestCase() {
  private val optionName = "myOpt"
  private val defaultValue = "Monday,Tuesday"

  override fun setUp() {
    super.setUp()
    val option = StringOption(optionName, optionName, defaultValue, true, setOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"))
    injector.optionGroup.addOption(option)
    configureByText("\n")
  }

  override fun tearDown() {
    super.tearDown()
    injector.optionGroup.removeOption(optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test set valid list`() {
    enterCommand("set $optionName=Thursday,Friday")
    assertEquals("Thursday,Friday", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test set list with invalid value`() {
    enterCommand("set $optionName=Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    assertEquals(defaultValue, options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append single item`() {
    enterCommand("set $optionName+=Wednesday")
    assertEquals("Monday,Tuesday,Wednesday", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append invalid item`() {
    enterCommand("set $optionName+=Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    assertEquals("Monday,Tuesday", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append list`() {
    enterCommand("set $optionName+=Wednesday,Thursday")
    assertEquals("Monday,Tuesday,Wednesday,Thursday", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append list with invalid item`() {
    enterCommand("set $optionName+=Wednesday,Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    assertEquals("Monday,Tuesday", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend item`() {
    enterCommand("set $optionName^=Wednesday")
    assertEquals("Wednesday,Monday,Tuesday", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend invalid item`() {
    enterCommand("set $optionName^=Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    assertEquals("Monday,Tuesday", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend list`() {
    enterCommand("set $optionName^=Wednesday,Thursday")
    assertEquals("Wednesday,Thursday,Monday,Tuesday", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend list with invalid item`() {
    enterCommand("set $optionName^=Wednesday,Blue")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: $optionName")
    assertEquals("Monday,Tuesday", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test remove item`() {
    enterCommand("set $optionName-=Monday")
    assertEquals("Tuesday", options().getStringValue(optionName))
  }

  fun `test remove list`() {
    enterCommand("set $optionName-=Monday,Tuesday")
    assertEquals("", options().getStringValue(optionName))
  }

  fun `test remove list with wrong order`() {
    enterCommand("set $optionName-=Tuesday,Monday")
    assertEquals("Monday,Tuesday", options().getStringValue(optionName))
  }

  fun `test remove list with invalid value`() {
    enterCommand("set $optionName-=Monday,Blue")
    assertEquals("Monday,Tuesday", options().getStringValue(optionName))
  }
}
