/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class StringListOptionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(super.testInfo)
  }

  private fun getOptionValue() = injector.options(fixture.editor.vim).virtualedit.value

  @Test
  fun `test set value`() {
    enterCommand("set virtualedit=all")
    assertEquals("all", getOptionValue())
  }

  @Test
  fun `test set repeated value`() {
    enterCommand("set virtualedit=all,all,all")
    assertEquals("all,all,all", getOptionValue())
  }

  @Test
  fun `test append value`() {
    enterCommand("set virtualedit=all")
    enterCommand("set virtualedit+=onemore")
    assertEquals("all,onemore", getOptionValue())
  }

  @Test
  fun `test append existing value does not modify`() {
    enterCommand("set virtualedit=all")
    enterCommand("set virtualedit+=all")

    assertEquals("all", getOptionValue())
  }

  @Test
  fun `test append matching sublist does not modify`() {
    enterCommand("set virtualedit=all,onemore,block")
    enterCommand("set virtualedit+=all,onemore")

    assertEquals("all,onemore,block", getOptionValue())
  }

  @Test
  fun `test append matching sublist with different order will modify even if causes repeated items`() {
    enterCommand("set virtualedit=all,onemore,block")
    enterCommand("set virtualedit+=onemore,all")

    assertEquals("all,onemore,block,onemore,all", getOptionValue())
  }

  @Test
  fun `test prepend value`() {
    enterCommand("set virtualedit=all")
    enterCommand("set virtualedit^=onemore")
    assertEquals("onemore,all", getOptionValue())
  }

  @Test
  fun `test prepend matching sublist does not modify`() {
    enterCommand("set virtualedit=all,onemore,block")
    enterCommand("set virtualedit^=onemore,block")

    assertEquals("all,onemore,block", getOptionValue())
  }

  @Test
  fun `test prepend matching sublist with different order will modify even if causes repeated items`() {
    enterCommand("set virtualedit=all,onemore,block")
    enterCommand("set virtualedit^=onemore,all")

    assertEquals("onemore,all,all,onemore,block", getOptionValue())
  }

  @Test
  fun `test remove value`() {
    enterCommand("set virtualedit=all,onemore")
    enterCommand("set virtualedit-=onemore")
    assertEquals("all", getOptionValue())
  }

  @Test
  fun `test remove matching sublist`() {
    enterCommand("set virtualedit=all,onemore,block")
    enterCommand("set virtualedit-=onemore,block")
    assertEquals("all", getOptionValue())
  }

  @Test
  fun `test remove matching sublist with different order does not modify`() {
    enterCommand("set virtualedit=all,onemore,block")
    enterCommand("set virtualedit-=block,onemore")
    assertEquals("all,onemore,block", getOptionValue())
  }
}
