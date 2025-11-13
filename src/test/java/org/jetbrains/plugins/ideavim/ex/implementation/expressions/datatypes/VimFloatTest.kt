/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

class VimFloatTest : VimTestCase() {

  @Test
  fun `round 6 digits`() {
    assertEquals("0.999999", VimFloat(0.999999).toOutputString())
  }

  @Test
  fun `round 7 digits`() {
    assertEquals("1.0", VimFloat(0.9999999).toOutputString())
  }

  @Test
  fun `use point as decimal separator always`() {
    val oldLocale = Locale.getDefault()
    Locale.setDefault(Locale.GERMANY) // In Germany, they use a comma as a decimal separator, i.e., "3,14".
    try {
      assertEquals("3.14", VimFloat(3.14).toOutputString())
    } finally {
      Locale.setDefault(oldLocale)
    }
  }

  @Test
  fun `test returns self when converting to Float`() {
    assertEquals(VimFloat(1.23), VimFloat(1.23).toVimFloat())
  }

  @Test
  fun `test throws when trying to convert to Number`() {
    val value = VimFloat(1.23)
    val exception = assertThrows<ExException> {
      value.toVimNumber()
    }
    assertEquals("E805: Using a Float as a Number", exception.message)
  }

  @Test
  fun `test throws when trying to convert to String`() {
    val value = VimFloat(1.23)
    val exception = assertThrows<ExException> {
      value.toVimString()
    }
    assertEquals("E806: Using a Float as a String", exception.message)
  }

  @Test
  fun `test has value semantics`() {
    assertEquals(VimFloat(1.23), VimFloat(1.23))
    assertNotEquals(VimFloat(1.23), VimFloat(53.22))
  }

  @Test
  fun `test copy returns new instance with same value`() {
    val value = VimFloat(1.23)
    val copy = value.copy()
    assertNotSame(value, copy)
    assertEquals(value.value, copy.value)
  }

  @Test
  fun `test deepCopy returns new instance with same value`() {
    val value = VimFloat(1.23)
    val copy = value.deepCopy(useReferences = true) as VimFloat
    assertNotSame(value, copy)
    assertEquals(value.value, copy.value)
  }
}
