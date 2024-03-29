/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.command

import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.ui.widgets.mode.ModeWidgetFactory
import com.maddyhome.idea.vim.ui.widgets.mode.VimModeWidget
import org.jetbrains.plugins.ideavim.VimTestCase

// TODO it would be cool to test widget, but status bar is not initialized
class VimShowModeTest : VimTestCase() {
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in normal`() {
//    configureByText("123")
//    val widget = getModeWidget()
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in insert`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("i"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- INSERT --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in replace`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("R"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- REPLACE --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in visual`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("v"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- VISUAL --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in visual line`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("V"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- VISUAL LINE --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in visual block`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("<C-V>"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- VISUAL BLOCK --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in select`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("gh"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- SELECT --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in select line`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("gH"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- SELECT LINE --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in select block`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("g<C-H>"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- SELECT BLOCK --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in one command`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("i<C-O>"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- (insert) --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in one command visual`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("i<C-O>v"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- (insert) VISUAL --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in one command visual block`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("i<C-O><C-V>"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- (insert) VISUAL BLOCK --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in one command visual line`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("i<C-O>V"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- (insert) VISUAL LINE --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in one command select`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("i<C-O>gh"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- (insert) SELECT --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in one command select block`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("i<C-O>g<C-H>"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- (insert) SELECT BLOCK --", statusString)
//  }
//
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  @Test
//  fun `test status string in one command select line`() {
//    configureByText("123")
//    typeText(injector.parser.parseKeys("i<C-O>gH"))
//    val statusString = fixture.editor.vim.getStatusString()
//    kotlin.test.assertEquals("-- (insert) SELECT LINE --", statusString)
//  }
//

  // Always return null
  private fun getModeWidget(): VimModeWidget? {
    val project = fixture.editor?.project ?: return null
    val statusBar = WindowManager.getInstance()?.getStatusBar(project) ?: return null
    return statusBar.getWidget(ModeWidgetFactory.ID) as? VimModeWidget
  }
}
