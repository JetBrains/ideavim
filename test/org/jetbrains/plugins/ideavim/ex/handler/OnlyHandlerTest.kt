package org.jetbrains.plugins.ideavim.ex.handler

import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimFileEditorTestCase
import javax.swing.SwingConstants

/**
 * @author Alex Plate
 */

class OnlyHandlerTest : VimFileEditorTestCase() {
    fun `test double window close`() {
        val psiFile = myFixture.configureByText("A_Discovery", "I found it in a legendary land")
        fileManager.openFile(psiFile.virtualFile, false)
        val primaryWindow = fileManager.currentWindow
        primaryWindow.split(SwingConstants.VERTICAL, true, psiFile.virtualFile, true)
        TestCase.assertEquals(2, fileManager.windows.size)

        typeText(commandToKeys("only"))
        TestCase.assertEquals(1, fileManager.windows.size)
        TestCase.assertTrue(fileManager.windows[0].files.isNotEmpty())
    }

    fun `test short command`() {
        val psiFile = myFixture.configureByText("A_Discovery", "I found it in a legendary land")
        fileManager.openFile(psiFile.virtualFile, false)
        val primaryWindow = fileManager.currentWindow
        primaryWindow.split(SwingConstants.VERTICAL, true, psiFile.virtualFile, true)
        TestCase.assertEquals(2, fileManager.windows.size)

        typeText(commandToKeys("on"))
        TestCase.assertEquals(1, fileManager.windows.size)
        TestCase.assertTrue(fileManager.windows[0].files.isNotEmpty())
    }

    fun `test multiple window close`() {
        val psiFile = myFixture.configureByText("A_Discovery", "I found it in a legendary land")
        fileManager.openFile(psiFile.virtualFile, false)
        val primaryWindow = fileManager.currentWindow
        primaryWindow.split(SwingConstants.VERTICAL, true, psiFile.virtualFile, true)
        primaryWindow.split(SwingConstants.VERTICAL, true, psiFile.virtualFile, true)
        primaryWindow.split(SwingConstants.VERTICAL, true, psiFile.virtualFile, true)
        TestCase.assertEquals(4, fileManager.windows.size)

        typeText(commandToKeys("only"))
        TestCase.assertEquals(1, fileManager.windows.size)
        TestCase.assertTrue(fileManager.windows[0].files.isNotEmpty())
    }
}