/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ui

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.ui.ShowCmd
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class ShowCmdTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    val before = "${c}Lorem ipsum dolor sit amet,"
    configureByText(before)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd on by default`() {
    kotlin.test.assertTrue(options().isSet(Options.showcmd))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd shows nothing if disabled`() {
    enterCommand("set noshowcmd")
    typeText(injector.parser.parseKeys("3"))
    kotlin.test.assertEquals("", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd count`() {
    typeText(injector.parser.parseKeys("3"))
    kotlin.test.assertEquals("3", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd multiple count`() {
    typeText(injector.parser.parseKeys("320"))
    kotlin.test.assertEquals("320", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd incomplete command`() {
    typeText(injector.parser.parseKeys("3d2"))
    kotlin.test.assertEquals("3d2", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd clears on completed command`() {
    typeText(injector.parser.parseKeys("3d2w"))
    kotlin.test.assertEquals("", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd clears on Escape`() {
    typeText(injector.parser.parseKeys("3d2<Esc>"))
    kotlin.test.assertEquals("", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd expands mapped keys`() {
    enterCommand("nmap rrrr d")
    typeText(injector.parser.parseKeys("32rrrr"))
    kotlin.test.assertEquals("32d", getShowCmdText())
  }

  // TODO: This test fails because IdeaVim's mapping handler doesn't correctly expand unhandled keys on timeout
  @Test
  @Disabled
  fun `test showcmd expands ambiguous mapped keys on timeout`() {
//     `rrr` should timeout and replay `rr` which is mapped to `42`
    enterCommand("nmap rr 42")
    enterCommand("nmap rrr 55")
    typeText(injector.parser.parseKeys("12rr"))
    waitAndAssert { "1242" == getShowCmdText() }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd updates count when expanding mapped keys`() {
    enterCommand("nmap rrrr 55d")
    typeText(injector.parser.parseKeys("32rrrr"))
    kotlin.test.assertEquals("3255d", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd removes count on Delete`() {
    typeText(injector.parser.parseKeys("32<Del>"))
    kotlin.test.assertEquals("3", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd clears if Delete all count chars`() {
    typeText(injector.parser.parseKeys("32<Del><Del>"))
    kotlin.test.assertEquals("", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd removes motion count on Delete`() {
    typeText(injector.parser.parseKeys("32d44<Del><Del>"))
    kotlin.test.assertEquals("32d", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd clears if Delete on operator`() {
    typeText(injector.parser.parseKeys("32d<Del>"))
    kotlin.test.assertEquals("", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd shows nothing in insert mode`() {
    typeText(injector.parser.parseKeys("i" + "hello world"))
    kotlin.test.assertEquals("", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd shows digraph entry in insert mode`() {
    typeText(injector.parser.parseKeys("i" + "<C-K>O"))
    kotlin.test.assertEquals("^KO", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd clears when cancelling digraph entry in insert mode`() {
    typeText(injector.parser.parseKeys("i" + "<C-K>O" + "<Esc>"))
    kotlin.test.assertEquals("", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd shows literal entry in insert mode`() {
    typeText(injector.parser.parseKeys("i" + "<C-V>12"))
    kotlin.test.assertEquals("^V12", getShowCmdText())
  }

  // Vim seems to hard code <C-Q> and swaps it for <C-V>
  @VimBehaviorDiffers("^V12")
  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd shows literal entry with CTRL-Q in insert mode`() {
    typeText(injector.parser.parseKeys("i" + "<C-Q>12"))
    kotlin.test.assertEquals("^Q12", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd clears when cancelling literal entry in insert mode`() {
    typeText(injector.parser.parseKeys("i" + "<C-V>1" + "<Esc>"))
    kotlin.test.assertEquals("", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd shows register entry in insert mode`() {
    typeText(injector.parser.parseKeys("i" + "<C-R>"))
    kotlin.test.assertEquals("^R", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd clears when cancelling registry entry in insert mode`() {
    typeText(injector.parser.parseKeys("i" + "<C-R>" + "<Esc>"))
    kotlin.test.assertEquals("", getShowCmdText())
  }

  // Note that Vim shows the number of lines, or rows x cols for visual mode. We don't because IntelliJ already
  // shows this kind of information in the position panel
  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd works in visual mode`() {
    typeText(injector.parser.parseKeys("v" + "32f"))
    kotlin.test.assertEquals("32f", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd works in single command mode`() {
    typeText(injector.parser.parseKeys("i" + "<C-O>" + "32f"))
    kotlin.test.assertEquals("32f", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd only shows last 10 characters of buffer`() {
    typeText(injector.parser.parseKeys("12345678900987654321"))
    kotlin.test.assertEquals("0987654321", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd tooltip shows full buffer`() {
    typeText(injector.parser.parseKeys("12345678900987654321"))
    kotlin.test.assertEquals("12345678900987654321", getShowCmdTooltipText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd shows select register command`() {
    typeText(injector.parser.parseKeys("\"a32d"))
    kotlin.test.assertEquals("\"a32d", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd shows count and select register command`() {
    typeText(injector.parser.parseKeys("32\"ad"))
    kotlin.test.assertEquals("32\"ad", getShowCmdText())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SHOW_CMD)
  @Test
  fun `test showcmd shows repeated select register with counts`() {
    typeText(injector.parser.parseKeys("22\"a22\"a22\"a22\"a22d22"))
    kotlin.test.assertEquals("a22\"a22d22", getShowCmdText())
    kotlin.test.assertEquals("22\"a22\"a22\"a22\"a22d22", getShowCmdTooltipText())
  }

  private fun getShowCmdText() = ShowCmd.getWidgetText(fixture.editor!!)

  private fun getShowCmdTooltipText() = ShowCmd.getFullText(fixture.editor!!)
}
