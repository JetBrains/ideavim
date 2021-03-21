/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssert

class MacroWithEditingTest : VimTestCase() {
  fun `test print macro`() {
    typeTextInFile(parseKeys("qa", "iHello<Esc>", "q"), "")
    setText("")
    typeText(parseKeys("\"ap"))
    myFixture.checkResult("iHello<Esc>")
  }

  fun `test copy and perform macro`() {
    typeTextInFile(parseKeys("^v\$h\"wy"), "iHello<Esc>")
    assertEquals("iHello<Esc>", VimPlugin.getRegister().getRegister('w')?.rawText)
    setText("")
    typeText(parseKeys("@w"))
    waitAndAssert {
      myFixture.editor.document.text == "Hello"
    }
  }

  fun `test copy and perform macro ctrl_a`() {
    typeTextInFile(parseKeys("^v\$h\"wy"), "<C-A>")
    assertEquals("<C-A>", VimPlugin.getRegister().getRegister('w')?.rawText)
    setText("1")
    typeText(parseKeys("@w"))
    waitAndAssert {
      myFixture.editor.document.text == "2"
    }
  }
}
