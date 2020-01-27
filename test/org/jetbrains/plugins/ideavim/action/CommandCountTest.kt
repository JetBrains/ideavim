package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class CommandCountTest : VimTestCase() {
  fun `test count operator motion`() {
    configureByText("${c}1234567890")
    typeText(parseKeys("3dl"))
    myFixture.checkResult("4567890")
  }

  fun `test operator count motion`() {
    configureByText("${c}1234567890")
    typeText(parseKeys("d3l"))
    myFixture.checkResult("4567890")
  }

  fun `test count operator count motion`() {
    configureByText("${c}1234567890")
    typeText(parseKeys("2d3l"))
    myFixture.checkResult("7890")
  }
}
