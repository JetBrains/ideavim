/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the `:let` command assigning to an option lvalue
 *
 * This tests assigning to an option lvalue. It tests assigning different values to Number and String option types. It
 * also ensures compound assignment works, but does not test all compound assignment operators. They are tested
 * exhaustively in [LetCommandOperatorsTest].
 */
class LetCommandOptionLValueTest : VimTestCase("\n") {
  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    injector.optionGroup.removeOption(OPTION_NAME)
    super.tearDown(testInfo)
  }

  @Test
  fun `test let option updates toggle option with Number value`() {
    enterCommand("let &incsearch=1")
    assertTrue(options().incsearch)
    enterCommand("let &incsearch = 0")
    assertFalse(options().incsearch)
  }

  @Test
  fun `test let assigning Float to Number option reports error`() {
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, 42)
    injector.optionGroup.addOption(option)
    enterCommand("let &test=1.23")
    assertPluginError(true)
    assertPluginErrorMessage("E805: Using a Float as a Number")
  }

  @Test
  fun `test let assigning List to Number option reports error`() {
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, 42)
    injector.optionGroup.addOption(option)
    enterCommand("let &test=[1,2,3]")
    assertPluginError(true)
    assertPluginErrorMessage("E745: Using a List as a Number")
  }

  @Test
  fun `test let assigning Dictionary to Number option reports error`() {
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, 42)
    injector.optionGroup.addOption(option)
    enterCommand("let &test={'key1' : 1, 'key2' : 2}")
    assertPluginError(true)
    assertPluginErrorMessage("E728: Using a Dictionary as a Number")
  }

  @Test
  fun `test let assigning String to Number option converts to Number`() {
    enterCommand("let &incsearch='1'")
    assertTrue(options().incsearch)
    enterCommand("let &incsearch='0'")
    assertFalse(options().incsearch)
    enterCommand("let &incsearch='21'")
    assertTrue(options().incsearch)
  }

  @VimBehaviorDiffers("E521: Number required: &incsearch='foo'")
  @Test
  fun `test let toggle option with invalid string value reports error`() {
    // Looks like Vim checks a String value that evaluates to 0. If it's not actually 0, throw an error
    enterCommand("let &incsearch='foo'")
    assertPluginError(true)
    assertPluginErrorMessage("E521: Number required after =: &incsearch='foo'")
  }

  @Test
  fun `test let assigning String option with String value`() {
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, "something")
    injector.optionGroup.addOption(option)
    enterCommand("let &test='whatever'")
    val value = injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)).value
    assertEquals("whatever", value)
  }

  @Test
  fun `test let assigning String option with Number value`() {
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, "something")
    injector.optionGroup.addOption(option)
    enterCommand("let &test=12")
    assertPluginError(false)
    val value = injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)).value
    assertEquals("12", value)
  }

  @Test
  fun `test let assigning String option with Float value`() {
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, "something")
    injector.optionGroup.addOption(option)
    enterCommand("let &test=1.23")
    val value = injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)).value
    assertEquals("1.23", value)
  }

  @Test
  fun `test let assigning String option with List value raises error`() {
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, "something")
    injector.optionGroup.addOption(option)
    enterCommand("let &test=[1,2,3]")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  @Test
  fun `test let assigning String option with Dictionary value raises error`() {
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, "something")
    injector.optionGroup.addOption(option)
    enterCommand("let &test={'key1' : 1, 'key2' : 2}")
    assertPluginError(true)
    assertPluginErrorMessage("E731: Using a Dictionary as a String")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test let option without scope behaves like set`() {
    // We don't have a local toggle option we can try this with. 'number' and 'relativenumber' are backed by the IDE
    val option = ToggleOption("test", OptionDeclaredScope.LOCAL_TO_WINDOW, "test", false)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("let &test = 12")
      val globalValue = injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim))
      val localValue = injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim))
      assertEquals(12, globalValue.value)
      assertEquals(12, localValue.value)
      assertTrue(
        injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)).booleanValue
      )
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test let option with local scope behaves like setlocal`() {
    // We don't have a local toggle option we can try this with. 'number' and 'relativenumber' are backed by the IDE
    val option = ToggleOption("test", OptionDeclaredScope.LOCAL_TO_WINDOW, "test", false)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("let &l:test = 12")
      val globalValue = injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim))
      val localValue = injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim))
      assertEquals(0, globalValue.value)
      assertEquals(12, localValue.value)
      assertTrue(
        injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)).booleanValue
      )
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test let option with global scope behaves like setglobal`() {
    // We don't have a local toggle option we can try this with. 'number' and 'relativenumber' are backed by the IDE
    val option = ToggleOption("test", OptionDeclaredScope.LOCAL_TO_WINDOW, "test", false)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("let &g:test = 12")
      val globalValue = injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim))
      val localValue = injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim))
      assertEquals(12, globalValue.value)
      assertEquals(0, localValue.value)
      assertFalse(
        injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)).booleanValue
      )
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test let option with compound operator and no scope`() {
    // 'scroll' is a local to window number option
    enterCommand("set scroll=42")
    enterCommand("let &scroll+=10")
    val globalValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.GLOBAL(fixture.editor.vim))
    val localValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.LOCAL(fixture.editor.vim))
    assertEquals(52, globalValue.value)
    assertEquals(52, localValue.value)
    assertEquals(52, options().scroll)
  }

  @Test
  fun `test let local option with compound operator`() {
    enterCommand("setlocal scroll=42")
    enterCommand("let &l:scroll+=10")
    val globalValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.GLOBAL(fixture.editor.vim))
    val localValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.LOCAL(fixture.editor.vim))
    assertEquals(0, globalValue.value)
    assertEquals(52, localValue.value)
    assertEquals(52, options().scroll)
  }

  @Test
  fun `test let global option with compound operator`() {
    enterCommand("setglobal scroll=42")
    enterCommand("let &g:scroll+=10")
    val globalValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.GLOBAL(fixture.editor.vim))
    val localValue = injector.optionGroup.getOptionValue(Options.scroll, OptionAccessScope.LOCAL(fixture.editor.vim))
    assertEquals(52, globalValue.value)
    assertEquals(0, localValue.value)
    assertEquals(0, options().scroll)
  }

  @Test
  fun `test let String option with string concatenation operator`() {
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, "something")
    injector.optionGroup.addOption(option)
    enterCommand("let &test.=' good'")
    val value = injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)).value
    assertEquals("something good", value)
  }

  @Test
  fun `test let String option with arithmetic compound assignment operator raises error for Number rvalue`() {
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, "something")
    injector.optionGroup.addOption(option)
    enterCommand("let &test='10'")
    enterCommand("let &test += 1")
    assertPluginError(true)
    assertPluginErrorMessage("E734: Wrong variable type for +=")
  }

  @Test
  fun `test let String option with arithmetic compound assignment operator raises error for String rvalue`() {
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, "something")
    injector.optionGroup.addOption(option)
    enterCommand("let &test='10'")
    enterCommand("let &test += '1'")
    assertPluginError(true)
    assertPluginErrorMessage("E734: Wrong variable type for +=")
  }

  @Test
  fun `test let Number option with arithmetic compound assignment operator`() {
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, 42)
    injector.optionGroup.addOption(option)
    enterCommand("let &test+=1")
    val value = injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)).value
    assertEquals(43, value)
  }

  @Test
  fun `test let Number option with string concatenation compound assignment operator raises error`() {
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, 42)
    injector.optionGroup.addOption(option)
    enterCommand("let &test.='1'")
    assertPluginError(true)
    assertPluginErrorMessage("E734: Wrong variable type for .=")
  }

  companion object {
    const val OPTION_NAME = "test"
  }
}
