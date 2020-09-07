/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase

class YankLinesHandlerTest : VimTestCase() {
  fun `test copy with range`() {
    configureByText(
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                h""".trimIndent()
    )
    typeText(commandToKeys("3,4y"))
    val yanked = VimPlugin.getRegister().lastRegister!!.text
    assertEquals(
      """|I found it in a legendary land
         |all rocks and lavender and tufted grass,
         |
         """.trimMargin(), yanked)
  }

  fun `test copy with one char on the last line`() {
    configureByText(
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                h""".trimIndent()
    )
    typeText(commandToKeys("%y"))
    val yanked = VimPlugin.getRegister().lastRegister!!.text
    assertEquals(
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                h""".trimIndent(), yanked)
  }
}
