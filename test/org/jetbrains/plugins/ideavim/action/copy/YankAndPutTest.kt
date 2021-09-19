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

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

class YankAndPutTest : VimOptionTestCase(ClipboardOptionsData.name) {
  @VimOptionTestConfiguration(
    VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, [ClipboardOptionsData.unnamed])
  )
  fun `test yank to number register with unnamed`() {
    val before = """
            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
    """.trimIndent()
    configureByText(before)
    // Select and yank first word
    typeText(parseKeys("vey"))
    // Replace second word
    typeText(parseKeys("wvep"))
    // Replace previous word
    typeText(parseKeys("bbvep"))

    assertState(
      """
            I it found in a legendary land
            all rocks and lavender and tufted grass,
      """.trimIndent()
    )
  }

  @VimOptionTestConfiguration(
    VimTestOption(
      ClipboardOptionsData.name,
      VimTestOptionType.LIST,
      [ClipboardOptionsData.unnamed, ClipboardOptionsData.ideaput]
    )
  )
  fun `test yank to number register with unnamed and ideaput`() {
    val before = """
            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
    """.trimIndent()
    configureByText(before)
    // Select and yank first word
    typeText(parseKeys("vey"))
    // Replace second word
    typeText(parseKeys("wvep"))
    // Replace previous word
    typeText(parseKeys("bbvep"))

    assertState(
      """
            I it found in a legendary land
            all rocks and lavender and tufted grass,
      """.trimIndent()
    )
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, []))
  fun `test yank to number register`() {
    val before = """
            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
    """.trimIndent()
    configureByText(before)
    // Select and yank first word
    typeText(parseKeys("vey"))
    // Replace second word
    typeText(parseKeys("wvep"))
    // Replace previous word
    typeText(parseKeys("bbvep"))

    assertState(
      """
            I it found in a legendary land
            all rocks and lavender and tufted grass,
      """.trimIndent()
    )
  }
}
