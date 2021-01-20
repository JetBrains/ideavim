/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.config.migration

import com.maddyhome.idea.vim.config.migration.`Detect versions 3, 4, 5, 6`
import org.junit.Test
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