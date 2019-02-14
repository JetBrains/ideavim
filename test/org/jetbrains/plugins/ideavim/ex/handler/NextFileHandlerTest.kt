package org.jetbrains.plugins.ideavim.ex.handler

import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimFileEditorTestCase

/**
 * @author Alex Plate
 */
class NextFileHandlerTest : VimFileEditorTestCase() {
    fun `test simple move`() {
        myFixture.configureByText("A_Discovery1", "I found it in a legendary land")
        myFixture.configureByText("A_Discovery2", "all rocks and lavender and tufted grass,")
        myFixture.configureByText("A_Discovery3", "where it was settled on some sodden sand")
        TestCase.assertEquals(2, fileManager.windows[0].tabbedPane?.selectedIndex)
        typeText(commandToKeys("next"))
        TestCase.assertEquals(0, fileManager.windows[0].tabbedPane?.selectedIndex)
        typeText(commandToKeys("next"))
        TestCase.assertEquals(1, fileManager.windows[0].tabbedPane?.selectedIndex)
    }
}