package org.jetbrains.plugins.ideavim.group.motion

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
@Suppress("ClassName")
class VisualMotionGroup_ControlNonVimSelection_Test : VimTestCase() {
    fun `test enable character selection no selection`() {
        configureByText("""
            A Discovery

            I $s$c${se}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.COMMAND)
        assertSubMode(CommandState.SubMode.NONE)
        assertCaretsColour()
    }

    fun `test enable character selection cursor in the middle`() {
        configureByText("""
            A Discovery

            I ${s}found$c it$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery

            I ${s}found $c${se}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()
    }

    fun `test enable character selection cursor on end`() {
        configureByText("""
            A Discovery

            I ${s}found it$c$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery

            I ${s}found it $c${se}in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()
    }

    fun `test enable character selection cursor on start`() {
        configureByText("""
            A Discovery

            I $s${c}found it$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery

            I f$c${s}ound it$se in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()
    }

    fun `test enable character selection lineend`() {
        configureByText("""
            A Discovery

            I ${s}found ${c}it in a legendary land$se
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery

            I ${s}found i$c${se}t in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()
    }

    fun `test enable character selection next line`() {
        configureByText("""
            A Discovery

            I ${s}found ${c}it in a legendary land
            ${se}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery

            I ${s}found i$c${se}t in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()
    }

    fun `test enable character selection start on line start`() {
        configureByText("""
            A Discovery

            ${s}I found ${c}it ${se}in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery

            ${s}I found i$c${se}t in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()
    }

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
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery
            $s
            I found i$c${se}t in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()
    }

    fun `test enable character selection multicaret`() {
        configureByText("""
            A Discovery
            $s
            I found ${c}it ${se}in a legendary land
            all rocks $s$c${se}and lavender and tufted grass,
            where it was $s${c}settled$se on some sodden sand
            hard by the torrent of a mountain ${s}pass.$c$se""".trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery
            $s
            I found i$c${se}t in a legendary land
            all rocks ${s}a$c${se}nd lavender and tufted grass,
            where it was s$s${c}ettled$se on some sodden sand
            hard by the torrent of a mountain ${s}pass.$c$se""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()
    }

    fun `test enable line selection`() {
        configureByText("""
            A Discovery

            ${s}I found ${c}it in a legendary land$se
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery

            ${s}I found i${c}t in a legendary land$se
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()

        typeText(parseKeys("<S-Down>"))
        myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legendary land
            all rocks$c and lavender and tufted grass,$se
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()
    }

    fun `test enable line selection next line`() {
        configureByText("""
            A Discovery

            ${s}I found ${c}it in a legendary land
            ${se}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()

        typeText(parseKeys("<S-Down>"))
        myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legendary land
            all rock${c}s and lavender and tufted grass,$se
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()
    }

    fun `test enable line selection cursor on last line`() {
        configureByText("""
            A Discovery

            ${s}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled ${c}on some sodden sand$se
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()

        typeText(parseKeys("<S-Down>"))
        myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent o${c}f a mountain pass.$se
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()
    }

    fun `test enable line selection cursor on first line`() {
        configureByText("""
            A Discovery

            ${s}I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand$se
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()

        typeText(parseKeys("<S-Down>"))
        myFixture.checkResult("""
            A Discovery

            I found it in a legendary land
            ${s}all rocks and la${c}vender and tufted grass,
            where it was settled on some sodden sand$se
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()
    }

    fun `test enable line selection multicaret`() {
        configureByText("""
            A Discovery

            ${s}I found it in a ${c}legendary land$se
            all rocks and lavender and tufted grass,
            ${s}where it was settled ${c}on some sodden sand
            hard by the torrent of a mountain pass.$se
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()

        typeText(parseKeys("<S-Down>"))
        myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legendary land
            all rocks and la${c}vender and tufted grass,$se
            where it was settled on some sodden sand
            ${s}hard by the torrent o${c}f a mountain pass.$se
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()
    }

    fun `test enable line selection motion up`() {
        configureByText("""
            A Discovery

            I found it in a legendary land
            ${s}all rocks and lavender ${c}and tufted grass,$se
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()

        typeText(parseKeys("<S-Up>"))
        myFixture.checkResult("""
            A Discovery

            ${s}I found it in a legenda${c}ry land
            all rocks and lavender and tufted grass,$se
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
        assertCaretsColour()
    }

    fun `test enable character selection looks like block`() {
        configureByText("""
            A Discovery

            I ${s}found$c$se it in a legendary land
            al${s}l roc$c${se}ks and lavender and tufted grass,
            wh${s}ere i$c${se}t was settled on some sodden sand
            ha${s}rd by $c${se}the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
        assertCaretsColour()
    }

    fun `test enable character selection `() {
        configureByText("""
            A Discovery

            I ${s}found$c$se it in a legendary land
            al${s}l roc$c${se}ks and lavender and tufted grass,
            wh${s}ere i$c${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery

            I ${s}found $c${se}it in a legendary land
            al${s}l rock$c${se}s and lavender and tufted grass,
            wh${s}ere it$c$se was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()
    }

    fun `test enable character selection with longer line`() {
        configureByText("""
            A Discovery

            I ${s}found it in a legendary land$c$se
            al${s}l rocks and lavender and tufted grass,$c$se
            wh${s}ere it was settled on some sodden sand$c$se
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()

        typeText(parseKeys("<S-Down>"))
        myFixture.checkResult("""
            A Discovery

            I ${s}found it in a legendary land$c$se
            al${s}l rocks and lavender and tufted gras$c${se}s,
            wh${s}ere it was settled on some sodden sa$c${se}nd
            ha${s}rd by the torrent of a mountain pass.$c$se
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()
    }

    fun `test enable character selection caret to the left`() {
        configureByText("""
            A Discovery

            I $s${c}found$se it in a legendary land
            al$s${c}l roc${se}ks and lavender and tufted grass,
            wh$s${c}ere i${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.COMMAND)
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(myFixture.editor)
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()

        typeText(parseKeys("<S-Right>"))
        myFixture.checkResult("""
            A Discovery

            I f$s${c}ound$se it in a legendary land
            all$s$c roc${se}ks and lavender and tufted grass,
            whe$s${c}re i${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()
    }
}