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

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.Register
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase


/**
 * @author Alex Plate
 */

class PutVisualTextActionTest : VimTestCase() {

    fun `test put visual text`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("v2e", "2p"))
        val after = "legendarylegendar<caret>y in a legendary land"
        myFixture.checkResult(after)
    }

    fun `test put visual text full line`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("v$", "2p"))
        val after = "legendarylegendar<caret>y"
        myFixture.checkResult(after)
    }

    fun `test put visual text linewise`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.LINE_WISE, false)
        typeText(parseKeys("v2e", "p"))
        val after = """

            <caret>legendary
             in a legendary land
            """.trimIndent()
        myFixture.checkResult(after)
    }

    fun `test put visual text line linewise`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("V", "p"))
        val after = "<caret>legendary\n"
        myFixture.checkResult(after)
    }

    fun `test replace row`() {
        val file = """
            A Discovery

            <caret>I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val newFile = """
            A Discovery

            <caret>Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(file)
        VimPlugin.getRegister().storeText(editor, TextRange(2, 11), SelectionType.LINE_WISE, false)
        typeText(parseKeys("V", "p"))
        myFixture.checkResult(newFile)
    }

    fun `test put text in block selection`() {
        val file = """
            A Discovery

            <caret>I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val newFile = """
            A Discovery

            Discover<caret>y it in a legendary land
            Discoveryks and lavender and tufted grass,
            Discoveryt was settled on some sodden sand
            Discovery the torrent of a mountain pass.
        """.trimIndent()
        val editor = configureByText(file)
        VimPlugin.getRegister().storeText(editor, TextRange(2, 11), SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("<C-v>", "3j", "2e", "p"))
        myFixture.checkResult(newFile)
    }

    fun `test put line in block selection`() {
        val file = """
            <caret>A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val newFile = """
            A Discovery

            ound it in a legendary land
             rocks and lavender and tufted grass,
            re it was settled on some sodden sand
            <caret>d by the torrent of a mountain pass.
            A Discovery

        """.trimIndent()
        typeTextInFile(parseKeys("Y", "2j", "<C-v>", "2l", "3j", "p"), file)
        myFixture.checkResult(newFile)
    }

    fun `test Put visual text linewise`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.LINE_WISE, false)
        typeText(parseKeys("v2e", "P"))
        val after = """

            <caret>legendary
             in a legendary land
            """.trimIndent()
        myFixture.checkResult(after)
    }

    fun `test Put visual text`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("v2e", "2P"))
        val after = "legendarylegendar<caret>y in a legendary land"
        myFixture.checkResult(after)
    }

    fun `test Put visual text full line`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("v$", "2P"))
        val after = "legendarylegendar<caret>y"
        myFixture.checkResult(after)
    }

    fun `test Put visual text line linewise`() {
        val before = "<caret>I found it in a legendary land"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("V", "P"))
        val after = "<caret>legendary\n"
        myFixture.checkResult(after)
    }

    fun `test Put line in block selection`() {
        val file = """
            <caret>A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val newFile = """
            A Discovery

            <caret>A Discovery
            ound it in a legendary land
             rocks and lavender and tufted grass,
            re it was settled on some sodden sand
            d by the torrent of a mountain pass.
        """.trimIndent()
        typeTextInFile(parseKeys("Y", "2j", "<C-v>", "2l", "3j", "P"), file)
        myFixture.checkResult(newFile)
    }


    // Legacy tests
    fun `test put visual text linewise multicaret`() {
        val before = """
            q<caret>werty
            as<caret>dfgh
            <caret>zxcvbn

            """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(14, 21), SelectionType.LINE_WISE, false)
        typeText(parseKeys("vl", "p"))
        val after = """
            q
            <caret>zxcvbn
            rty
            as
            <caret>zxcvbn
            gh

            <caret>zxcvbn
            cvbn

            """.trimIndent()
        myFixture.checkResult(after)
    }


    fun `test put visual block visual line mode`() {
        val before = """
            qw<caret>e
            asd
            zxc
            rty
            fgh
            vbn
            """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.BLOCK_WISE, false)
        typeText(parseKeys("<S-v>", "p"))
        val after = """
            <caret>fgh
            asd
            zxc
            rty
            fgh
            vbn
            """.trimIndent()
        myFixture.checkResult(after)
    }

    fun `test put visual block linewise`() {
        val before = """
            qw<caret>e
            asd
            zxc
            rty
            fgh
            vbn
            """.trimIndent()
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.LINE_WISE, false)
        typeText(parseKeys("<C-v>", "h", "p"))
        val after = """
            <caret>q
            fgh

            asd
            zxc
            rty
            fgh
            vbn
            """.trimIndent()
        myFixture.checkResult(after)
    }


    fun `test put visual text multicaret`() {
        val before = "<caret>qwe asd <caret>zxc rty <caret>fgh vbn"
        val editor = configureByText(before)
        VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
        typeText(parseKeys("v2e", "2p"))
        val after = "fghfg<caret>h fghfg<caret>h fghfg<caret>h"
        myFixture.checkResult(after)
    }

    fun `test put empty text`() {
        val before = """
            qwe
            <caret>asd
            z<caret>xc

            """.trimIndent()
        val editor = configureByText(before)

        val text = "\uFFFF"
        val register = Register('a', SelectionType.CHARACTER_WISE, parseKeys(text))
        VimPlugin.getRegister().selectRegister('a')
        TestCase.assertNull(register.getText())
        VimPlugin.getRegister().storeTextInternal(editor, TextRange(0, 1), text, SelectionType.CHARACTER_WISE, 'a',
                false)

        typeText(parseKeys("p"))
        myFixture.checkResult(before)

        typeText(parseKeys("vlp"))
        val after = """
            qwe
            d
            z

            """.trimIndent()
        myFixture.checkResult(after)
    }

}
