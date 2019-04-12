package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectMotionDownActionTest : VimTestCase() {
    fun `test char select simple move`() {
        doTest(parseKeys("gH", "<Down>"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.COMMAND,
                CommandState.SubMode.NONE)
    }

    fun `test select multiple carets`() {
        doTest(parseKeys("gH", "<Down>"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.""".trimIndent(),
                CommandState.Mode.COMMAND,
                CommandState.SubMode.NONE)
    }
}