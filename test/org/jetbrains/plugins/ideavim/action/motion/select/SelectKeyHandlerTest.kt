package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviourDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class SelectKeyHandlerTest : VimTestCase() {
    fun `test type in select mode`() {
        val typed = "Hello"
        this.doTest(parseKeys("gh", "<S-Right>", typed),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                ${typed}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test char mode on empty line`() {
        val typed = "Hello"
        this.doTest(parseKeys("gh", typed),
                """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery
                $typed
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test char mode multicaret`() {
        val typed = "Hello"
        this.doTest(parseKeys("gh", "<S-Right>", typed),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I ${typed}und it in a legendary land
                all rocks and ${typed}vender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test line mode`() {
        val typed = "Hello"
        this.doTest(parseKeys("gH", typed),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                $typed
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test line mode empty line`() {
        val typed = "Hello"
        this.doTest(parseKeys("gH", typed),
                """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery
                $typed
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test line mode multicaret`() {
        val typed = "Hello"
        this.doTest(parseKeys("gH", typed),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                Hello
                Hello
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test type in select block mode`() {
        val typed = "Hello"
        this.doTest(parseKeys("g<C-H>", "<S-Down>", "<S-Right>", typed),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                ${typed}found it in a legendary land
                ${typed}l rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertCaretsColour()
    }

    @VimBehaviourDiffers(originalVimAfter = """
                A Discovery
                Hello
                Hellofound it in a legendary land
                Hellol rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """)
    fun `test block mode empty line`() {
        val typed = "Hello"
        this.doTest(parseKeys("g<C-H>", "<S-Down>".repeat(2), "<S-Right>", typed),
                """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                $typed found it in a legendary land
                ${typed}ll rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertCaretsColour()
    }

    fun `test block mode longer line`() {
        val typed = "Hello"
        this.doTest(parseKeys("g<C-H>", "<S-Down>", "<S-Right>".repeat(2), typed),
                """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I found it in a legendary lan$typed
                all rocks and lavender and tu${typed}d grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertCaretsColour()
    }

    fun `test block mode longer line with esc`() {
        val typed = "Hello"
        this.doTest(parseKeys("g<C-H>", "<S-Down>", "<S-Right>".repeat(2), typed, "<esc>"),
                """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I found it in a legendary lanHell${c}o
                all rocks and lavender and tuHell${c}od grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertCaretsColour()
        assertMode(CommandState.Mode.COMMAND)
    }
}