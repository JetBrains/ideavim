package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

class YankMotionActionTest : VimTestCase() {
  fun `test yank till new line`() {
    val file = """
            A Discovery

            I found it in a legendary l${c}and
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    typeTextInFile(StringHelper.parseKeys("yW"), file)
    val text = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()

    TestCase.assertEquals("and", text)
  }

  fun `test yank caret doesn't move`() {
    val file = """
            A Discovery

            I found it in a legendary l${c}and
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    configureByText(file)

    val initialOffset = myFixture.editor.caretModel.offset
    typeText(StringHelper.parseKeys("yy"))

    TestCase.assertEquals(initialOffset, myFixture.editor.caretModel.offset)
  }
}
