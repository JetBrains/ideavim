/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCaseBase
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.junit.jupiter.api.Test

class MacroWithEditingTest : VimTestCaseBase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test print macro`() {
    typeTextInFile(injector.parser.parseKeys("qa" + "iHello<Esc>" + "q"), "")
    setText("")
    typeText(injector.parser.parseKeys("\"ap"))
    assertState("iHello" + 27.toChar())
  }

  @Test
  fun `test copy and perform macro`() {
    typeTextInFile(injector.parser.parseKeys("^v\$h\"wy"), "iHello<Esc>")
    kotlin.test.assertEquals("iHello<Esc>", VimPlugin.getRegister().getRegister('w')?.rawText)
    setText("")
    typeText(injector.parser.parseKeys("@w"))
    waitAndAssert {
      fixture.editor.document.text == "Hello"
    }
  }

  @Test
  fun `test copy and perform macro ctrl_a`() {
    typeTextInFile(injector.parser.parseKeys("^v\$h\"wy"), "<C-A>")
    kotlin.test.assertEquals("<C-A>", VimPlugin.getRegister().getRegister('w')?.rawText)
    setText("1")
    typeText(injector.parser.parseKeys("@w"))
    waitAndAssert {
      fixture.editor.document.text == "2"
    }
  }
}
