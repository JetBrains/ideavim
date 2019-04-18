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
import javax.swing.SwingConstants

/**
 * @author Alex Plate
 */
class ExitHandlerTest : VimFileEditorTestCase() {
    fun `test single file`() {
        val psiFile = myFixture.configureByText("A_Discovery", "I found it in a legendary land")
        fileManager.openFile(psiFile.virtualFile, false)
        TestCase.assertEquals(1, fileManager.windows.size)

        typeText(commandToKeys("qa"))
        TestCase.assertEquals(1, fileManager.windows.size)
        TestCase.assertTrue(fileManager.windows[0].files.isEmpty())
    }

    fun `test full command`() {
        val psiFile = myFixture.configureByText("A_Discovery", "I found it in a legendary land")
        fileManager.openFile(psiFile.virtualFile, false)
        TestCase.assertEquals(1, fileManager.windows.size)

        typeText(commandToKeys("qall"))
        TestCase.assertEquals(1, fileManager.windows.size)
        TestCase.assertTrue(fileManager.windows[0].files.isEmpty())
    }

    fun `test multiple files`() {
        val psiFile1 = myFixture.configureByText("A_Discovery1", "I found it in a legendary land")
        val psiFile2 = myFixture.configureByText("A_Discovery2", "all rocks and lavender and tufted grass,")
        fileManager.openFile(psiFile1.virtualFile, false)
        fileManager.openFile(psiFile2.virtualFile, false)
        TestCase.assertEquals(1, fileManager.windows.size)
        TestCase.assertEquals(2, fileManager.windows[0].files.size)

        typeText(commandToKeys("qa"))
        TestCase.assertEquals(1, fileManager.windows.size)
        TestCase.assertTrue(fileManager.windows[0].files.isEmpty())
    }

    fun `test split`() {
        val psiFile = myFixture.configureByText("A_Discovery", "I found it in a legendary land")
        fileManager.openFile(psiFile.virtualFile, false)
        val primaryWindow = fileManager.currentWindow
        primaryWindow.split(SwingConstants.VERTICAL, true, psiFile.virtualFile, true)
        TestCase.assertEquals(2, fileManager.windows.size)

        typeText(commandToKeys("qa"))
        TestCase.assertEquals(1, fileManager.windows.size)
        TestCase.assertTrue(fileManager.windows[0].files.isEmpty())
    }
}