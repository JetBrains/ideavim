/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.handler

import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimFileEditorTestCase

/**
 * @author Daniele Megna
 */

class TabOnlyHandlerTest : VimFileEditorTestCase() {
  fun `test close not selected tabs`() {
    val firstTabFile = myFixture.configureByText("A_Discovery", "I found it in a legendary land")
    val secondTabFile = myFixture.configureByText("A_Legend", "I found it in a new land")
    val thirdTabFile = myFixture.configureByText("A_Detection", "I found it in a that land")
    fileManager.openFile(firstTabFile.virtualFile, false)
    fileManager.openFile(secondTabFile.virtualFile, true)
    fileManager.openFile(thirdTabFile.virtualFile, false)
    fileManager.windows[0].tabbedPane!!.setSelectedIndex(1, true)

    TestCase.assertEquals(1, fileManager.windows.size)
    TestCase.assertEquals(3, fileManager.windows[0].files.size)
    TestCase.assertEquals(3, fileManager.windows[0].tabCount)
    TestCase.assertEquals(secondTabFile.virtualFile, fileManager.windows[0].selectedFile)

    typeText(commandToKeys("tabonly"))

    TestCase.assertEquals(1, fileManager.windows.size)
    TestCase.assertEquals(1, fileManager.windows[0].files.size)
    TestCase.assertEquals(1, fileManager.windows[0].tabCount)
    TestCase.assertEquals(secondTabFile.virtualFile, fileManager.windows[0].selectedFile)
  }
}