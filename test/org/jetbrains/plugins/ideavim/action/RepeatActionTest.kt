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

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.Test

class RepeatActionTest : VimTestCase() {

  @Test
  fun testSimpleRepeatLastCommand() {
    configureByText("foo foo")
    typeText(parseKeys("cw", "bar", "<Esc>", "w", "."))
    myFixture.checkResult("bar bar")
  }

  @Test
  fun testRepeatChangeToCharInNextLine() {
    configureByText("The first line.\n" +
      "This is the second line.\n" +
      "Third line here, with a comma.\n" +
      "Last line.")
    typeText(parseKeys("j", "ct.", "Change the line to point", "<Esc>", "j0", "."))
    myFixture.checkResult("The first line.\n" +
      "Change the line to point.\n" +
      "Change the line to point.\n" +
      "Last line.")
  }

  // VIM-1644
  @Test
  fun testRepeatChangeInVisualMode() {
    configureByText("foobar foobar")
    typeText(parseKeys("<C-V>llc", "fu", "<Esc>", "w", "."))
    myFixture.checkResult("fubar fubar")
  }

  // VIM-1644
  @Test
  fun testRepeatChangeInVisualModeMultiline() {
    configureByText(
      "There is a red house.\n" +
        "Another red house there.\n" +
        "They have red windows.\n" +
        "Good."
    )
    typeText(parseKeys("www", "<C-V>ec", "blue", "<Esc>", "j0w.", "j0ww."))
    myFixture.checkResult(
      "There is a blue house.\n" +
        "Another blue house there.\n" +
        "They have blue windows.\n" +
        "Good."
    )
  }

}
