/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class WrapOptionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test 'wrap' defaults to true`() {
    assertTrue(fixture.editor.settings.isUseSoftWraps)
  }

  @Test
  fun `test 'wrap' can be turned off`() {
    enterCommand("set nowrap")
    assertFalse(fixture.editor.settings.isUseSoftWraps)
  }

  @Test
  fun `test 'wrap' can be turned back on`() {
    enterCommand("set nowrap")
    assertFalse(fixture.editor.settings.isUseSoftWraps)
    enterCommand("set wrap")
    assertTrue(fixture.editor.settings.isUseSoftWraps)
  }

  @Test
  fun `test 'wrap' reflects IDE value`() {
    fixture.editor.settings.isUseSoftWraps = false
    assertCommandOutput("set wrap?", "nowrap\n")
  }

  @Test
  fun `test 'wrap' is disabled by setlocal`() {
    enterCommand("setlocal nowrap")
    assertFalse(fixture.editor.settings.isUseSoftWraps)
  }

  @Test
  fun `test 'wrap' is not disabled by setglobal`() {
    enterCommand("setglobal nowrap")
    assertTrue(fixture.editor.settings.isUseSoftWraps)
  }
}