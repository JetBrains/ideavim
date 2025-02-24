/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals

class VimFloatTest {

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
}
