package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionInnerBlockParenActionTest : VimTestCase() {
    // VIM-1633 |v_i)|
    fun `test single letter with single parentheses`() {
        configureByText("(<caret>a)")
        typeText(parseKeys("vi)"))
        assertSelection("a")
    }

    fun `test single letter with double parentheses`() {
        configureByText("((<caret>a))")
        typeText(parseKeys("vi)"))
        assertSelection("(a)")
    }

    fun `test multiline outside parentheses`() {
        configureByText("""(outer
                        |<caret>(inner))""".trimMargin())
        typeText(parseKeys("vi)"))
        assertSelection("inner")
    }

    fun `test multiline in parentheses`() {
        configureByText("""(outer
                        |(inner<caret>))""".trimMargin())
        typeText(parseKeys("vi)"))
        assertSelection("inner")
    }

    fun `test multiline inside of outer parentheses`() {
        configureByText("""(outer
                         |<caret> (inner))""".trimMargin())
        typeText(parseKeys("vi)"))
        assertSelection("""outer
                        | (inner)""".trimMargin())
    }

    fun `test double motion`() {
        configureByText("""(outer
                      |<caret>(inner))""".trimMargin())
        typeText(parseKeys("vi)i)"))
        assertSelection("""outer
                          |(inner)""".trimMargin())
    }

    fun `test motion with count`() {
        configureByText("""(outer
                          |<caret>(inner))""".trimMargin())
        typeText(parseKeys("v2i)"))
        assertSelection("""outer
                      |(inner)""".trimMargin())
    }

    fun `test text object after motion`() {
        configureByText("""(outer
                      |<caret>(inner))""".trimMargin())
        typeText(parseKeys("vlli)"))
        assertSelection("""outer
                      |(inner)""".trimMargin())
    }

    fun `test text object after motion outside parentheses`() {
        configureByText("""(outer
                      |(inner<caret>))""".trimMargin())
        typeText(parseKeys("vlli)"))
        assertSelection("inner")
    }

    fun `test text object after motion inside parentheses`() {
        configureByText("""(outer
                      |(<caret>inner))""".trimMargin())
        typeText(parseKeys("vllli)"))
        assertSelection("inner")
    }

    // VIM-326 |d| |v_ib|
    fun testDeleteInnerBlock() {
        typeTextInFile(parseKeys("di)"),
                "foo(\"b<caret>ar\")\n")
        myFixture.checkResult("foo()\n")
    }

    // VIM-326 |d| |v_ib|
    fun testDeleteInnerBlockCaretBeforeString() {
        typeTextInFile(parseKeys("di)"),
                "foo(<caret>\"bar\")\n")
        myFixture.checkResult("foo()\n")
    }

    // VIM-326 |c| |v_ib|
    fun testChangeInnerBlockCaretBeforeString() {
        typeTextInFile(parseKeys("ci)"),
                "foo(<caret>\"bar\")\n")
        myFixture.checkResult("foo()\n")
    }

    // VIM-392 |c| |v_ib|
    fun testChangeInnerBlockCaretBeforeBlock() {
        typeTextInFile(parseKeys("ci)"),
                "foo<caret>(bar)\n")
        myFixture.checkResult("foo()\n")
        assertOffset(4)
    }

    // |v_ib|
    fun testInnerBlockCrashWhenNoDelimiterFound() {
        typeTextInFile(parseKeys("di)"), "(x\n")
        myFixture.checkResult("(x\n")
    }

    // VIM-275 |d| |v_ib|
    fun testDeleteInnerParensBlockBeforeOpen() {
        typeTextInFile(parseKeys("di)"),
                "foo<caret>(bar)\n")
        myFixture.checkResult("foo()\n")
        assertOffset(4)
    }

    // |d| |v_ib|
    fun testDeleteInnerParensBlockBeforeClose() {
        typeTextInFile(parseKeys("di)"),
                "foo(bar<caret>)\n")
        myFixture.checkResult("foo()\n")
    }
}