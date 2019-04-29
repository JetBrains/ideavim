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

package org.jetbrains.plugins.ideavim.action.motion.visual

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.vimSelectionStart
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf

class VisualToggleCharacterModeActionTest : VimTestCase() {
    fun `test vim start after enter visual`() {
        val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        configureByText(before)
        typeText(parseKeys("v"))
        val startOffset = (before rangeOf "found").startOffset
        assertEquals(startOffset, myFixture.editor.caretModel.primaryCaret.vimSelectionStart)
    }

    fun `test vim start after enter visual with motion`() {
        val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        configureByText(before)
        typeText(parseKeys("vl"))
        val startOffset = (before rangeOf "found").startOffset
        assertEquals(startOffset, myFixture.editor.caretModel.primaryCaret.vimSelectionStart)
    }

    fun `test vim start after enter visual multicaret`() {
        val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was ${c}settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        configureByText(before)
        typeText(parseKeys("vl"))
        val startOffset1 = (before rangeOf "found").startOffset
        val startOffset2 = (before rangeOf "settled").startOffset
        assertEquals(startOffset1, myFixture.editor.caretModel.allCarets[0].vimSelectionStart)
        assertEquals(startOffset2, myFixture.editor.caretModel.allCarets[1].vimSelectionStart)
    }

    fun `test vim start after enter visual multicaret with merge`() {
        val before = """
            A Discovery

            I found it in ${c}a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was ${c}settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        configureByText(before)
        typeText(parseKeys("v2e"))
        val startOffset1 = (before rangeOf "legendary").startOffset
        val startOffset2 = (before rangeOf "settled").startOffset
        assertEquals(startOffset1, myFixture.editor.caretModel.allCarets[0].vimSelectionStart)
        assertEquals(startOffset2, myFixture.editor.caretModel.allCarets[1].vimSelectionStart)
    }

    fun `test vim start after enter visual multicaret with merge to left`() {
        val before = """
            A Discovery

            I found it in ${c}a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was ${c}settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        configureByText(before)
        typeText(parseKeys("v2b"))
        val startOffset1 = (before rangeOf "legendary").startOffset
        val startOffset2 = (before rangeOf "settled").startOffset
        assertEquals(startOffset1, myFixture.editor.caretModel.allCarets[0].vimSelectionStart)
        assertEquals(startOffset2, myFixture.editor.caretModel.allCarets[1].vimSelectionStart)
    }
}