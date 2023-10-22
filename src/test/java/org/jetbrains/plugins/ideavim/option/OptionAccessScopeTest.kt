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
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

private const val OPTION_NAME = "test"

@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class OptionAccessScopeTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    injector.optionGroup.removeOption(OPTION_NAME)
    super.tearDown(testInfo)
  }


  // GLOBAL
  @Test
  fun `test set global option at global scope affects all scopes`() {
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, 10)
    injector.optionGroup.addOption(option)

    val globalValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), globalValue)

    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global option at local scope affects all scopes`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val setValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), setValue)

    assertEquals(setValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(setValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(setValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global option at effective scope affects all scopes`() {
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL, OPTION_NAME, 10)
    injector.optionGroup.addOption(option)

    val effectiveValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), effectiveValue)

    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }


  // LOCAL_TO_BUFFER
  @Test
  fun `test set local-to-buffer option at global scope does not change local value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val globalValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), globalValue)

    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set local-to-buffer option at local scope does not change global value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val localValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), localValue)

    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set local-to-buffer option at effective scope changes both local and global values`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val effectiveValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), effectiveValue)

    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }


  // LOCAL_TO_WINDOW
  @Test
  fun `test set local-to-window option at global scope does not change local value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val globalValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), globalValue)

    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set local-to-window option at local scope does not change global value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val localValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), localValue)

    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set local-to-window option at effective scope changes both local and global values`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val effectiveValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), effectiveValue)

    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }


  // Global-local is tricky. The local value is initially not set, and the global value is used until it is. For string
  // options, this is represented by the local value showing as an empty string. Number options usually use the value -1
  // to indicate unset. This is not true for `'undolevels'`, which uses -1 to represent "no undo", and so uses the
  // sentinel value -123456 to represent unset. Since booleans are internally represented as number options, they also
  // use the -1 value to represent unset.
  // The `:setglobal` command always gets/sets the global value, without affecting the local value.
  // The `:setlocal` command will set the local value without affecting the global value. It will always get the current
  // value of the option, even if that is the sentinel value to indicate "unset". When showing the value of a boolean
  // option, if the local value is unset, it is prefixed by two dashes. E.g. `:setlocal autoread?` would show
  // `--autoread`.
  // The `:set` command will get the effective value - the local value if set, the global value if not. When setting the
  // value, if the local value has not been set, then `:set` will only set the global value. If the local value has
  // previously been set, then `:set` will reset string options to an empty string, but reset number (and boolean)
  // options to the global value. It does not "unset" the local value.
  // The `:set {option}<` or `:setlocal {option}<` syntax can be used to copy the local value to the global value. Note
  // that this is a copy, and so does not "unset" the local value either. (`:setglobal so<` does nothing useful). This
  // also applies to string options - it copies the global value to the local value. To unset a local string value, use
  // `:setlocal {option}=` to set it to an empty path.


  // GLOBAL_OR_LOCAL_TO_BUFFER
  // Int (unset value defaults to -1)
  @Test
  fun `test set global-local (buffer) number option at global scope does not change unset local value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val globalValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), globalValue)

    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (buffer) number option at local scope does not change global value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val localValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), localValue)

    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (buffer) number option at effective scope does not change unset local value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val effectiveValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), effectiveValue)

    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (buffer) number option at effective scope with existing local value updates global and local values`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val initialLocalValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), initialLocalValue)

    val newValue = VimInt(200)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), newValue)

    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  // Boolean (unset value defaults to -1)
  @Test
  fun `test set global-local (buffer) toggle option at global scope does not change unset local value`() {
    val defaultValue = VimInt.ONE
    val option = ToggleOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val globalValue = VimInt.ZERO
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), globalValue)

    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (buffer) toggle option at local scope does not change global value`() {
    val defaultValue = VimInt.ONE
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val localValue = VimInt.ZERO
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), localValue)

    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (buffer) toggle option at effective scope does not change unset local value`() {
    val defaultValue = VimInt.ONE
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val effectiveValue = VimInt.ZERO
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), effectiveValue)

    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (buffer) toggle option at effective scope with existing local value updates global and local values`() {
    val defaultValue = VimInt.ONE
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val initialLocalValue = VimInt.ZERO
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), initialLocalValue)

    val newValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), newValue)

    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  // String (unset value is empty string)
  @Test
  fun `test set global-local (buffer) string option at global scope does not change unset local value`() {
    val defaultValue = VimString("default")
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val globalValue = VimString("lorem ipsum")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), globalValue)

    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (buffer) string option at local scope does not change global value`() {
    val defaultValue = VimString("default")
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val localValue = VimString("lorem ipsum")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), localValue)

    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (buffer) string option at effective scope does not change unset local value`() {
    val defaultValue = VimString("default")
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val effectiveValue = VimString("lorem ipsum")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), effectiveValue)

    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  // Note that this behaviour is different to number based options! (including toggle options)
  @Test
  fun `test set global-local (buffer) string option at effective scope with existing local value updates global value and unsets local value`() {
    val defaultValue = VimString("default")
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val initialLocalValue = VimString("lorem ipsum")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), initialLocalValue)

    val newValue = VimString("dolor sit amet")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), newValue)

    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }


  // GLOBAL_OR_LOCAL_TO_WINDOW
  // Int (unset value defaults to -1)
  @Test
  fun `test set global-local (window) number option at global scope does not change unset local value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val globalValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), globalValue)

    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (window) number option at local scope does not change global value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val localValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), localValue)

    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (window) number option at effective scope does not change unset local value`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val effectiveValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), effectiveValue)

    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (window) number option at effective scope with existing local value updates global and local values`() {
    val defaultValue = VimInt(10)
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val initialLocalValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), initialLocalValue)

    val newValue = VimInt(200)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), newValue)

    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }


  // Boolean (unset value defaults to -1)
  @Test
  fun `test set global-local (window) toggle option at global scope does not change unset local value`() {
    val defaultValue = VimInt.ONE
    val option = ToggleOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val globalValue = VimInt.ZERO
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), globalValue)

    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (window) toggle option at local scope does not change global value`() {
    val defaultValue = VimInt.ONE
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val localValue = VimInt.ZERO
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), localValue)

    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (window) toggle option at effective scope does not change unset local value`() {
    val defaultValue = VimInt.ONE
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val effectiveValue = VimInt.ZERO
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), effectiveValue)

    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (window) toggle option at effective scope with existing local value updates global and local values`() {
    val defaultValue = VimInt.ONE
    val option = NumberOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val initialLocalValue = VimInt.ZERO
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), initialLocalValue)

    val newValue = VimInt(100)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), newValue)

    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  // String (unset value is empty string)
  @Test
  fun `test set global-local (window) string option at global scope does not change unset local value`() {
    val defaultValue = VimString("default")
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val globalValue = VimString("lorem ipsum")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), globalValue)

    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(globalValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (window) string option at local scope does not change global value`() {
    val defaultValue = VimString("default")
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val localValue = VimString("lorem ipsum")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), localValue)

    assertEquals(defaultValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(localValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  @Test
  fun `test set global-local (window) string option at effective scope does not change unset local value`() {
    val defaultValue = VimString("default")
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val effectiveValue = VimString("lorem ipsum")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), effectiveValue)

    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(effectiveValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }

  // Note that this behaviour is different to number based options! (including toggle options)
  @Test
  fun `test set global-local (window) string option at effective scope with existing local value updates global value and unsets local value`() {
    val defaultValue = VimString("default")
    val option = StringOption(OPTION_NAME, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, OPTION_NAME, defaultValue)
    injector.optionGroup.addOption(option)

    val initialLocalValue = VimString("lorem ipsum")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), initialLocalValue)

    val newValue = VimString("dolor sit amet")
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim), newValue)

    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim)))
    assertEquals(option.unsetValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim)))
    assertEquals(newValue, injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(fixture.editor.vim)))
  }
}