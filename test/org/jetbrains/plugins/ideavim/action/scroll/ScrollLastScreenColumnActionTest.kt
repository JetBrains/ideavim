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

package org.jetbrains.plugins.ideavim.action.scroll

import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.Assert

/*
                                                       *ze*
ze                      Scroll the text horizontally to position the cursor
                        at the end (right side) of the screen.  This only
                        works when 'wrap' is off.
 */
class ScrollLastScreenColumnActionTest : VimTestCase() {
  fun `test scroll caret column to last screen column`() {
    configureByColumns(200)
    typeText(StringHelper.parseKeys("100|", "ze"))
    assertVisibleLineBounds(0, 20, 99)
  }

  fun `test scroll caret column to last screen column with sidescrolloff`() {
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    typeText(StringHelper.parseKeys("100|", "ze"))
    assertVisibleLineBounds(0, 30, 109)
  }

  fun `test scroll at or near start of line does nothing`() {
    configureByColumns(200)
    typeText(StringHelper.parseKeys("10|", "ze"))
    assertVisibleLineBounds(0, 0, 79)
  }

  fun `test scroll end of line to last screen column`() {
    configureByColumns(200)
    typeText(StringHelper.parseKeys("$", "ze"))
    assertVisibleLineBounds(0, 120, 199)
  }

  fun `test scroll end of line to last screen column with sidescrolloff`() {
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    typeText(StringHelper.parseKeys("$", "ze"))
    // See myFixture.editor.settings.additionalColumnsCount
    assertVisibleLineBounds(0, 120, 199)
  }

  fun `test scroll caret column to last screen column with sidescrolloff containing an inline inlay`() {
    // The offset should include space for the inlay
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    val inlay = addInlay(101, true, 5)
    typeText(StringHelper.parseKeys("100|", "ze"))
    val availableColumns = getAvailableColumns(inlay)
    // Rightmost text column will still be the same, even if it's offset by an inlay
    // TODO: Should the offset include the visual column taken up by the inlay?
    // Note that the values for this test are -1 when compared to other tests. That's because the inlay takes up a
    // visual column, and scrolling doesn't distinguish the type of visual column
    // We need to decide if folds and/or inlays should be included in offsets, and figure out how to reasonably implement it
    assertVisibleLineBounds(0, 108 - availableColumns + 1, 108)
  }

  fun `test last screen column does not include previous inline inlay associated with preceding text`() {
    // The inlay is associated with the column before the caret, appears on the left of the caret, so does not affect
    // the last visible column
    configureByColumns(200)
    val inlay = addInlay(99, true, 5)
    typeText(StringHelper.parseKeys("100|", "ze"))
    val availableColumns = getAvailableColumns(inlay)
    assertVisibleLineBounds(0, 99 - availableColumns + 1, 99)
  }

  fun `test last screen column does not include previous inline inlay associated with following text`() {
    // The inlay is associated with the caret, but appears on the left, so does not affect the last visible column
    configureByColumns(200)
    val inlay = addInlay(99, false, 5)
    typeText(StringHelper.parseKeys("100|", "ze"))
    val availableColumns = getAvailableColumns(inlay)
    assertVisibleLineBounds(0, 99 - availableColumns + 1, 99)
  }

  fun `test last screen column includes subsequent inline inlay associated with preceding text`() {
    // The inlay is inserted after the caret and relates to the caret column. It should still be visible
    configureByColumns(200)
    val inlay = addInlay(100, true, 5)
    typeText(StringHelper.parseKeys("100|", "ze"))
    val visibleArea = myFixture.editor.scrollingModel.visibleArea
    val textWidth = visibleArea.width - inlay.widthInPixels
    val availableColumns = textWidth / EditorUtil.getPlainSpaceWidth(myFixture.editor)

    // The last visible text column will be 99, but it will be positioned before the inlay
    assertVisibleLineBounds(0, 99 - availableColumns + 1, 99)

    // We have to assert the location of the inlay
    Assert.assertEquals(visibleArea.x + textWidth, inlay.bounds!!.x)
    Assert.assertEquals(visibleArea.x + visibleArea.width, inlay.bounds!!.x + inlay.bounds!!.width)
  }

  fun `test last screen column does not include subsequent inline inlay associated with following text`() {
    // The inlay is inserted after the caret, and relates to text after the caret. It should not affect the last visible
    // column
    configureByColumns(200)
    addInlay(100, false, 5)
    typeText(StringHelper.parseKeys("100|", "ze"))
    assertVisibleLineBounds(0, 20, 99)
  }

  private fun getAvailableColumns(inlay: Inlay<*>): Int {
    val textWidth = myFixture.editor.scrollingModel.visibleArea.width - inlay.widthInPixels
    return textWidth / EditorUtil.getPlainSpaceWidth(myFixture.editor)
  }
}