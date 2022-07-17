/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionOuterBlockBraceActionTest : VimTestCase() {
  fun testOutside() {
    typeTextInFile(
      injector.parser.parseKeys("di}"),
      "${c}foo{bar}\n"
    )
    assertState("foo{}\n")
  }

  fun testOutsideInString() {
    typeTextInFile(
      injector.parser.parseKeys("di}"),
      "\"1${c}23\"foo{bar}\n"
    )
    assertState("\"123\"foo{}\n")
  }

  fun testOutsideInString2() {
    typeTextInFile(
      injector.parser.parseKeys("di}"),
      "\"1${c}23{dsa}d\"foo{bar}\n"
    )
    assertState("\"123{}d\"foo{bar}\n")
  }
}
