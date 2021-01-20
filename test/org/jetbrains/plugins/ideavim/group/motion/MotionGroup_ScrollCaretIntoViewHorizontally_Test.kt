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

package org.jetbrains.plugins.ideavim.group.motion

import com.intellij.openapi.editor.ex.util.EditorUtil
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

@Suppress("ClassName")
class MotionGroup_ScrollCaretIntoViewHorizontally_Test : VimTestCase() {
  fun `test moving right scrolls half screen to right by default`() {
    configureByColumns(200)
    typeText(parseKeys("80|", "l")) // 1 based
    assertPosition(0, 80) // 0 based
    assertVisibleLineBounds(0, 40, 119) // 0 based
  }

  fun `test moving right scrolls half screen to right by default 2`() {
    configureByColumns(200)
    setEditorVisibleSize(100, screenHeight)
    typeText(parseKeys("100|", "l"))
    assertVisibleLineBounds(0, 50, 149)
  }

  fun `test moving right scrolls half screen if moving too far 1`() {
    configureByColumns(400)
    typeText(parseKeys("70|", "41l")) // Move more than half screen width, but scroll less
    assertVisibleLineBounds(0, 70, 149)
  }

  fun `test moving right scrolls half screen if moving too far 2`() {
    configureByColumns(400)
    typeText(parseKeys("50|", "200l")) // Move and scroll more than half screen width
    assertVisibleLineBounds(0, 209, 288)
  }

  fun `test moving right with sidescroll 1`() {
    OptionsManager.sidescroll.set(1)
    configureByColumns(200)
    typeText(parseKeys("80|", "l"))
    assertVisibleLineBounds(0, 1, 80)
  }

  fun `test moving right with sidescroll 2`() {
    OptionsManager.sidescroll.set(2)
    configureByColumns(200)
    typeText(parseKeys("80|", "l"))
    assertVisibleLineBounds(0, 2, 81)
  }

  fun `test moving right with sidescrolloff`() {
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    typeText(parseKeys("70|", "l"))
    assertVisibleLineBounds(0, 30, 109)
  }

  fun `test moving right with sidescroll and sidescrolloff`() {
    OptionsManager.sidescroll.set(1)
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    typeText(parseKeys("70|", "l"))
    assertVisibleLineBounds(0, 1, 80)
  }

  fun `test moving right with large sidescrolloff keeps cursor centred`() {
    OptionsManager.sidescrolloff.set(999)
    configureByColumns(200)
    typeText(parseKeys("50|", "l"))
    assertVisibleLineBounds(0, 10, 89)
  }

  fun `test moving right with inline inlay`() {
    OptionsManager.sidescroll.set(1)
    configureByColumns(200)
    val inlay = addInlay(110, true, 5)
    typeText(parseKeys("100|", "20l"))
    // These columns are hard to calculate, because the visible offset depends on the rendered width of the inlay
    // Also, because we're scrolling right (adding columns to the right) we make the right most column line up
    val textWidth = myFixture.editor.scrollingModel.visibleArea.width - inlay.widthInPixels
    val availableColumns = textWidth / EditorUtil.getPlainSpaceWidth(myFixture.editor)
    assertVisibleLineBounds(0, 119 - availableColumns + 1, 119)
  }

  fun `test moving left scrolls half screen to left by default`() {
    configureByColumns(200)
    typeText(parseKeys("80|zs", "h"))
    assertPosition(0, 78)
    assertVisibleLineBounds(0, 38, 117)
  }

  fun `test moving left scrolls half screen to left by default 2`() {
    configureByColumns(200)
    setEditorVisibleSize(100, screenHeight)
    typeText(parseKeys("100|zs", "h"))
    assertVisibleLineBounds(0, 48, 147)
  }

  fun `test moving left scrolls half screen if moving too far 1`() {
    configureByColumns(400)
    typeText(parseKeys("170|zs", "41h")) // Move more than half screen width, but scroll less
    assertVisibleLineBounds(0, 88, 167)
  }

  fun `test moving left scrolls half screen if moving too far 2`() {
    configureByColumns(400)
    typeText(parseKeys("290|zs", "200h")) // Move more than half screen width, but scroll less
    assertVisibleLineBounds(0, 49, 128)
  }

  fun `test moving left with sidescroll 1`() {
    OptionsManager.sidescroll.set(1)
    configureByColumns(200)
    typeText(parseKeys("100|zs", "h"))
    assertVisibleLineBounds(0, 98, 177)
  }

  fun `test moving left with sidescroll 2`() {
    OptionsManager.sidescroll.set(2)
    configureByColumns(200)
    typeText(parseKeys("100|zs", "h"))
    assertVisibleLineBounds(0, 97, 176)
  }

  fun `test moving left with sidescrolloff`() {
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    typeText(parseKeys("120|zs", "h"))
    assertVisibleLineBounds(0, 78, 157)
  }

  fun `test moving left with sidescroll and sidescrolloff`() {
    OptionsManager.sidescroll.set(1)
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    typeText(parseKeys("120|zs", "h"))
    assertVisibleLineBounds(0, 108, 187)
  }

  fun `test moving left with inline inlay`() {
    OptionsManager.sidescroll.set(1)
    configureByColumns(200)
    val inlay = addInlay(110, true, 5)
    typeText(parseKeys("120|zs", "20h"))
    // These columns are hard to calculate, because the visible offset depends on the rendered width of the inlay
    val textWidth = myFixture.editor.scrollingModel.visibleArea.width - inlay.widthInPixels
    val availableColumns = textWidth / EditorUtil.getPlainSpaceWidth(myFixture.editor)
    assertVisibleLineBounds(0, 99, 99 + availableColumns - 1)
  }

  fun `test moving left with large sidescrolloff keeps cursor centred`() {
    OptionsManager.sidescrolloff.set(999)
    configureByColumns(200)
    typeText(parseKeys("50|", "h"))
    assertVisibleLineBounds(0, 8, 87)
  }
}