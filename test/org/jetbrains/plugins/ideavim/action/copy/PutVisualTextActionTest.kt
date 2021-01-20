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


@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf
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
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
        """.trimIndent()
    configureByText(before)
    typeText(parseKeys("V", "p"))
    val after = """
            ${c}all rocks and lavender and tufted grass,
        """.trimIndent()
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text without copy`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    typeText(parseKeys("ve", "p"))
    val after = "$c it in a legendary land"
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("ve", "p"))
    val after = "legendar${c}y it in a legendary land"
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text twice`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("v2e", "2p"))
    val after = "legendarylegendar${c}y in a legendary land"
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text full line`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("v$", "2p"))
    val after = "legendarylegendar${c}y"
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text multicaret`() {
    val before = "${c}I found ${c}it in a ${c}legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("ve", "p"))
    val after = "legendar${c}y legendar${c}y in a legendar${c}y land"
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text another direction`() {
    val before = "I foun${c}d it in a legendary land"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("vb", "p"))
    val after = "I legendar${c}y it in a legendary land"
    myFixture.checkResult(after)
  }

  // ----- Case 2: Copied | Linewise | --- pasted | Characterwise | ---| small p |--------------------

  @Test
  fun `test put visual text linewise`() {
    val before = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("ve", "p"))
    val after = """
            A Discovery


            ${c}A Discovery
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

            I found$c it in a legendary land
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
            ${c}A Discovery
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
            ${c}hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("ve", "p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
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
            ${c}hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("v$", "p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery

            """.trimIndent()
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text linewise multicaret`() {
    val before = """
            A Discovery

            ${c}I found it in a legendary land
            ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("ve", "p"))
    val after = """
            A Discovery


            ${c}A Discovery
             it in a legendary land

            ${c}A Discovery
             rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
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
            ${c}hard by the$c torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("ve", "p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
             by the
            ${c}A Discovery
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
            ${c}hard by the$c torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("ve", "2p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
            A Discovery
             by the
            ${c}A Discovery
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

            The ${c}features it combines mark it as new
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

            The $c|found| it combines mark it as new
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
            the dingy ${c}underside, the checquered fringe.
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
            the dingy $c|found|, the checquered fringe.
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
            the dingy ${c}underside, the checquered fringe.
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
            the dingy $c|found||found|, the checquered fringe.
                      |l roc||l roc|
                      |ere i||ere i|
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text blockwise multicaret`() {
    val before = """
            A Discovery

            I |found| it in a ${c}legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
    typeText(parseKeys("ve", "p"))
    val after = """
            A Discovery

            I |found| it in a $c|found| land
            al|l roc|ks and la|l roc|vender and tufted grass,
            wh|ere i|t was set|ere i|tled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy $c|found|, the checquered fringe.
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

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("V", "p"))
    val after = """
            A Discovery

            ${c}Discovery
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

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("V", "2p"))
    val after = """
            A Discovery

            ${c}Discovery
            Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery
    """)
  @Test
  fun `test put visual text character to last line`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by ${c}the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("V", "p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery

            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery
    """)
  @Test
  fun `test put visual text character to line multicaret`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("V", "p"))
    val after = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery

            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery
    """)
  @Test
  fun `test put visual text character to line multicaret on same line`() {
    val before = """
            A Discovery

            I found ${c}it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("V", "p"))
    val after = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery

            """.trimIndent()
    myFixture.checkResult(after)
  }

  // ----- Case 5: Copied | Linewise | --- pasted | Linewise | ---| small p |--------------------

  @Test
  fun `test put visual text line to line`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("V", "p"))
    val after = """
            A Discovery

            ${c}A Discovery
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

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("V", "2p"))
    val after = """
            A Discovery

            ${c}A Discovery
            A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """)
  @Test
  fun `test put visual text line to last line`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("V", "p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery

            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """)
  @Test
  fun `test put visual text line to line multicaret`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("V", "p"))
    val after = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery

            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """)
  @Test
  fun `test put visual text line to line multicaret on same line`() {
    val before = """
            A Discovery

            I found ${c}it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("V", "p"))
    val after = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery

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

            The ${c}features it combines mark it as new
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

            $c|found|
            |l roc|
            |ere i|
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|
    """)
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
            the dingy ${c}underside, the checquered fringe.
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
            $c|found|
            |l roc|
            |ere i|



            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|
            |found|
            |l roc|
            |ere i|
    """)
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
            the dingy ${c}underside, the checquered fringe.
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

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            $c|found|
            |l roc|
            |ere i|
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|
    """)
  @Test
  fun `test put visual text blockwise multicaret to line`() {
    val before = """
            A Discovery

            I |found| it in a ${c}legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
    typeText(parseKeys("V", "p"))
    val after = """
            A Discovery

            $c|found|
            |l roc|
            |ere i|
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|



            """.trimIndent()
    myFixture.checkResult(after)
  }

  // ----- Case 7: Copied | Characterwise | --- pasted | Blockwise | ---| small p |--------------------

  @Test
  fun `test put visual block without copy`() {
    val before = """
            I $c|found| it in a legendary land
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

  @Test
  fun `test put visual text character to block`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("<C-V>2e2j", "p"))
    val after = """
            A Discovery

            I Discover${c}y it in a legendary land
            alDiscoveryks and lavender and tufted grass,
            whDiscoveryt was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text character to block motion up`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh$c|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("<C-V>3e2k", "p"))
    val after = """
            A Discovery

            I Discover${c}y it in a legendary land
            alDiscoveryks and lavender and tufted grass,
            whDiscoveryt was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text character to block twice`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("<C-V>2e2j", "2p"))
    val after = """
            A Discovery

            I DiscoveryDiscover${c}y it in a legendary land
            alDiscoveryDiscoveryks and lavender and tufted grass,
            whDiscoveryDiscoveryt was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text character to block with dollar motion`() {
    val before = """
            A Discovery

            I $c|found it in a legendary land
            al|l rocks and lavender and tufted grass,[ additional characters]
            wh|ere it was settled on some sodden sand
            ha|rd by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "Discovery", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("<C-V>3j$", "p"))
    val after = """
            A Discovery

            I Discover${c}y
            alDiscovery
            whDiscovery
            haDiscovery
            """.trimIndent()
    myFixture.checkResult(after)
  }

  // ----- Case 8: Copied | Linewise | --- pasted | Blockwise | ---| small p |--------------------

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ${c}A Discovery
            hard by the torrent of a mountain pass.
    """)
  @Test
  fun `test put visual text line to block`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
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
            wht was settled on some sodden sand
            ${c}A Discovery

            hard by the torrent of a mountain pass.
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text line to block before caret`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("<C-V>2e2j", "P"))
    val after = """
            A Discovery

            ${c}A Discovery
            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ${c}A Discovery
            A Discovery
            hard by the torrent of a mountain pass.
            """)
  @Test
  fun `test put visual text line to block twice`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
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
            wht was settled on some sodden sand
            ${c}A Discovery
            A Discovery

            hard by the torrent of a mountain pass.
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ha the torrent of a mountain pass.
            ${c}A Discovery
    """)
  @Test
  fun `test put visual text line to block till end`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
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
            ha the torrent of a mountain pass.
            ${c}A Discovery

            """.trimIndent()
    myFixture.checkResult(after)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            I
            a
            w
            ${c}A Discovery
            hard by the torrent of a mountain pass.
    """)
  @Test
  fun `test put visual text line to block with dollar motion`() {
    val before = """
            A Discovery

            I${c}| found it in a legendary land
            a|ll rocks and lavender and tufted grass,[ additional characters]
            w|here it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, before rangeOf "A Discovery\n", SelectionType.LINE_WISE, false)
    typeText(parseKeys("<C-V>2j$", "p"))
    val after = """
            A Discovery

            I
            a
            w
            ${c}A Discovery

            hard by the torrent of a mountain pass.
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

            The $c|features| it combines mark it as new
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

            The $c|found| it combines mark it as new
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

            The $c|features| it combines mark it as new
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

            The $c|found| it combines mark it as new
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

            The $c|features| it combines mark it as new
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

            The $c|found| it combines mark it as new
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
            akin to $c|moonlight|, tempering its blue,
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
            akin to $c|found|, tempering its blue,
            the ding|l roc|de, the checquered fringe.
                    |ere i|
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @Test
  fun `test put visual text blockwise to block with dollar motion`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|features it combines mark it as new
            to s|cience: shape and shade -- the special tinge,[ additional characters]
            akin| to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
        """.trimIndent()
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
    typeText(parseKeys("<C-V>2j$", "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|found|
            to s|l roc|
            akin|ere i|
            the dingy underside, the checquered fringe.
            """.trimIndent()
    myFixture.checkResult(after)
  }

}
