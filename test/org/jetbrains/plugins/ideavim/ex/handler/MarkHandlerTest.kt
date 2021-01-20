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

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.VimPlugin
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class MarkHandlerTest : VimTestCase() {
  fun `test simple mark`() {
    configureByText("""I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it$c was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
    typeText(commandToKeys("mark a"))
    VimPlugin.getMark().getMark(myFixture.editor, 'a')?.let {
      assertEquals(2, it.logicalLine)
      assertEquals(0, it.col)
    } ?: TestCase.fail("Mark is null")
  }

  fun `test global mark`() {
    configureByText("""I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it$c was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
    typeText(commandToKeys("mark G"))
    VimPlugin.getMark().getMark(myFixture.editor, 'G')?.let {
      assertEquals(2, it.logicalLine)
      assertEquals(0, it.col)
    } ?: TestCase.fail("Mark is null")
  }

  fun `test k mark`() {
    configureByText("""I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it$c was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
    typeText(commandToKeys("k a"))
    VimPlugin.getMark().getMark(myFixture.editor, 'a')?.let {
      assertEquals(2, it.logicalLine)
      assertEquals(0, it.col)
    } ?: TestCase.fail("Mark is null")
  }

  fun `test mark in range`() {
    configureByText("""I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it$c was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
    typeText(commandToKeys("1,2 mark a"))
    VimPlugin.getMark().getMark(myFixture.editor, 'a')?.let {
      assertEquals(1, it.logicalLine)
      assertEquals(0, it.col)
    } ?: TestCase.fail("Mark is null")
  }
}
