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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.group.visual

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.option.SelectModeOptionData
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType
import org.jetbrains.plugins.ideavim.waitAndAssertMode

/**
 * @author Alex Plate
 */
class IdeaVisualControlTest : VimOptionTestCase(SelectModeOptionData.name) {
  @VimOptionDefaultAll
  fun `test enable character selection no selection`() {
    configureByText("""
            A Discovery

            I $s$c${se}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    assertMode(CommandState.Mode.COMMAND)
    assertSubMode(CommandState.SubMode.NONE)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection cursor in the middle`() {
    configureByText("""
            A Discovery

            I ${s}found$c it$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery

            I ${s}found ${c}i${se}t in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

            I ${s}found i${c}t$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """)
  @VimOptionDefaultAll
  fun `test enable character selection cursor on end`() {
    configureByText("""
            A Discovery

            I ${s}found it$c$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery

            I ${s}found it ${c}i${se}n a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection cursor on start`() {
    configureByText("""
            A Discovery

            I $s${c}found it$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery

            I f${s}${c}ound it$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection lineend`() {
    configureByText("""
            A Discovery

            I ${s}found ${c}it in a legendary land$se
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery

            I ${s}found i${c}t${se} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection next line`() {
    configureByText("""
            A Discovery

            I ${s}found ${c}it in a legendary land
            ${se}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery

            I ${s}found i${c}t${se} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection start on line start`() {
    configureByText("""
            A Discovery

            ${s}I found ${c}it ${se}in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery

            ${s}I found i${c}t${se} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection start on line end`() {
    configureByText("""
            A Discovery
            $s
            I found ${c}it ${se}in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery
            $s
            I found i${c}t${se} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection multicaret`() {
    configureByText("""
            A Discovery
            $s
            I found ${c}it ${se}in a legendary land
            all rocks $s$c${se}and lavender and tufted grass,
            where it was $s${c}settled$se on some sodden sand
            hard by the torrent of a mountain ${s}pass.$c$se""".trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery
            $s
            I found i${c}t${se} in a legendary land
            all rocks ${s}a${c}n${se}d lavender and tufted grass,
            where it was s$s${c}ettled$se on some sodden sand
            hard by the torrent of a mountain ${s}pass.$c$se""".trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable line selection`() {
    configureByText("""
            A Discovery

            ${s}I found ${c}it in a legendary land$se
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery

            ${s}I found i${c}t in a legendary land
            ${se}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()

    typeText(parseKeys("j"))
    myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legendary land
            all rocks$c and lavender and tufted grass,
            ${se}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable line selection next line`() {
    configureByText("""
            A Discovery

            ${s}I found ${c}it in a legendary land
            ${se}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()

    typeText(parseKeys("j"))
    myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legendary land
            all rock${c}s and lavender and tufted grass,
            ${se}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable line selection cursor on last line`() {
    configureByText("""
            A Discovery

            ${s}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled ${c}on some sodden sand$se
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()

    typeText(parseKeys("j"))
    myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent o${c}f a mountain pass.$se
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable line selection cursor on first line`() {
    configureByText("""
            A Discovery

            ${s}I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand$se
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()

    typeText(parseKeys("j"))
    myFixture.checkResult("""
            A Discovery

            I found it in a legendary land
            ${s}all rocks and la${c}vender and tufted grass,
            where it was settled on some sodden sand
            ${se}hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable line selection multicaret`() {
    configureByText("""
            A Discovery

            ${s}I found it in a ${c}legendary land$se
            all rocks and lavender and tufted grass,
            ${s}where it was settled ${c}on some sodden sand
            hard by the torrent of a mountain pass.$se
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()

    typeText(parseKeys("j"))
    myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legendary land
            all rocks and la${c}vender and tufted grass,
            ${se}where it was settled on some sodden sand
            ${s}hard by the torrent o${c}f a mountain pass.$se
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable line selection motion up`() {
    configureByText("""
            A Discovery

            I found it in a legendary land
            ${s}all rocks and lavender ${c}and tufted grass,$se
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()

    typeText(parseKeys("k"))
    myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legenda${c}ry land
            all rocks and lavender and tufted grass,
            ${se}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_LINE)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection looks like block`() {
    configureByText("""
            A Discovery

            I ${s}found$c$se it in a legendary land
            al${s}l roc$c${se}ks and lavender and tufted grass,
            wh${s}ere i$c${se}t was settled on some sodden sand
            ha${s}rd by $c${se}the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection`() {
    configureByText("""
            A Discovery

            I ${s}found$c$se it in a legendary land
            al${s}l roc$c${se}ks and lavender and tufted grass,
            wh${s}ere i$c${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery

            I ${s}found ${c}i${se}t in a legendary land
            al${s}l rock${c}s${se} and lavender and tufted grass,
            wh${s}ere it${c} ${se}was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection with longer line`() {
    configureByText("""
            A Discovery

            I ${s}found it in a legendary land$c$se
            al${s}l rocks and lavender and tufted grass,$c$se
            wh${s}ere it was settled on some sodden sand$c$se
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    assertCaretsColour()

    typeText(parseKeys("j"))
    myFixture.checkResult("""
            A Discovery

            I ${s}found it in a legendary lan${c}d$se
            al${s}l rocks and lavender and tufted gras${c}s${se},
            wh${s}ere it was settled on some sodden sa${c}n${se}d
            ha${s}rd by the torrent of a mountain pass.${c}$se
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    assertCaretsColour()
  }

  @VimOptionDefaultAll
  fun `test enable character selection caret to the left`() {
    configureByText("""
            A Discovery

            I $s${c}found$se it in a legendary land
            al$s${c}l roc${se}ks and lavender and tufted grass,
            wh$s${c}ere i${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    assertCaretsColour()

    typeText(parseKeys("l"))
    myFixture.checkResult("""
            A Discovery

            I f$s${c}ound$se it in a legendary land
            all$s$c roc${se}ks and lavender and tufted grass,
            whe$s${c}re i${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertMode(CommandState.Mode.VISUAL)
    assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    assertCaretsColour()
  }

  @VimOptionTestConfiguration(VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [SelectModeOptionData.ideaselection]))
  fun `test control selection`() {
    configureByText("""
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
        """.trimIndent())
    VimListenerManager.EditorListeners.addAll()
    assertMode(CommandState.Mode.COMMAND)

    myFixture.editor.selectionModel.setSelection(5, 10)

    waitAndAssertMode(myFixture, CommandState.Mode.SELECT)
  }

  @VimOptionTestConfiguration(VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [""]))
  fun `test control selection to visual mode`() {
    configureByText("""
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
        """.trimIndent())
    VimListenerManager.EditorListeners.addAll()
    assertMode(CommandState.Mode.COMMAND)

    myFixture.editor.selectionModel.setSelection(5, 10)

    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
  }
}
