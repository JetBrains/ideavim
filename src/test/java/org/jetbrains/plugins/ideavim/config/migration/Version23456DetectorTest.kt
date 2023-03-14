/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.config.migration

import com.maddyhome.idea.vim.config.migration.`Detect versions 3, 4, 5, 6`
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Version23456DetectorTest {
  @Test
  fun `detect versions`() {
    assertEquals(6, getVersionFrom(testConfig))
    assertNull(getVersionFrom(localConfig))
    assertEquals(6, getVersionFrom(sharedConfig))
  }

  private fun getVersionFrom(file: String): Int? {
    file.split("\n").forEach {
      val version = `Detect versions 3, 4, 5, 6`.getVersionFromLine(it)
      if (version != null) {
        return version
      }
    }
    return null
  }
}
