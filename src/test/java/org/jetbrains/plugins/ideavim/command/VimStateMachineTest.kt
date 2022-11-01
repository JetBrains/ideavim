/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.command

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class VimStateMachineTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in normal`() {
    configureByText("123")
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in insert`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("INSERT", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in replace`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("R"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("REPLACE", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in visual`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("v"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- VISUAL --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in visual line`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("V"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- VISUAL LINE --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in visual block`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("<C-V>"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- VISUAL BLOCK --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in select`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("gh"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- SELECT --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in select line`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("gH"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- SELECT LINE --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in select block`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("g<C-H>"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- SELECT BLOCK --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- (insert) --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command visual`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>v"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- (insert) VISUAL --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command visual block`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O><C-V>"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- (insert) VISUAL BLOCK --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command visual line`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>V"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- (insert) VISUAL LINE --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command select`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>gh"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- (insert) SELECT --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command select block`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>g<C-H>"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- (insert) SELECT BLOCK --", statusString)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test status string in one command select line`() {
    configureByText("123")
    typeText(injector.parser.parseKeys("i<C-O>gH"))
    val statusString = myFixture.editor.vim.vimStateMachine.getStatusString()
    assertEquals("-- (insert) SELECT LINE --", statusString)
  }
}
