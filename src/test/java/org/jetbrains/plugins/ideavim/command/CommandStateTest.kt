/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.command

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class CommandStateTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in normal`() {
    configureByText("123")
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in insert`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("INSERT", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in replace`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("R"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("REPLACE", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in visual`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("v"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- VISUAL --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in visual line`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("V"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- VISUAL LINE --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in visual block`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("<C-V>"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- VISUAL BLOCK --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in select`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("gh"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- SELECT --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in select line`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("gH"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- SELECT LINE --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in select block`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("g<C-H>"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- SELECT BLOCK --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- (insert) --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command visual`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>v"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- (insert) VISUAL --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command visual block`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O><C-V>"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- (insert) VISUAL BLOCK --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command visual line`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>V"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- (insert) VISUAL LINE --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command select`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>gh"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- (insert) SELECT --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command select block`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>g<C-H>"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- (insert) SELECT BLOCK --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command select line`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>gH"))
    val statusString = myFixture.editor.vim.commandState.getStatusString()
    assertEquals("-- (insert) SELECT LINE --", statusString)
  }
}
