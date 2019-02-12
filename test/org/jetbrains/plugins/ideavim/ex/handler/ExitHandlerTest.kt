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