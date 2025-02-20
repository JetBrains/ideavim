/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.motion

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.helper.EditorHelper
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

@Suppress("ClassName")
class MotionGroup_ScrollCaretIntoViewHorizontally_Test : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right scrolls half screen to right by default`() {
    configureByColumns(200)
    typeText("80|", "l") // 1 based
    assertPosition(0, 80) // 0 based
    assertVisibleLineBounds(0, 40, 119) // 0 based
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right scrolls half screen to right by default 2`() {
    configureByColumns(200)
    setEditorVisibleSize(100, screenHeight)
    typeText("100|", "l")
    assertVisibleLineBounds(0, 50, 149)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right scrolls half screen if moving too far 1`() {
    configureByColumns(400)
    typeText("70|", "41l") // Move more than half screen width, but scroll less
    assertVisibleLineBounds(0, 70, 149)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right scrolls half screen if moving too far 2`() {
    configureByColumns(400)
    typeText("50|", "200l") // Move and scroll more than half screen width
    assertVisibleLineBounds(0, 209, 288)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right with sidescroll 1`() {
    configureByColumns(200)
    enterCommand("set sidescroll=1")
    typeText("80|", "l")
    assertVisibleLineBounds(0, 1, 80)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right with sidescroll 2`() {
    configureByColumns(200)
    enterCommand("set sidescroll=2")
    typeText("80|", "l")
    assertVisibleLineBounds(0, 2, 81)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right with sidescrolloff`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")
    typeText("70|", "l")
    assertVisibleLineBounds(0, 30, 109)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right with sidescroll and sidescrolloff`() {
    configureByColumns(200)
    enterCommand("set sidescroll=1")
    enterCommand("set sidescrolloff=10")
    typeText("70|", "l")
    assertVisibleLineBounds(0, 1, 80)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right with large sidescrolloff keeps cursor centred`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=999")
    typeText("50|", "l")
    assertVisibleLineBounds(0, 10, 89)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving right with inline inlay`() {
    configureByColumns(200)
    enterCommand("set sidescroll=1")
    val inlay = addInlay(110, true, 5)
    typeText("100|", "20l")
    // These columns are hard to calculate, because the visible offset depends on the rendered width of the inlay
    // Also, because we're scrolling right (adding columns to the right) we make the right most column line up
    ApplicationManager.getApplication().invokeAndWait {
      val textWidth = fixture.editor.scrollingModel.visibleArea.width - inlay.widthInPixels
      val availableColumns = (textWidth / EditorHelper.getPlainSpaceWidthFloat(fixture.editor)).roundToInt()
      assertVisibleLineBounds(0, 119 - availableColumns + 1, 119)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left scrolls half screen to left by default`() {
    configureByColumns(200)
    typeText("80|zs", "h")
    assertPosition(0, 78)
    assertVisibleLineBounds(0, 38, 117)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left scrolls half screen to left by default 2`() {
    configureByColumns(200)
    setEditorVisibleSize(100, screenHeight)
    typeText("100|zs", "h")
    assertVisibleLineBounds(0, 48, 147)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left scrolls half screen if moving too far 1`() {
    configureByColumns(400)
    typeText("170|zs", "41h") // Move more than half screen width, but scroll less
    assertVisibleLineBounds(0, 88, 167)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left scrolls half screen if moving too far 2`() {
    configureByColumns(400)
    typeText("290|zs", "200h") // Move more than half screen width, but scroll less
    assertVisibleLineBounds(0, 49, 128)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left with sidescroll 1`() {
    configureByColumns(200)
    enterCommand("set sidescroll=1")
    typeText("100|zs", "h")
    assertVisibleLineBounds(0, 98, 177)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left with sidescroll 2`() {
    configureByColumns(200)
    enterCommand("set sidescroll=2")
    typeText("100|zs", "h")
    assertVisibleLineBounds(0, 97, 176)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left with sidescrolloff`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")
    typeText("120|zs", "h")
    assertVisibleLineBounds(0, 78, 157)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left with sidescroll and sidescrolloff`() {
    configureByColumns(200)
    enterCommand("set sidescroll=1")
    enterCommand("set sidescrolloff=10")
    typeText("120|zs", "h")
    assertVisibleLineBounds(0, 108, 187)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left with inline inlay`() {
    configureByColumns(200)
    enterCommand("set sidescroll=1")
    val inlay = addInlay(110, true, 5)
    typeText("120|zs", "20h")
    // These columns are hard to calculate, because the visible offset depends on the rendered width of the inlay
    ApplicationManager.getApplication().invokeAndWait {
      val textWidth = fixture.editor.scrollingModel.visibleArea.width - inlay.widthInPixels
      val availableColumns = (textWidth / EditorHelper.getPlainSpaceWidthFloat(fixture.editor)).roundToInt()
      assertVisibleLineBounds(0, 99, 99 + availableColumns - 1)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving left with large sidescrolloff keeps cursor centred`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=999")
    typeText("50|", "h")
    assertVisibleLineBounds(0, 8, 87)
  }
}
