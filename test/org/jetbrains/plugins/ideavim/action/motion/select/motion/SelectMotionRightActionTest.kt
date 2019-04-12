@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectMotionRightActionTest : VimTestCase() {
    fun `test char select simple move`() {
        doTest(parseKeys("viw", "<C-G>", "<Right>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I found${c} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.COMMAND,
                CommandState.SubMode.NONE)
    }

    fun `test select multiple carets`() {
        doTest(parseKeys("viw", "<C-G>", "<Right>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden san${c}d
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I found${c} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden san${c}d
                hard by the torrent of a mountain pass.""".trimIndent(),
                CommandState.Mode.COMMAND,
                CommandState.SubMode.NONE)
    }
}