@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectMotionLeftActionTest : VimTestCase() {
    fun `test char select simple move`() {
        doTest(parseKeys("viw", "<C-G>", "<Left>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.COMMAND,
                CommandState.SubMode.NONE)
    }

    fun `test select multiple carets`() {
        doTest(parseKeys("viwo", "<C-G>", "<Left>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                ${c}where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I${c} found it in a legendary land
                all rocks and lavender and tufted grass,
                ${c}where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                CommandState.Mode.COMMAND,
                CommandState.SubMode.NONE)
    }
}