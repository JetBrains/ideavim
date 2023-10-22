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
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class GlobalOptionChangeListenerTest : VimTestCase() {
  private val optionName = "test"
  private val defaultValue = "defaultValue"

  private object Listener : GlobalOptionChangeListener {
    var called = false

    override fun onGlobalOptionChanged() {
      called = true
    }
  }

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
    Listener.called = false
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    injector.optionGroup.getOption(optionName)?.let {
      injector.optionGroup.removeGlobalOptionChangeListener(it, Listener)
      injector.optionGroup.removeOption(optionName)
    }
    super.tearDown(testInfo)
  }

  @Test
  fun `test listener called when global option changes`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL, optionName, defaultValue)
    injector.optionGroup.addOption(option)
    injector.optionGroup.addGlobalOptionChangeListener(option, Listener)

    // Global value of a global option, we can pass null
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(null), VimString("newValue"))

    assertTrue(Listener.called)
  }

  @Test
  fun `test listener not called when global option set to current value`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL, optionName, defaultValue)
    injector.optionGroup.addOption(option)
    injector.optionGroup.addGlobalOptionChangeListener(option, Listener)

    // Global value of a global option, we can pass null
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(null), VimString(defaultValue))

    assertFalse(Listener.called)
  }

  @Test
  fun `test listener called when effective value of global option changes`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL, optionName, defaultValue)
    injector.optionGroup.addOption(option)
    injector.optionGroup.addGlobalOptionChangeListener(option, Listener)

    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), VimString("newValue"))

    assertTrue(Listener.called)
  }

  @Test
  fun `test listener not called when effective value of global option set to current value`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL, optionName, defaultValue)
    injector.optionGroup.addOption(option)
    injector.optionGroup.addGlobalOptionChangeListener(option, Listener)

    // Global value of a global option, we can pass null
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), VimString(defaultValue))

    assertFalse(Listener.called)
  }

  @Test
  fun `test listener called when local value of global option changes`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL, optionName, defaultValue)
    injector.optionGroup.addOption(option)
    injector.optionGroup.addGlobalOptionChangeListener(option, Listener)

    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), VimString("newValue"))

    assertTrue(Listener.called)
  }

  @Test
  fun `test listener not called when local value of global option set to current value`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL, optionName, defaultValue)
    injector.optionGroup.addOption(option)
    injector.optionGroup.addGlobalOptionChangeListener(option, Listener)

    // Global value of a global option, we can pass null
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), VimString(defaultValue))

    assertFalse(Listener.called)
  }

  @Test
  fun `test cannot register listener for local option`() {
    val option = StringOption("myOption", OptionDeclaredScope.LOCAL_TO_BUFFER, optionName, defaultValue)
    try {
      injector.optionGroup.addOption(option)

      assertThrows<IllegalStateException> {
        injector.optionGroup.addGlobalOptionChangeListener(option, Listener)
      }
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test cannot register listener for global-local option`() {
    val option = StringOption("myOption", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, optionName, defaultValue)
    try {
      injector.optionGroup.addOption(option)

      assertThrows<IllegalStateException> {
        injector.optionGroup.addGlobalOptionChangeListener(option, Listener)
      }
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }
}