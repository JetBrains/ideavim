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

package org.jetbrains.plugins.ideavim.longrunning

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

class MapCommandTest : VimTestCase() {

  fun `test double recursion`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map b wbb"))
    typeText(StringHelper.parseKeys("b"))

    TestCase.assertTrue(VimPlugin.isError())
  }
}
