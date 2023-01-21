/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.options.OptionScope.GLOBAL
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
  }

  override fun tearDown() {
    super.tearDown()
    injector.optionGroup.removeOption(optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test set valid list`() {
    injector.optionGroup.setOptionValue(GLOBAL, optionName, "Thursday,Friday")
    assertEquals("Thursday,Friday", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test set list with invalid value`() {
    try {
      injector.optionGroup.setOptionValue(GLOBAL, optionName, "Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals(defaultValue, optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append single item`() {
    injector.optionGroup.appendValue(GLOBAL, optionName, "Wednesday")
    assertEquals("Monday,Tuesday,Wednesday", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append invalid item`() {
    try {
      injector.optionGroup.appendValue(GLOBAL, optionName, "Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals("Monday,Tuesday", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append list`() {
    injector.optionGroup.appendValue(GLOBAL, optionName, "Wednesday,Thursday")
    assertEquals("Monday,Tuesday,Wednesday,Thursday", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append list with invalid item`() {
    try {
      injector.optionGroup.appendValue(GLOBAL, optionName, "Wednesday,Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals("Monday,Tuesday", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend item`() {
    injector.optionGroup.prependValue(GLOBAL, optionName, "Wednesday")
    assertEquals("Wednesday,Monday,Tuesday", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend invalid item`() {
    try {
      injector.optionGroup.prependValue(GLOBAL, optionName, "Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals("Monday,Tuesday", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend list`() {
    injector.optionGroup.prependValue(GLOBAL, optionName, "Wednesday,Thursday")
    assertEquals("Wednesday,Thursday,Monday,Tuesday", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend list with invalid item`() {
    try {
      injector.optionGroup.prependValue(GLOBAL, optionName, "Wednesday,Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals("Monday,Tuesday", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test remove item`() {
    injector.optionGroup.removeValue(GLOBAL, optionName, "Monday")
    assertEquals("Tuesday", optionsNoEditor().getStringValue(optionName))
  }

  fun `test remove list`() {
    injector.optionGroup.removeValue(GLOBAL, optionName, "Monday,Tuesday")
    assertEquals("", optionsNoEditor().getStringValue(optionName))
  }

  fun `test remove list with wrong order`() {
    injector.optionGroup.removeValue(GLOBAL, optionName, "Tuesday,Monday")
    assertEquals("Monday,Tuesday", optionsNoEditor().getStringValue(optionName))
  }

  fun `test remove list with invalid value`() {
    injector.optionGroup.removeValue(GLOBAL, optionName, "Monday,Blue")
    assertEquals("Monday,Tuesday", optionsNoEditor().getStringValue(optionName))
  }
}
