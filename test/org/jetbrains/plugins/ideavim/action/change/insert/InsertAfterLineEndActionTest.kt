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

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertAfterLineEndActionTest : VimTestCase() {
    fun `test insert after line end action`() {
        typeTextInFile(parseKeys("A", " four", "<ESC>"),
                """
                    one two <caret>three
                    sev<caret>en si<caret>x five

                    """.trimIndent())
        myFixture.checkResult("""
    one two three fou<caret>r
    seven six five fou<caret>r

    """.trimIndent())
    }

    fun `test multiple carets`() {
        doTest(parseKeys("AHello<esc>"),
                """
                <caret>A Discovery

                <caret>I found it in a legendary land
                all rocks and <caret>lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A DiscoveryHell<caret>o

                I found it in a legendary landHell<caret>o
                all rocks and lavender and tufted grass,Hell<caret>o
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
    }
}