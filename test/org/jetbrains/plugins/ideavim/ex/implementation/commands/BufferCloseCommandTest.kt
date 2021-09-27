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

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Michal Placek
 */
class BufferCloseCommandTest : VimTestCase() {
  fun `test close file by bd command`() {

    val psiFile1 = myFixture.configureByText("A_Discovery1", "I found it in a legendary land")
    val psiFile2 = myFixture.configureByText("A_Discovery2", "all rocks and lavender and tufted grass,")

    fileManager.openFile(psiFile1.virtualFile, false)
    fileManager.openFile(psiFile2.virtualFile, true)
    assertPluginError(false)

    typeText(commandToKeys("bd"))

    assertPluginError(false)
  }
}
