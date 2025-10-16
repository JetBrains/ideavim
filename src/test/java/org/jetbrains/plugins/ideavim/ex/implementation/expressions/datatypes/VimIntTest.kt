/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VimIntTest : VimTestCase() {
  @Test
  fun `test returns self when converting to Number`() {
    assertEquals(VimInt(123), VimInt(123).toVimNumber())
  }

  @Test
  fun `test converts to String`() {
    assertEquals("123", VimInt(123).toVimString().value)
  }

  @Test
  fun `test throws when trying to convert to Float`() {
    val value = VimInt(123)
    val exception = assertThrows<IllegalStateException> {
      value.toVimFloat()
    }
    assertEquals("Using a Number as a Float is not allowed", exception.message)
  }

  @Test
  fun `test output string`() {
    assertEquals("123", VimInt(123).toOutputString())
  }

  @Test
  fun `test has value semantics`() {
    assertEquals(VimInt(123), VimInt(123))
  }

  @Test
  fun `test compareTo`() {
    assertTrue(VimInt(123) > 100)
    assertTrue(VimInt(123) >= 123)
    assertTrue(VimInt(100) < 123)
    assertTrue(VimInt(123) <= 123)
  }
}
