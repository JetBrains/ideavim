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

package org.jetbrains.plugins.ideavim.group.motion

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
@Suppress("ClassName")
class VisualMotionGroup_SetVisualMode_Test : VimTestCase() {
  fun `test enable character selection`() {
    configureByText("""
            A Discovery

            I ${s}found it$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    VimPlugin.getVisualMotion().setVisualMode(myFixture.editor)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
  }

  fun `test enable character selection multicaret`() {
    configureByText("""
            A Discovery

            I ${s}found it$c$se in a legendary land
            all rocks and lavender ${s}and tufted$c$se grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    VimPlugin.getVisualMotion().setVisualMode(myFixture.editor)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
  }


  fun `test enable line selection`() {
    configureByText("""
            A Discovery

            ${s}I found it in a legendary land$se
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    VimPlugin.getVisualMotion().setVisualMode(myFixture.editor)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
  }

  fun `test enable line selection multicaret`() {
    configureByText("""
            A Discovery

            ${s}I found it in a legendary land$c$se
            all rocks and lavender and tufted grass,
            ${s}where it was settled on some sodden sand$c$se
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    VimPlugin.getVisualMotion().setVisualMode(myFixture.editor)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
  }

  fun `test enable line selection till next line`() {
    configureByText("""
            A Discovery

            ${s}I found it in a legendary land
            ${se}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    VimPlugin.getVisualMotion().setVisualMode(myFixture.editor)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
  }

  fun `test enable line selection till next line multicaret`() {
    configureByText("""
            ${s}A Discovery
            $c$se
            ${s}I found it in a legendary land
            $c${se}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    VimPlugin.getVisualMotion().setVisualMode(myFixture.editor)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
  }


  fun `test enable block selection`() {
    configureByText("""
            A Discovery

            I ${s}found$c$se it in a legendary land
            al${s}l roc$c${se}ks and lavender and tufted grass,
            wh${s}ere i$c${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    VimPlugin.getVisualMotion().setVisualMode(myFixture.editor)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
  }

  fun `test enable block selection with different line size`() {
    configureByText("""
            A Discovery

            I ${s}found it in a legendary land$c$se
            al${s}l rocks and lavender and tufted grass,$c$se
            wh${s}ere it was settled on some sodden sand$c$se
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    VimPlugin.getVisualMotion().setVisualMode(myFixture.editor)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
  }

  fun `test enable block selection with long line`() {
    configureByText("""
            A Discovery

            I ${s}found it in a legendary land$c$se
            al${s}l rocks and lavender and tufted grass,$c$se
            wh${s}ere it was settled on some sodden sand12345$c${se}6789
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    VimPlugin.getVisualMotion().setVisualMode(myFixture.editor)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
  }
}
