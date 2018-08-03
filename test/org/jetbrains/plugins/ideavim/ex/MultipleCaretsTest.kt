package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import org.jetbrains.plugins.ideavim.VimTestCase

class MultipleCaretsTest : VimTestCase() {
  fun testGotoToNthCharacter() {
    val before = "qwe rty a<caret>sd\n fgh zx<caret>c <caret>vbn"
    configureByText(before)
    typeText(commandToKeys("go 5"))
    val after = "qwe <caret>rty asd\n fgh zxc vbn"
    myFixture.checkResult(after)
  }

  fun testGotoLine() {
    val before = "qwe\n" + "rty\n" + "asd\n" + "f<caret>gh\n" + "zxc\n" + "v<caret>bn\n"
    configureByText(before)
    typeText(commandToKeys("2"))
    val after = "qwe\n" + "<caret>rty\n" + "asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

  fun testGotoLineInc() {
    val before = "qwe\n" + "rt<caret>y\n" + "asd\n" + "fgh\n" + "zxc\n" + "v<caret>bn\n"
    configureByText(before)
    typeText(commandToKeys("+2"))
    val after = "qwe\n" + "rty\n" + "asd\n" + "<caret>fgh\n" + "zxc\n" + "<caret>vbn\n"
    myFixture.checkResult(after)
  }

  fun testJoinLines() {
    val before = "qwe\n" + "r<caret>ty\n" + "asd\n" + "fg<caret>h\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("j"))
    val after = "qwe\n" + "rty<caret> asd\n" + "fgh<caret> zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

//  fun testJoinVisualLines() {
//    val before = "qwe\n" + "r<caret>ty\n" + "asd\n" + "fg<caret>h\n" + "zxc\n" + "vbn\n"
//    configureByText(before)
//    typeText(parseKeys("vj"))
//    typeText(commandToKeys("j"))
//    val after = "qwe\n" + "rty<caret> asd\n" + "fgh<caret> zxc\n" + "vbn\n"
//    myFixture.checkResult(after)
//  }

  fun testCopyText() {
    val before = "qwe\n" + "rty\n" + "a<caret>sd\n" + "fg<caret>h\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("co 2"))
    val after = "qwe\n" + "rty\n" + "<caret>asd\n" + "<caret>fgh\n" + "asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

//  fun testCopyVisualText() {
//    val before = "qwe\n" + "<caret>rty\n" + "asd\n" + "f<caret>gh\n" + "zxc\n" + "vbn\n"
//    configureByText(before)
//    typeText(parseKeys("vj"))
//    typeText(commandToKeys(":co 2"))
//    val after = "qwe\n" + "rty\n" + "<caret>rty\n" + "asd\n" + "<caret>fgh\n" + "zxc\n" + "asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
//    myFixture.checkResult(after)
//  }

  fun testPutText() {
    val before = "<caret>qwe\n" + "rty\n" + "<caret>as<caret>d\n" + "fgh\n" + "zxc\n" + "vbn\n"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
    typeText(commandToKeys("pu"))
    val after = "qwe\n" + "<caret>zxc\n" + "rty\n" + "asd\n" + "<caret>zxc\n" + "<caret>zxc\n" + "fgh\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

  fun testPutTextCertainLine() {
    val before = "<caret>qwe\n" + "rty\n" + "<caret>as<caret>d\n" + "fgh\n" + "zxc\n" + "vbn\n"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
    typeText(commandToKeys("4pu"))
    val after = "qwe\n" + "rty\n" + "asd\n" + "fgh\n" + "<caret>zxc\n" + "<caret>zxc\n" + "<caret>zxc\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

//  fun testPutVisualLines() {
//    val before = "<caret>qwe\n" + "rty\n" + "as<caret>d\n" + "fgh\n" + "zxc\n" + "vbn\n"
//    val editor = configureByText(before)
//    VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
//
//    typeText(parseKeys("vj"))
//    typeText(commandToKeys("pu"))
//
//    val after = "qwe\n" + "rty\n" + "<caret>zxc\n" + "asd\n" + "fgh\n" + "<caret>zxc\n" + "zxc\n" + "vbn\n"
//    myFixture.checkResult(after)
//  }

  fun testMoveTextBeforeCarets() {
    val before = "qwe\n" + "rty\n" + "<caret>asd\n" + "fgh\n" + "z<caret>xc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("m 1"))
    val after = "qwe\n" + "<caret>asd\n" + "<caret>zxc\n" + "rty\n" + "fgh\n" + "vbn\n"
    myFixture.checkResult(after)

  }

  fun testMoveTextAfterCarets() {
    val before = "q<caret>we\n" + "rty\n" + "<caret>asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("m 4"))
    val after = "rty\n" + "fgh\n" + "zxc\n" + "<caret>qwe\n" + "<caret>asd\n" + "vbn\n"
    myFixture.checkResult(after)
  }

  fun testMoveTextBetweenCarets() {
    val before = "q<caret>we\n" + "rty\n" + "<caret>asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("m 2"))
    val after = "rty\n" + "<caret>qwe\n" + "<caret>asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }
}