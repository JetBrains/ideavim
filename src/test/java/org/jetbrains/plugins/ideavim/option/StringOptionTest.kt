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
class StringOptionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(super.testInfo)
  }

  // We use `'shell'` because it's one of the few string options IdeaVim supports that can be an arbitrary string value,
  // and it makes it easy to verify behaviour with Vim
  private fun getOptionValue() = injector.options(fixture.editor.vim).shell

  @Test
  fun `test set value`() {
    enterCommand("set shell=foo")
    assertEquals("foo", getOptionValue())
  }

  @Test
  fun `test set repeated value`() {
    enterCommand("set shell=foo,foo,foo")
    assertEquals("foo,foo,foo", getOptionValue())
  }

  @Test
  fun `test append value`() {
    enterCommand("set shell=foo")
    enterCommand("set shell+=bar")
    assertEquals("foobar", getOptionValue())
  }

  @Test
  fun `test append existing value will append value`() {
    enterCommand("set shell=foo")
    enterCommand("set shell+=foo")

    assertEquals("foofoo", getOptionValue())
  }

  @Test
  fun `test prepend value`() {
    enterCommand("set shell=foo")
    enterCommand("set shell^=bar")
    assertEquals("barfoo", getOptionValue())
  }

  @Test
  fun `test prepend existing value will prepend value`() {
    enterCommand("set shell=foo")
    enterCommand("set shell^=foo")
    assertEquals("foofoo", getOptionValue())
  }

  @Test
  fun `test remove value`() {
    enterCommand("set shell=foobar")
    enterCommand("set shell-=foo")
    assertEquals("bar", getOptionValue())
  }

  @Test
  fun `test remove value at end of string`() {
    enterCommand("set shell=foobar")
    enterCommand("set shell-=bar")
    assertEquals("foo", getOptionValue())
  }

  @Test
  fun `test removes first matching substring`() {
    enterCommand("set shell=foobarfoo")
    enterCommand("set shell-=foo")
    assertEquals("barfoo", getOptionValue())
  }
}
