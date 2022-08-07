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

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf
import org.junit.Test

class PutTestAfterCursorActionTest : VimTestCase() {
  fun `test put from number register`() {
    setRegister('4', "XXX ")
    doTest(
      "\"4p",
      "This is my$c text",
      "This is my XXX$c text",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """
  )
  @Test
  fun `test put visual text line to last line`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor.vim, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(injector.parser.parseKeys("p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            ${c}A Discovery

    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test inserting same content to multiple carets`() {
    val before = """
            A Discovery

            ${c}I found it in a legendary land
            ${c}all rocks and lavender and tufted grass,
            ${c}where it was settled on some sodden sand
            ${c}hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor.vim, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("vep"))
    val after = """
            A Discovery

            Discovery it in a legendary land
            Discovery rocks and lavender and tufted grass,
            Discovery it was settled on some sodden sand
            Discovery by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }
}
