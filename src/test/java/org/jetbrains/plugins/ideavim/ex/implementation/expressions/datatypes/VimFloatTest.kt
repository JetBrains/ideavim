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
import kotlin.test.assertEquals

class VimFloatTest {

  @Test
  fun `round 6 digits`() {
    assertEquals("0.999999", VimFloat(0.999999).toString())
  }

  @Test
  fun `round 7 digits`() {
    assertEquals("1.0", VimFloat(0.9999999).toString())
  }
}
