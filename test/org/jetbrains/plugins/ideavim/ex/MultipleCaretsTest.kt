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
}