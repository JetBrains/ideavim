package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class YankVisualLinesActionTest : VimTestCase() {
  fun `test from visual mode`() {
    val text = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val yankedTest = """
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            
    """.trimIndent()
    configureByText(text)
    typeText(parseKeys("vjY"))
    val savedText = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()
    TestCase.assertEquals(yankedTest, savedText)
  }

  fun `test from visual mode till the end`() {
    val text = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was sett${c}led on some sodden sand
            hard by the torrent of a mountain pass.""".trimIndent()
    val yankedTest = """
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.""".trimIndent()
    configureByText(text)
    typeText(parseKeys("vjY"))
    val savedText = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()
    TestCase.assertEquals(yankedTest, savedText)
  }

  fun `test from line visual mode`() {
    val text = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val yankedTest = """
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            
    """.trimIndent()
    configureByText(text)
    typeText(parseKeys("VjY"))
    val savedText = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()
    TestCase.assertEquals(yankedTest, savedText)
  }
}
