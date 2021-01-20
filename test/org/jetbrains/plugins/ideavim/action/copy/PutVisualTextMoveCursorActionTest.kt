/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */


package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.Ignore

/**
 * @author Alex Plate
 */

class PutVisualTextMoveCursorActionTest : VimTestCase() {

  fun `test put visual text`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("v2e", "2gp"))
    val after = "legendarylegendary$c in a legendary land"
    myFixture.checkResult(after)
  }

  fun `test put visual text linewise`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.LINE_WISE, false)
    typeText(parseKeys("v2e", "gp"))
    val after = """

            legendary
            $c in a legendary land
            """.trimIndent()
    myFixture.checkResult(after)
  }

  fun `test put visual text line linewise`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("V", "gp"))
    val after = "legendary\n$c"
    myFixture.checkResult(after)
  }

  fun `test replace row`() {
    val file = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val newFile = """
            A Discovery

            Discovery
            ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(file)
    VimPlugin.getRegister().storeText(editor, TextRange(2, 11), SelectionType.LINE_WISE, false)
    typeText(parseKeys("V", "gp"))
    myFixture.checkResult(newFile)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            ound it in a legendary land
             rocks and lavender and tufted grass,
            re it was settled on some sodden sand
            d by the torrent of a mountain pass.
            ${c}A Discovery
    """)
  fun `test put line in block selection`() {
    val file = """
            ${c}A Discovery

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
            d by the torrent of a mountain pass.
            A Discovery
            $c
        """.trimIndent()
    typeTextInFile(parseKeys("Y", "2j", "<C-v>", "2l", "3j", "gp"), file)
    myFixture.checkResult(newFile)
  }

  fun `test Put visual text linewise`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.LINE_WISE, false)
    typeText(parseKeys("v2e", "gP"))
    val after = """

            legendary
            $c in a legendary land
            """.trimIndent()
    myFixture.checkResult(after)
  }

  fun `test Put visual text`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("v2e", "2gP"))
    val after = "legendarylegendary$c in a legendary land"
    myFixture.checkResult(after)
  }

  fun `test Put visual text full line`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("v$", "2gP"))
    val after = "legendarylegendar${c}y"
    myFixture.checkResult(after)
  }

  fun `test Put visual text line linewise`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 25), SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("V", "gP"))
    val after = "legendary\n$c"
    myFixture.checkResult(after)
  }

  fun `test Put line in block selection`() {
    val file = """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val newFile = """
            A Discovery

            A Discovery
            ${c}ound it in a legendary land
             rocks and lavender and tufted grass,
            re it was settled on some sodden sand
            d by the torrent of a mountain pass.
        """.trimIndent()
    typeTextInFile(parseKeys("Y", "2j", "<C-v>", "2l", "3j", "gP"), file)
    myFixture.checkResult(newFile)
  }


  // Legacy tests
  fun `test put visual text linewise multicaret`() {
    val before = """
            q${c}werty
            as${c}dfgh
            ${c}zxcvbn

            """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(14, 21), SelectionType.LINE_WISE, false)
    typeText(parseKeys("vl", "gp"))
    val after = """
            q
            zxcvbn
            ${c}rty
            as
            zxcvbn
            ${c}gh

            zxcvbn
            ${c}cvbn

            """.trimIndent()
    myFixture.checkResult(after)
  }


  @Ignore
  fun `ingoretest put visual block visual line mode`() {
    val before = """
            qw${c}e
            asd
            zxc
            rty
            fgh
            vbn
            """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.BLOCK_WISE, false)
    typeText(parseKeys("<S-v>", "gp"))
    val after = """
            ${c}fgh
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
            qw${c}e
            asd
            zxc
            rty
            fgh
            vbn
            """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.LINE_WISE, false)
    typeText(parseKeys("<C-v>", "h", "gp"))
    val after = """
            q
            fgh
            $c
            asd
            zxc
            rty
            fgh
            vbn
            """.trimIndent()
    myFixture.checkResult(after)
  }


  @Ignore
  fun `ignoretest put visual text multicaret`() {
    val before = "${c}qwe asd ${c}zxc rty ${c}fgh vbn"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("v2e", "2gp"))
    val after = "fghfgh$c fghfgh$c fghfgh$c"
    myFixture.checkResult(after)
  }
}
