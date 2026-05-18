/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.KeyValuePairOption
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

@Suppress("SpellCheckingInspection")
@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class KeyValuePairOptionTest : VimTestCase("\n") {
  // IdeaVim doesn't support this option, but it's good for testing. You can follow along with Vim
  // Note that setting "margin" requires "border" to also be set
  val option = KeyValuePairOption(
    "pumopt",
    OptionDeclaredScope.GLOBAL,
    "pumopt",
    "",
    listOf("border", "height", "width", "maxwidth", "opacity"),
    listOf("margin", "shadow")
  )

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    injector.optionGroup.addOption(option)
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    injector.optionGroup.removeOption(option.name)
    super.tearDown(testInfo)
  }

  private fun getOptionValue() = injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(null)).value

  @Test
  fun `test set value`() {
    enterCommand("set pumopt=height:10")
    assertEquals("height:10", getOptionValue())
  }

  @Test
  fun `test set multiple values`() {
    enterCommand("set pumopt=height:10,width:20")
    assertEquals("height:10,width:20", getOptionValue())
  }

  @Test
  fun `test set flag`() {
    enterCommand("set pumopt=shadow")
    assertEquals("shadow", getOptionValue())
  }

  @Test
  fun `test append value`() {
    enterCommand("set pumopt=height:10")
    enterCommand("set pumopt+=width:20")
    assertEquals("height:10,width:20", getOptionValue())
  }

  @Test
  fun `test append multiple values`() {
    enterCommand("set pumopt=height:10")
    enterCommand("set pumopt+=width:20,border:single")
    assertEquals("height:10,width:20,border:single", getOptionValue())
  }

  @Test
  fun `test append flag`() {
    enterCommand("set pumopt=margin,border:single")
    enterCommand("set pumopt+=shadow")
    assertEquals("margin,border:single,shadow", getOptionValue())
  }

  @Test
  fun `test append multiple flags`() {
    enterCommand("set pumopt=margin,border:single")
    enterCommand("set pumopt+=shadow,margin")
    assertEquals("margin,border:single,shadow", getOptionValue())
  }

  @Test
  fun `test append mix`() {
    enterCommand("set pumopt=height:10")
    enterCommand("set pumopt+=shadow,width:20")
    assertEquals("height:10,shadow,width:20", getOptionValue())
  }

  @Test
  fun `test prepend value`() {
    enterCommand("set pumopt=height:10")
    enterCommand("set pumopt^=width:20")
    assertEquals("width:20,height:10", getOptionValue())
  }

  @Test
  fun `test prepend multiple values`() {
    enterCommand("set pumopt=height:10")
    enterCommand("set pumopt^=width:20,border:single")
    assertEquals("border:single,width:20,height:10", getOptionValue())
  }

  @Test
  fun `test prepend flag`() {
    enterCommand("set pumopt=margin,border:single")
    enterCommand("set pumopt^=shadow")
    assertEquals("shadow,margin,border:single", getOptionValue())
  }

  @Test
  fun `test prepend multiple flags`() {
    enterCommand("set pumopt=margin,border:single")
    enterCommand("set pumopt^=shadow,margin")
    assertEquals("shadow,margin,border:single", getOptionValue())
  }

  @Test
  fun `test prepend mix`() {
    enterCommand("set pumopt=height:10")
    enterCommand("set pumopt^=shadow,width:20")
    assertEquals("width:20,shadow,height:10", getOptionValue())
  }

  @Test
  fun `test remove value`() {
    enterCommand("set pumopt=height:10,width:20")
    enterCommand("set pumopt-=width:20")
    assertEquals("height:10", getOptionValue())
  }

  @Test
  fun `test remove pair by key only`() {
    enterCommand("set pumopt=height:10,width:20")
    enterCommand("set pumopt-=width:10")
    assertEquals("height:10", getOptionValue())
  }

  @Test
  fun `test remove multiple values`() {
    enterCommand("set pumopt=height:10,width:20,border:single")
    enterCommand("set pumopt-=width:20,border:single")
    assertEquals("height:10", getOptionValue())
  }

  @Test
  fun `test remove flag`() {
    enterCommand("set pumopt=margin,shadow,border:single")
    enterCommand("set pumopt-=shadow")
    assertEquals("margin,border:single", getOptionValue())
  }

  @Test
  fun `test remove multiple flags`() {
    enterCommand("set pumopt=margin,border:single,shadow")
    enterCommand("set pumopt-=margin,shadow")
    assertEquals("border:single", getOptionValue())
  }

  @Test
  fun `test remove mix`() {
    enterCommand("set pumopt=height:10,shadow,width:20")
    enterCommand("set pumopt-=shadow,width:20")
    assertEquals("height:10", getOptionValue())
  }

  @Test
  fun `test append existing key replaces and moves to end`() {
    enterCommand("set pumopt=height:10,width:20")
    enterCommand("set pumopt+=height:15")
    assertEquals("width:20,height:15", getOptionValue())
  }

  @Test
  fun `test prepend existing key replaces and moves to start`() {
    enterCommand("set pumopt=height:10,width:20")
    enterCommand("set pumopt^=width:25")
    assertEquals("width:25,height:10", getOptionValue())
  }

  @Test
  fun `test append existing flag does nothing`() {
    enterCommand("set pumopt=border:single,shadow,opacity:10")
    enterCommand("set pumopt+=shadow")
    assertEquals("border:single,shadow,opacity:10", getOptionValue())
  }

  @Test
  fun `test prepend existing flag does nothing`() {
    enterCommand("set pumopt=border:single,shadow,opacity:10")
    enterCommand("set pumopt^=shadow")
    assertEquals("border:single,shadow,opacity:10", getOptionValue())
  }

  @Test
  fun `test error when passing value with unknown key`() {
    enterCommand("set pumopt=invalid:10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: pumopt=invalid:10")
  }

  @Test
  fun `test error when passing value to flag`() {
    enterCommand("set pumopt=margin:10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: pumopt=margin:10")
  }

  @Test
  fun `test error when passing unknown flag`() {
    enterCommand("set pumopt=invalid")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: pumopt=invalid")
  }

  @Test
  fun `test error when using key as flag`() {
    enterCommand("set pumopt=border")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: pumopt=border")
  }
}
