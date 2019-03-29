package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectEnterActionTest : VimTestCase() {
    fun `test simple enter`() {
        configureByJavaText("""
        class C {
            int my${c}Var = 5;
        }
        """.trimIndent())
        this.typeText(parseKeys("gh", "<enter>"))
        myFixture.checkResult("""
        class C {
            int my
                ${c}ar = 5;
        }
        """.trimIndent())
        assertMode(CommandState.Mode.INSERT)
    }
}