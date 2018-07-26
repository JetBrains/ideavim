package org.jetbrains.plugins.ideavim.ex

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
}