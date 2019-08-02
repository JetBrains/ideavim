package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class PutHandlerTest : VimTestCase() {
  // VIM-550 |:put|
  fun `test put creates new line`() {
    myFixture.configureByText("a.txt", "Test\n" + "Hello <caret>World!\n")
    typeText(parseKeys("\"ayw"))
    typeText(commandToKeys("put a"))
    myFixture.checkResult("Test\n" +
      "Hello World!\n" +
      "<caret>World\n")
  }

  // VIM-551 |:put|
  fun `test put default`() {
    myFixture.configureByText("a.txt", "<caret>Hello World!\n")
    typeText(parseKeys("yw"))
    typeText(commandToKeys("put"))
    myFixture.checkResult("Hello World!\n" + "<caret>Hello \n")
  }
}