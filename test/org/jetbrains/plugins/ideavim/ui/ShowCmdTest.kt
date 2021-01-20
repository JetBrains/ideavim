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

package org.jetbrains.plugins.ideavim.ui

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.ui.ShowCmd
import org.jetbrains.plugins.ideavim.VimTestCase

class ShowCmdTest: VimTestCase() {
  override fun setUp() {
    super.setUp()
    val before = "${c}I found it in a legendary land"
    configureByText(before)
  }

  fun `test showcmd on by default`() {
    OptionsManager.resetAllOptions()
    assertTrue("showcmd", OptionsManager.showcmd.isSet)
  }

  fun `test showcmd shows nothing if disabled`() {
    OptionsManager.showcmd.reset()

    typeText(parseKeys("3"))
    assertEquals("", getShowCmdText())
  }

  fun `test showcmd count`() {
    typeText(parseKeys("3"))
    assertEquals("3", getShowCmdText())
  }

  fun `test showcmd multiple count`() {
    typeText(parseKeys("320"))
    assertEquals("320", getShowCmdText())
  }

  fun `test showcmd incomplete command`() {
    typeText(parseKeys("3d2"))
    assertEquals("3d2", getShowCmdText())
  }

  fun `test showcmd clears on completed command`() {
    typeText(parseKeys("3d2w"))
    assertEquals("", getShowCmdText())
  }

  fun `test showcmd clears on Escape`() {
    typeText(parseKeys("3d2<Esc>"))
    assertEquals("", getShowCmdText())
  }

  fun `test showcmd expands mapped keys`() {
    enterCommand("nmap rrrr d")
    typeText(parseKeys("32rrrr"))
    assertEquals("32d", getShowCmdText())
  }

  // TODO: This test fails because IdeaVim's mapping handler doesn't correctly expand unhandled keys on timeout
//  fun `test showcmd expands ambiguous mapped keys on timeout`() {
    // `rrr` should timeout and replay `rr` which is mapped to `42`
//    enterCommand("nmap rr 42")
//    enterCommand("nmap rrr 55")
//    typeText(parseKeys("12rr"))
//    waitAndAssert { "1242" == getShowCmdText() }
//  }

  fun `test showcmd updates count when expanding mapped keys`() {
    enterCommand("nmap rrrr 55d")
    typeText(parseKeys("32rrrr"))
    assertEquals("3255d", getShowCmdText())
  }

  fun `test showcmd removes count on Delete`() {
    typeText(parseKeys("32<Del>"))
    assertEquals("3", getShowCmdText())
  }

  fun `test showcmd clears if Delete all count chars`() {
    typeText(parseKeys("32<Del><Del>"))
    assertEquals("", getShowCmdText())
  }

  fun `test showcmd removes motion count on Delete`() {
    typeText(parseKeys("32d44<Del><Del>"))
    assertEquals("32d", getShowCmdText())
  }

  fun `test showcmd clears if Delete on operator`() {
    typeText(parseKeys("32d<Del>"))
    assertEquals("", getShowCmdText())
  }

  fun `test showcmd shows nothing in insert mode`() {
    typeText(parseKeys("i", "hello world"))
    assertEquals("", getShowCmdText())
  }

  fun `test showcmd shows digraph entry in insert mode`() {
    typeText(parseKeys("i", "<C-K>O"))
    assertEquals("^KO", getShowCmdText())
  }

  fun `test showcmd clears when cancelling digraph entry in insert mode`() {
    typeText(parseKeys("i", "<C-K>O", "<Esc>"))
    assertEquals("", getShowCmdText())
  }

  fun `test showcmd shows literal entry in insert mode`() {
    typeText(parseKeys("i", "<C-V>12"))
    assertEquals("^V12", getShowCmdText())
  }

  // Vim seems to hard code <C-Q> and swaps it for <C-V>
  @VimBehaviorDiffers("^V12")
  fun `test showcmd shows literal entry with CTRL-Q in insert mode`() {
    typeText(parseKeys("i", "<C-Q>12"))
    assertEquals("^Q12", getShowCmdText())
  }

  fun `test showcmd clears when cancelling literal entry in insert mode`() {
    typeText(parseKeys("i", "<C-V>1", "<Esc>"))
    assertEquals("", getShowCmdText())
  }

  fun `test showcmd shows register entry in insert mode`() {
    typeText(parseKeys("i", "<C-R>"))
    assertEquals("^R", getShowCmdText())
  }

  fun `test showcmd clears when cancelling registry entry in insert mode`() {
    typeText(parseKeys("i", "<C-R>", "<Esc>"))
    assertEquals("", getShowCmdText())
  }

  // Note that Vim shows the number of lines, or rows x cols for visual mode. We don't because IntelliJ already
  // shows this kind of information in the position panel
  fun `test showcmd works in visual mode`() {
    typeText(parseKeys("v", "32f"))
    assertEquals("32f", getShowCmdText())
  }

  fun `test showcmd works in single command mode`() {
    typeText(parseKeys("i", "<C-O>", "32f"))
    assertEquals("32f", getShowCmdText())
  }

  fun `test showcmd only shows last 10 characters of buffer`() {
    typeText(parseKeys("12345678900987654321"))
    assertEquals("0987654321", getShowCmdText())
  }

  fun `test showcmd tooltip shows full buffer`() {
    typeText(parseKeys("12345678900987654321"))
    assertEquals("12345678900987654321", getShowCmdTooltipText())
  }

  fun `test showcmd shows select register command`() {
    typeText(parseKeys("\"a32d"))
    assertEquals("\"a32d", getShowCmdText())
  }

  fun `test showcmd shows count and select register command`() {
    typeText(parseKeys("32\"ad"))
    assertEquals("32\"ad", getShowCmdText())
  }

  fun `test showcmd shows repeated select register with counts`() {
    typeText(parseKeys("22\"a22\"a22\"a22\"a22d22"))
    assertEquals("a22\"a22d22", getShowCmdText())
    assertEquals("22\"a22\"a22\"a22\"a22d22", getShowCmdTooltipText())
  }

  private fun getShowCmdText() = ShowCmd.getWidgetText(myFixture.editor!!)
  private fun getShowCmdTooltipText() = ShowCmd.getFullText(myFixture.editor!!)
}