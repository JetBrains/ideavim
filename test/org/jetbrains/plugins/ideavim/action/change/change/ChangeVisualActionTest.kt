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

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class ChangeVisualActionTest : VimTestCase() {
    fun `test multiple line change`() {
        val keys = parseKeys("VjcHello<esc>")
        val before = """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val after = """
            Hello
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    }

    fun `test change visual action`() {
        typeTextInFile(parseKeys("v2lc", "aaa", "<ESC>"),
                "abcd${c}ffffff${c}abcde${c}aaaa\n")
        assertMode(CommandState.Mode.COMMAND)
        myFixture.checkResult("abcdaa${c}afffaa${c}adeaa${c}aa\n")
    }

    // VIM-1379 |CTRL-V| |j| |v_b_c|
    fun `test change visual block with empty line in the middle`() {
        doTest(parseKeys("ll", "<C-V>", "ljjc", "_quux_", "<Esc>"),
                "foo foo\n" +
                        "\n" +
                        "bar bar\n",
                ("fo_quux_foo\n" +
                        "\n" +
                        "ba_quux_bar\n"),
                CommandState.Mode.COMMAND,
                CommandState.SubMode.NONE)
    }


    // VIM-1379 |CTRL-V| |j| |v_b_c|
    fun `test change visual block with shorter line in the middle`() {
        doTest(parseKeys("ll", "<C-V>", "ljjc", "_quux_", "<Esc>"),
                "foo foo\n" +
                        "x\n" +
                        "bar bar\n",
                ("fo_quux_foo\n" +
                        "x\n" +
                        "ba_quux_bar\n"),
                CommandState.Mode.COMMAND,
                CommandState.SubMode.NONE)
    }
}