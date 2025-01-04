/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.command

import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.ui.widgets.mode.VimModeWidget
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class VimShowModeTest : VimTestCase() {
  @Test
  fun `test status string in normal`() {
    configureByText("123")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("NORMAL", statusString)
  }

  @Test
  fun `test status string in insert`() {
    configureByText("123")
    typeText("i")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("INSERT", statusString)
  }

  @Test
  fun `test status string in replace`() {
    configureByText("123")
    typeText("R")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("REPLACE", statusString)
  }

  @Test
  fun `test status string in visual`() {
    configureByText("123")
    typeText("v")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("VISUAL", statusString)
  }

  @Test
  fun `test status string in visual line`() {
    configureByText("123")
    typeText("V")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("V-LINE", statusString)
  }

  @Test
  fun `test status string in visual block`() {
    configureByText("123")
    typeText("<C-V>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("V-BLOCK", statusString)
  }

  @Test
  fun `test status string in select`() {
    configureByText("123")
    typeText("gh")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("SELECT", statusString)
  }

  @Test
  fun `test status string in select line`() {
    configureByText("123")
    typeText("gH")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("S-LINE", statusString)
  }

  @Test
  fun `test status string in select block`() {
    configureByText("123")
    typeText("g<C-H>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("S-BLOCK", statusString)
  }

  @Test
  fun `test status string for Insert Normal mode`() {
    configureByText("123")
    typeText("i<C-O>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(insert) NORMAL", statusString)
  }

  @Test
  fun `test status string after escape out of Insert Normal mode`() {
    configureByText("123")
    typeText("i<C-O><Esc>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("INSERT", statusString)
  }

  @Test
  fun `test status string for Replace pending Normal mode`() {
    configureByText("123")
    typeText("R<C-O>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(replace) NORMAL", statusString)
  }

  @Test
  fun `test status string in Insert Visual mode`() {
    configureByText("123")
    typeText("i<C-O>v")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(insert) VISUAL", statusString)
  }

  @Test
  fun `test status string in Replace pending Visual mode`() {
    configureByText("123")
    typeText("R<C-O>v")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(replace) VISUAL", statusString)
  }

  @Test
  fun `test status string in Insert Visual block mode`() {
    configureByText("123")
    typeText("i<C-O><C-V>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(insert) V-BLOCK", statusString)
  }

  @Test
  fun `test status string in Replace pending Visual block mode`() {
    configureByText("123")
    typeText("R<C-O><C-V>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(replace) V-BLOCK", statusString)
  }

  @Test
  fun `test status string in Insert Visual line mode`() {
    configureByText("123")
    typeText("i<C-O>V")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(insert) V-LINE", statusString)
  }

  @Test
  fun `test status string in Replace pending Visual line mode`() {
    configureByText("123")
    typeText("R<C-O>V")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(replace) V-LINE", statusString)
  }

  @Test
  fun `test status string in Insert Select mode`() {
    configureByText("123")
    typeText("i<C-O>gh")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(insert) SELECT", statusString)
  }

  @Test
  fun `test status string in Insert Select mode 2`() {
    configureByText("123")
    enterCommand("set selectmode=key keymodel=startsel")
    typeText("i<S-Right>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(insert) SELECT", statusString)
  }

  @Test
  fun `test status string in Insert Select block mode`() {
    configureByText("123")
    typeText("i<C-O>g<C-H>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(insert) S-BLOCK", statusString)
  }

  @Test
  fun `test status string in Insert Select line mode`() {
    configureByText("123")
    typeText("i<C-O>gH")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(insert) S-LINE", statusString)
  }

  @Test
  fun `test status string in Visual with Select pending`() {
    configureByText("123")
    enterCommand("set selectmode=key keymodel=startsel")
    typeText("<S-Right><C-O>")
    val statusString = VimModeWidget.getModeText(fixture.editor.vim.mode)
    assertEquals("(select) VISUAL", statusString)
  }
}
