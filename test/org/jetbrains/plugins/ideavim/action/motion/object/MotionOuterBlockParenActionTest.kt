package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionOuterBlockParenActionTest : VimTestCase() {
    // VIM-1633 |v_a)|
    fun `test single letter with single parentheses`() {
        configureByText("(<caret>a)")
        typeText(parseKeys("va)"))
        assertSelection("(a)")
    }

    fun `test single letter with double parentheses`() {
        configureByText("((<caret>a))")
        typeText(parseKeys("va)"))
        assertSelection("(a)")
    }

    fun `test multiline outside parentheses`() {
        configureByText("""(outer
                      |<caret>(inner))""".trimMargin())
        typeText(parseKeys("va)"))
        assertSelection("(inner)")
    }

    fun `test multiline in parentheses`() {
        configureByText("""(outer
                      |(inner<caret>))""".trimMargin())
        typeText(parseKeys("va)"))
        assertSelection("(inner)")
    }

    fun `test multiline inside of outer parentheses`() {
        configureByText("""(outer
                     |<caret> (inner))""".trimMargin())
        typeText(parseKeys("va)"))
        assertSelection("""(outer
                        | (inner))""".trimMargin())
    }

    fun `test double motion`() {
        configureByText("""(outer
                      |<caret>(inner))""".trimMargin())
        typeText(parseKeys("va)a)"))
        assertSelection("""(outer
                      |(inner))""".trimMargin())
    }

    fun `test motion with count`() {
        configureByText("""(outer
                      |<caret>(inner))""".trimMargin())
        typeText(parseKeys("v2a)"))
        assertSelection("""(outer
                      |(inner))""".trimMargin())
    }

    fun `test text object after motion`() {
        configureByText("""(outer
                      |<caret>(inner))""".trimMargin())
        typeText(parseKeys("vlla)"))
        assertSelection("""(outer
                      |(inner))""".trimMargin())
    }

    fun `test text object after motion outside parentheses`() {
        configureByText("""(outer
                      |(inner<caret>))""".trimMargin())
        typeText(parseKeys("vlla)"))
        assertSelection("(inner)")
    }

    // |d| |v_ab|
    fun testDeleteOuterBlock() {
        typeTextInFile(parseKeys("da)"),
                "foo(b<caret>ar, baz);\n")
        myFixture.checkResult("foo;\n")
    }
}