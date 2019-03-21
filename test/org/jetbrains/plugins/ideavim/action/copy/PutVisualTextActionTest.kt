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


package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviourDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.Test


/**
 * @author Alex Plate
 *
 * c - characterwise
 * l - linewise
 * b - blockwise
 *
 *   Table of test cases
 *
 *            ||    copied with
 *            ||  c  |  l  |  b  |
 *   p      ======================
 *   a w    c ||  1  |  2  |  3  |
 *   s i    --||------------------
 *   t t    l ||  4  |  5  |  6  |
 *   e h    --||------------------
 *   d      b ||  7  |  8  |  9  |
 */
class PutVisualTextActionTest : VimTestCase() {

    // ----- Case 1: Copied | Characterwise | --- pasted | Characterwise | ---| small p |--------------------

    @Test
    fun `test put visual line without copy`() {
        val before = """
            <caret>I found it in a legendary land
            all rocks and lavender and tufted grass,
        """.trimIndent()
        configureByText(before)
        typeText(parseKeys("V", "p"))
        val after = """
            <caret>all rocks and lavender and tufted grass,
        """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text without copy`() {
        val before = "<caret>I found it in a legendary land"
        configureByText(before)
        typeText(parseKeys("ve", "p"))
        val after = "<caret> it in a legendary land"
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = "legendar<caret>y it in a legendary land"
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text twice`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("v2e", "2p"))
        val after = "legendarylegendar<caret>y in a legendary land"
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text full line`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("v$", "2p"))
        val after = "legendarylegendar<caret>y"
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text multicaret`() {
        val before = "<caret>I found <caret>it in a <caret>legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = "legendar<caret>y legendar<caret>y in a legendar<caret>y land"
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text another direction`() {
        val before = "I foun<caret>d it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("vb", "p"))
        val after = "I legendar<caret>y it in a legendary land"
        myFixture.checkResult(after)
    }

    // ----- Case 2: Copied | Linewise | --- pasted | Characterwise | ---| small p |--------------------

    @Test
    fun `test put visual text linewise`() {
        val before = """
            A Discovery

            <caret>I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = """
            A Discovery


            <caret>A Discovery
             it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text linewise in middle`() {
        val before = """
            A Discovery

            I found<caret> it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = """
            A Discovery

            I found
            <caret>A Discovery
             in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text linewise last line`() {
        val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            <caret>A Discovery
             by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text linewise last line full line`() {
        val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("v$", "p"))
        val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            <caret>A Discovery

            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text linewise multicaret`() {
        val before = """
            A Discovery

            <caret>I found it in a legendary land
            <caret>all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = """
            A Discovery


            <caret>A Discovery
             it in a legendary land

            <caret>A Discovery
             rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            <caret>A Discovery
             by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text linewise multicaret on same line`() {
        val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>hard by the<caret> torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            <caret>A Discovery
             by the
            <caret>A Discovery
             of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text linewise multicaret on same line twice`() {
        val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>hard by the<caret> torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("ve", "2p"))
        val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            <caret>A Discovery
            A Discovery
             by the
            <caret>A Discovery
            A Discovery
             of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    // ----- Case 3: Copied | Blockwise | --- pasted | Characterwise | ---| small p |--------------------

    @Test
    fun `test put visual text blockwise`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The <caret>features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The <caret>|found| it combines mark it as new
            to s|l roc|cience: shape and shade -- the special tinge,
            akin|ere i| to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text blockwise on last line`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy <caret>underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy <caret>|found|, the checquered fringe.
                      |l roc|
                      |ere i|
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text blockwise on last line twice`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy <caret>underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("ve", "2p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy <caret>|found||found|, the checquered fringe.
                      |l roc||l roc|
                      |ere i||ere i|
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text blockwise multicaret`() {
        val before = """
            A Discovery

            I |found| it in a <caret>legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy <caret>underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("ve", "p"))
        val after = """
            A Discovery

            I |found| it in a <caret>|found| land
            al|l roc|ks and la|l roc|vender and tufted grass,
            wh|ere i|t was set|ere i|tled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy <caret>|found|, the checquered fringe.
                      |l roc|
                      |ere i|
            """.trimIndent()
        myFixture.checkResult(after)
    }

    // ----- Case 4: Copied | Characterwise | --- pasted | Linewise | ---| small p |--------------------

    @Test
    fun `test put visual text character to line`() {
        val before = """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            <caret>Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text character to line twice`() {
        val before = """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("V", "2p"))
        val after = """
            A Discovery

            <caret>Discovery
            Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>Discovery
    """, trimIndent = true)
    @Test
    fun `test put visual text character to last line`() {
        val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by <caret>the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>Discovery

            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            <caret>Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>Discovery
    """, trimIndent = true)
    @Test
    fun `test put visual text character to line multicaret`() {
        val before = """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the <caret>torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            <caret>Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>Discovery

            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            <caret>Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>Discovery
    """, trimIndent = true)
    @Test
    fun `test put visual text character to line multicaret on same line`() {
        val before = """
            A Discovery

            I found <caret>it in a <caret>legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the <caret>torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            <caret>Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>Discovery

            """.trimIndent()
        myFixture.checkResult(after)
    }

    // ----- Case 5: Copied | Linewise | --- pasted | Linewise | ---| small p |--------------------

    @Test
    fun `test put visual text line to line`() {
        val before = """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            <caret>A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text line to line twice`() {
        val before = """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("V", "2p"))
        val after = """
            A Discovery

            <caret>A Discovery
            A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>A Discovery
    """, trimIndent = true)
    @Test
    fun `test put visual text line to last line`() {
        val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the <caret>torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>A Discovery

            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            <caret>A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>A Discovery
    """, trimIndent = true)
    @Test
    fun `test put visual text line to line multicaret`() {
        val before = """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the <caret>torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            <caret>A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>A Discovery

            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            <caret>A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>A Discovery
    """, trimIndent = true)
    @Test
    fun `test put visual text line to line multicaret on same line`() {
        val before = """
            A Discovery

            I found <caret>it in a <caret>legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the <caret>torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            <caret>A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            <caret>A Discovery

            """.trimIndent()
        myFixture.checkResult(after)
    }

    // ----- Case 6: Copied | Blockwise | --- pasted | Linewise | ---| small p |--------------------


    @Test
    fun `test put visual text blockwise to line`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The <caret>features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            <caret>|found|
            |l roc|
            |ere i|
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            <caret>|found|
            |l roc|
            |ere i|
    """, trimIndent = true)
    @Test
    fun `test put visual text blockwise on last line to line`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy <caret>underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            <caret>|found|
            |l roc|
            |ere i|




            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            <caret>|found|
            |l roc|
            |ere i|
            |found|
            |l roc|
            |ere i|
    """, trimIndent = true)
    @Test
    fun `test put visual text blockwise on last line twice to line`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy <caret>underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("V", "2p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            |found||found|
            |l roc||l roc|
            |ere i||ere i|




            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            <caret>|found|
            |l roc|
            |ere i|
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            <caret>|found|
            |l roc|
            |ere i|
    """, trimIndent = true)
    @Test
    fun `test put visual text blockwise multicaret to line`() {
        val before = """
            A Discovery

            I |found| it in a <caret>legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy <caret>underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = """
            A Discovery

            <caret>|found|
            |l roc|
            |ere i|
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            <caret>|found|
            |l roc|
            |ere i|




            """.trimIndent()
        myFixture.checkResult(after)
    }

    // ----- Case 7: Copied | Characterwise | --- pasted | Blockwise | ---| small p |--------------------

    @Test
    fun `test put visual block without copy`() {
        val before = """
            I <caret>|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
        """.trimIndent()
        configureByText(before)
        typeText(parseKeys("<C-V>2ej", "p"))
        val after = """
            I  it in a legendary land
            alks and lavender and tufted grass,
        """.trimIndent()
        myFixture.checkResult(after)
    }


    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            I Discover<caret>y it in a legendary land
            alDiscoveryks and lavender and tufted grass,
            whDiscoveryt was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """, trimIndent = true, description = "Different cursor position")
    @Test
    fun `test put visual text character to block`() {
        val before = """
            A Discovery

            I <caret>|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("<C-V>2e2j", "p"))
        val after = """
            A Discovery

            I Discovery it in a legendary land
            alDiscoveryks and lavender and tufted grass,
            whDiscover<caret>yt was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            I DiscoveryDiscover<caret>y it in a legendary land
            alDiscoveryDiscoveryks and lavender and tufted grass,
            whDiscoveryDiscoveryt was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """, trimIndent = true, description = "Different cursor position")
    @Test
    fun `test put visual text character to block twice`() {
        val before = """
            A Discovery

            I <caret>|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("<C-V>2e2j", "2p"))
        val after = """
            A Discovery

            I DiscoveryDiscovery it in a legendary land
            alDiscoveryDiscoveryks and lavender and tufted grass,
            whDiscoveryDiscover<caret>yt was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    // ----- Case 8: Copied | Linewise | --- pasted | Blockwise | ---| small p |--------------------


    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            <caret>A Discovery
            hard by the torrent of a mountain pass.
    """, trimIndent = true)
    @Test
    fun `test put visual text line to block`() {
        val before = """
            A Discovery

            I <caret>|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("<C-V>2e2j", "p"))
        val after = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            <caret>wht was settled on some sodden sand
            A Discovery

            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            <caret>A Discovery
            A Discovery
            hard by the torrent of a mountain pass.
            """, trimIndent = true)
    @Test
    fun `test put visual text line to block twice`() {
        val before = """
            A Discovery

            I <caret>|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("<C-V>2e2j", "2p"))
        val after = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            <caret>wht was settled on some sodden sand
            A Discovery

            A Discovery

            hard by the torrent of a mountain pass.
            """.trimIndent()
        myFixture.checkResult(after)
    }


    @VimBehaviourDiffers(originalVimAfter = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ha the torrent of a mountain pass.
            <caret>A Discovery
    """, trimIndent = true)
    @Test
    fun `test put visual text line to block till end`() {
        val before = """
            A Discovery

            I <caret>|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            ha|rd by| the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
        typeText(parseKeys("<C-V>2e3j", "p"))
        val after = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            <caret>ha the torrent of a mountain pass.
            A Discovery

            """.trimIndent()
        myFixture.checkResult(after)
    }

    // ----- Case 9: Copied | Blockwise | --- pasted | Blockwise | ---| small p |--------------------

    @Test
    fun `test put visual text blockwise to block`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The <caret>|features| it combines mark it as new
            to s|cience: |shape and shade -- the special tinge,
            akin| to moon|light, tempering its blue,
            the dingy underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("<C-V>2e2j", "p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The <caret>|found| it combines mark it as new
            to s|l roc|shape and shade -- the special tinge,
            akin|ere i|light, tempering its blue,
            the dingy underside, the checquered fringe.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text blockwise to longer block`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The <caret>|features| it combines mark it as new
            to s|cience: |shape and shade -- the special tinge,
            akin| to moon|light, tempering its blue,
            the |dingy un|derside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("<C-V>2e3j", "p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The <caret>|found| it combines mark it as new
            to s|l roc|shape and shade -- the special tinge,
            akin|ere i|light, tempering its blue,
            the derside, the checquered fringe.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text blockwise to shorter block`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The <caret>|features| it combines mark it as new
            to s|cience: |shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("<C-V>2ej", "p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The <caret>|found| it combines mark it as new
            to s|l roc|shape and shade -- the special tinge,
            akin|ere i| to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
            """.trimIndent()
        myFixture.checkResult(after)
    }

    @Test
    fun `test put visual text blockwise to shorter block on line end`() {
        val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to <caret>|moonlight|, tempering its blue,
            the ding|y undersi|de, the checquered fringe.
        """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("<C-V>elj", "p"))
        val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to <caret>|found|, tempering its blue,
            the ding|l roc|de, the checquered fringe.
                    |ere i|
            """.trimIndent()
        myFixture.checkResult(after)
    }
}

infix fun String.rangeOf(str: String): TextRange {
    val clearString = this.replace("<caret>", "")
    val indexOf = clearString.indexOf(str)
    if (indexOf == -1) throw RuntimeException("$str was not found in $clearString")

    return TextRange(indexOf, indexOf + str.length)
}

fun Editor.rangeOf(first: String, nLinesDown: Int): TextRange {
    val starts = ArrayList<Int>()
    val ends = ArrayList<Int>()

    val indexOf = document.text.replace("<caret>", "").indexOf(first)
    if (indexOf == -1) throw RuntimeException("$first was not found in $this")

    val position = offsetToLogicalPosition(indexOf)
    if (position.line + nLinesDown > document.lineCount) throw RuntimeException("To much lines")

    starts += indexOf
    ends += indexOf + first.length

    for (i in 1..nLinesDown) {
        val nextOffset = logicalPositionToOffset(LogicalPosition(position.line + i, position.column))
        starts += nextOffset
        ends += nextOffset + first.length
    }
    return TextRange(starts.toIntArray(), ends.toIntArray())
}
