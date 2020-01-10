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

  // See https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/normal.c#L631
  fun `test count resets to 999999999L if gets too large`() {
    configureByText("1")
    typeText(parseKeys("12345678901234567890<C-A>"))
    myFixture.checkResult("1000000000")
  }

  fun `test count select register count operator count motion`() {
    configureByText("${c}123456789012345678901234567890")
    typeText(parseKeys("2\"a3d4l")) // Delete 24 characters
    myFixture.checkResult("567890")
  }

  fun `test multiple select register counts`() {
    configureByText("${c}12345678901234567890123456789012345678901234567890")
    typeText(parseKeys("2\"a2\"b2\"b2d2l")) // Delete 32 characters
    myFixture.checkResult("345678901234567890")
  }
}
