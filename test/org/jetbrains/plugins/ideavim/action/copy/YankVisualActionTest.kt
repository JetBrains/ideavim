@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviourDiffers
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase
import javax.swing.KeyStroke

class YankVisualActionTest : VimTestCase() {
    fun `test simple yank`() {
        doTest(parseKeys("viw", "y"),
                """
                            A Discovery

                            I ${c}found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
                            """.trimIndent(),
                "found", SelectionType.CHARACTER_WISE)
    }

    @VimBehaviourDiffers("\n")
    fun `test yank empty line`() {
        doTest(parseKeys("v", "y"),
                """
                            A Discovery
                            ${c}
                            I found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
                            """.trimIndent(),
                "", SelectionType.CHARACTER_WISE)
    }

    @VimBehaviourDiffers("land\n")
    fun `test yank to the end`() {
        doTest(parseKeys("viwl", "y"),
                """
                            A Discovery

                            I found it in a legendary ${c}land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
                            """.trimIndent(),
                "land", SelectionType.CHARACTER_WISE)
    }

    fun `test yank multicaret`() {
        doTest(parseKeys("viw", "y"),
                """
                            A Discovery

                            I ${c}found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it ${c}was settled on some sodden sand
                            hard by the torrent of a mountain pass.
                            """.trimIndent(),
                "found\nwas", SelectionType.BLOCK_WISE)
    }


    fun testYankVisualRange() {
        val before = """
            q<caret>werty
            asdf<caret>gh
            <caret>zxcvbn

            """.trimIndent()
        configureByText(before)
        typeText(parseKeys("vey"))

        val lastRegister = VimPlugin.getRegister().lastRegister
        TestCase.assertNotNull(lastRegister)
        val text = lastRegister!!.text
        TestCase.assertNotNull(text)

        typeText(parseKeys("G", "$", "p"))
        val after = """qwerty
asdfgh
zxcvbn<caret>werty
      gh
      zxcvbn
"""
        myFixture.checkResult(after)
    }

    fun `test yank line`() {
        doTest(parseKeys("V", "y"),
                """
                            A Discovery

                            I ${c}found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
                            """.trimIndent(),
                "I found it in a legendary land\n", SelectionType.LINE_WISE)
    }

    fun `test yank last line`() {
        doTest(parseKeys("V", "y"),
                """
                            A Discovery

                            I found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by ${c}the torrent of a mountain pass.""".trimIndent(),
                "hard by the torrent of a mountain pass.", SelectionType.LINE_WISE)
    }

    fun `test yank multicaret line`() {
        doTest(parseKeys("V", "y"),
                """
                            A Discovery

                            I found it in a legendary land
                            all ${c}rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by ${c}the torrent of a mountain pass.""".trimIndent(),
                "all rocks and lavender and tufted grass,\nhard by the torrent of a mountain pass.", SelectionType.LINE_WISE)
    }

    fun testYankVisualLines() {
        val before = """
            q${c}we
            asd
            z${c}xc
            rt${c}y
            fgh
            vbn
            
            """.trimIndent()
        configureByText(before)
        typeText(parseKeys("Vy"))

        val lastRegister = VimPlugin.getRegister().lastRegister
        TestCase.assertNotNull(lastRegister)
        val text = lastRegister!!.text
        TestCase.assertNotNull(text)
        TestCase.assertEquals("""
    qwe
    zxc
    rty

    """.trimIndent(), text)

        typeText(parseKeys("p"))
        val after = """
            qwe
            ${c}qwe
            zxc
            rty
            asd
            zxc
            ${c}qwe
            zxc
            rty
            rty
            ${c}qwe
            zxc
            rty
            fgh
            vbn
            
            """.trimIndent()
        myFixture.checkResult(after)
    }

    fun `test block yank`() {
        doTest(parseKeys("<C-V>lj", "y"),
                """
                            A Discovery

                            I ${c}found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.""".trimIndent(),
                "fo\nl ", SelectionType.BLOCK_WISE)
    }

    private fun doTest(keys: List<KeyStroke>, before: String, expectedText: String, expectedType: SelectionType) {
        configureByText(before)
        typeText(keys)

        val lastRegister = VimPlugin.getRegister().lastRegister ?: run {
            TestCase.fail()
            return
        }
        val text = lastRegister.text
        val type = lastRegister.type
        assertEquals(expectedText, text)
        assertEquals(expectedType, type)
    }
}