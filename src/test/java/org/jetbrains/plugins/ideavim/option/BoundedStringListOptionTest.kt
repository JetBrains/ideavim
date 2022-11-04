/*
 * Copyright 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class BoundedStringListOptionTest : VimTestCase() {
  private val optionName = "myOpt"
  private val defaultValue = "Monday,Tuesday"
  private val optionService = injector.optionService

  init {
    val option = StringOption(optionName, optionName, defaultValue, true, setOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"))
    optionService.addOption(option)
  }

  private fun assertEquals(val1: String, val2: VimDataType) {
    assertEquals(VimString(val1), val2)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test set valid list`() {
    optionService.setOptionValue(GLOBAL, optionName, "Thursday,Friday")
    assertEquals(VimString("Thursday,Friday"), optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test set list with invalid value`() {
    try {
      optionService.setOptionValue(GLOBAL, optionName, "Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals(defaultValue, injector.optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append single item`() {
    optionService.appendValue(GLOBAL, optionName, "Wednesday")
    assertEquals("Monday,Tuesday,Wednesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append invalid item`() {
    try {
      optionService.appendValue(GLOBAL, optionName, "Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals("Monday,Tuesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append list`() {
    optionService.appendValue(GLOBAL, optionName, "Wednesday,Thursday")
    assertEquals("Monday,Tuesday,Wednesday,Thursday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append list with invalid item`() {
    try {
      optionService.appendValue(GLOBAL, optionName, "Wednesday,Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals("Monday,Tuesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend item`() {
    optionService.prependValue(GLOBAL, optionName, "Wednesday")
    assertEquals("Wednesday,Monday,Tuesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend invalid item`() {
    try {
      optionService.prependValue(GLOBAL, optionName, "Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals("Monday,Tuesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend list`() {
    optionService.prependValue(GLOBAL, optionName, "Wednesday,Thursday")
    assertEquals("Wednesday,Thursday,Monday,Tuesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend list with invalid item`() {
    try {
      optionService.prependValue(GLOBAL, optionName, "Wednesday,Blue")
      fail("Missing exception")
    } catch (e: ExException) {
      assertEquals("E474: Invalid argument: $optionName", e.message)
    }
    assertEquals("Monday,Tuesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test remove item`() {
    optionService.removeValue(GLOBAL, optionName, "Monday")
    assertEquals("Tuesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  fun `test remove list`() {
    optionService.removeValue(GLOBAL, optionName, "Monday,Tuesday")
    assertEquals("", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  fun `test remove list with wrong order`() {
    optionService.removeValue(GLOBAL, optionName, "Tuesday,Monday")
    assertEquals("Monday,Tuesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }

  fun `test remove list with invalid value`() {
    optionService.removeValue(GLOBAL, optionName, "Monday,Blue")
    assertEquals("Monday,Tuesday", optionService.getOptionValue(GLOBAL, optionName))
    optionService.resetDefault(GLOBAL, optionName)
  }
}
