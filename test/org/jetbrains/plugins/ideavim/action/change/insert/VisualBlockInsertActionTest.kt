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

class VisualBlockInsertActionTest : VimTestCase() {

    // VIM-1110 |CTRL-V| |v_b_i| |zc|
    fun `test block insert after folds`() {
        configureByJavaText("""$c/**
 * Something to fold.
 */
foo
bar
""")
        typeText(parseKeys("zc", "j", "<C-V>", "j", "I", "X", "<Esc>"))
        myFixture.checkResult("""/**
 * Something to fold.
 */
${c}Xfoo
Xbar
""")
    }

    // VIM-1379 |CTRL-V| |j| |v_b_I|
    fun `test insert visual block with empty line in the middle`() {
        doTest(parseKeys("ll", "<C-V>", "jjI", "_quux_", "<Esc>"),
                """
                    foo

                    bar

                    """.trimIndent(),
                """
                    fo_quux_o

                    ba_quux_r

                    """.trimIndent())
    }

    // VIM-632 |CTRL-V| |v_b_I|
    fun `test change visual block`() {
        doTest(parseKeys("<C-V>", "j", "I", "quux ", "<Esc>"),
                """
                    foo bar
                    ${c}baz quux
                    spam eggs

                    """.trimIndent(),
                ("""
                    foo bar
                    ${c}quux baz quux
                    quux spam eggs

                    """.trimIndent()))
    }

    fun `test visual block insert`() {
        val before = """
            ${c}int a;
            int b;
            int c;
            """.trimIndent()
        typeTextInFile(parseKeys("<C-V>", "2j", "I", "const ", "<Esc>"), before)
        val after = """
            ${c}const int a;
            const int b;
            const int c;
            """.trimIndent()
        myFixture.checkResult(after)
    }


    // VIM-1379 |CTRL-V| |j| |v_b_I|
    fun `test insert visual block with shorter line in the middle`() {
        doTest(parseKeys("ll", "<C-V>", "jjI", "_quux_", "<Esc>"),
                """
                    foo
                    x
                    bar

                    """.trimIndent(),
                ("""
                    fo_quux_o
                    x
                    ba_quux_r

                    """.trimIndent()))
    }

    fun `test insert in non block mode`() {
        doTest(parseKeys("vwIHello<esc>"),
                """
                ${c}A Discovery

                ${c}I found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                Hell${c}oA Discovery

                Hell${c}oI found it in a legendary land
                Hell${c}oall rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
    }
}