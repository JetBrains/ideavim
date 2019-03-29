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

package org.jetbrains.plugins.ideavim.group.motion

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class VisualMotionGroupTest : VimTestCase() {
    fun `test enable character selection`() {
        configureByText("""
            A Discovery

            I <selection>found it</selection> in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().setVisualMode(myFixture.editor, CommandState.SubMode.NONE)
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    }

    fun `test enable character selection multicaret`() {
        configureByText("""
            A Discovery

            I <selection>found it<caret></selection> in a legendary land
            all rocks and lavender <selection>and tufted<caret></selection> grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().setVisualMode(myFixture.editor, CommandState.SubMode.NONE)
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    }


    fun `test enable line selection`() {
        configureByText("""
            A Discovery

            <selection>I found it in a legendary land</selection>
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().setVisualMode(myFixture.editor, CommandState.SubMode.NONE)
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test enable line selection multicaret`() {
        configureByText("""
            A Discovery

            <selection>I found it in a legendary land<caret></selection>
            all rocks and lavender and tufted grass,
            <selection>where it was settled on some sodden sand<caret></selection>
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().setVisualMode(myFixture.editor, CommandState.SubMode.NONE)
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test enable line selection till next line`() {
        configureByText("""
            A Discovery

            <selection>I found it in a legendary land
            </selection>all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().setVisualMode(myFixture.editor, CommandState.SubMode.NONE)
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test enable line selection till next line multicaret`() {
        configureByText("""
            <selection>A Discovery
            <caret></selection>
            <selection>I found it in a legendary land
            <caret></selection>all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().setVisualMode(myFixture.editor, CommandState.SubMode.NONE)
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }


    fun `test enable block selection`() {
        configureByText("""
            A Discovery

            I <selection>found<caret></selection> it in a legendary land
            al<selection>l roc<caret></selection>ks and lavender and tufted grass,
            wh<selection>ere i<caret></selection>t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().setVisualMode(myFixture.editor, CommandState.SubMode.NONE)
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    }

    fun `test enable block selection with different line size`() {
        configureByText("""
            A Discovery

            I <selection>found it in a legendary land<caret></selection>
            al<selection>l rocks and lavender and tufted grass,<caret></selection>
            wh<selection>ere it was settled on some sodden sand<caret></selection>
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().setVisualMode(myFixture.editor, CommandState.SubMode.NONE)
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    }

    fun `test enable block selection with long line`() {
        configureByText("""
            A Discovery

            I <selection>found it in a legendary land<caret></selection>
            al<selection>l rocks and lavender and tufted grass,<caret></selection>
            wh<selection>ere it was settled on some sodden sand12345<caret></selection>6789
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().setVisualMode(myFixture.editor, CommandState.SubMode.NONE)
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    }
}