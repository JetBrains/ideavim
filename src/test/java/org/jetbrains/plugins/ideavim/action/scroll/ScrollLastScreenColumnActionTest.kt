/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.scroll

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Inlay
import com.maddyhome.idea.vim.helper.EditorHelper
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals

/*
                                                       *ze*
ze                      Scroll the text horizontally to position the cursor
                        at the end (right side) of the screen.  This only
                        works when 'wrap' is off.
 */
class ScrollLastScreenColumnActionTest : VimTestCase() {
  @Test
  fun `test scroll caret column to last screen column`() {
    configureByColumns(200)
    typeText("100|", "ze")
    assertVisibleLineBounds(0, 20, 99)
  }

  @Test
  fun `test scroll caret column to last screen column with sidescrolloff`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")
    typeText("100|", "ze")
    assertVisibleLineBounds(0, 30, 109)
  }

  @Test
  fun `test scroll at or near start of line does nothing`() {
    configureByColumns(200)
    typeText("10|", "ze")
    assertVisibleLineBounds(0, 0, 79)
  }

  @Test
  fun `test scroll end of line to last screen column`() {
    configureByColumns(200)
    typeText("$", "ze")
    assertVisibleLineBounds(0, 120, 199)
  }

  @Test
  fun `test scroll end of line to last screen column with sidescrolloff`() {
    configureByColumns(200)
    enterCommand("set scrolloff=10")
    typeText("$", "ze")
    // See myFixture.editor.settings.additionalColumnsCount
    assertVisibleLineBounds(0, 120, 199)
  }

  @Test
  fun `test scroll caret column to last screen column with sidescrolloff containing an inline inlay`() {
    // The offset should include space for the inlay
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")
    val inlay = addInlay(101, true, 5)
    typeText("100|", "ze")
    val availableColumns = getAvailableColumns(inlay)
    // Rightmost text column will still be the same, even if it's offset by an inlay
    // TODO: Should the offset include the visual column taken up by the inlay?
    // Note that the values for this test are -1 when compared to other tests. That's because the inlay takes up a
    // visual column, and scrolling doesn't distinguish the type of visual column
    // We need to decide if folds and/or inlays should be included in offsets, and figure out how to reasonably implement it
    assertVisibleLineBounds(0, 108 - availableColumns + 1, 108)
  }

  @Test
  fun `test last screen column does not include previous inline inlay associated with preceding text`() {
    // The inlay is associated with the column before the caret, appears on the left of the caret, so does not affect
    // the last visible column
    configureByColumns(200)
    val inlay = addInlay(99, true, 5)
    typeText("100|", "ze")
    val availableColumns = getAvailableColumns(inlay)
    assertVisibleLineBounds(0, 99 - availableColumns + 1, 99)
  }

  @Test
  fun `test last screen column does not include previous inline inlay associated with following text`() {
    // The inlay is associated with the caret, but appears on the left, so does not affect the last visible column
    configureByColumns(200)
    val inlay = addInlay(99, false, 5)
    typeText("100|", "ze")
    val availableColumns = getAvailableColumns(inlay)
    assertVisibleLineBounds(0, 99 - availableColumns + 1, 99)
  }

  @Test
  fun `test last screen column includes subsequent inline inlay associated with preceding text`() {
    // The inlay is inserted after the caret and relates to the caret column. It should still be visible
    configureByColumns(200)
    val inlay = addInlay(100, true, 5)
    typeText("100|", "ze")
    ApplicationManager.getApplication().invokeAndWait {
      val visibleArea = fixture.editor.scrollingModel.visibleArea
      val textWidth = visibleArea.width - inlay.widthInPixels
      val availableColumns = (textWidth / EditorHelper.getPlainSpaceWidthFloat(fixture.editor)).roundToInt()

      // The last visible text column will be 99, but it will be positioned before the inlay
      assertVisibleLineBounds(0, 99 - availableColumns + 1, 99)

      // We have to assert the location of the inlay
      val inlayX = fixture.editor.visualPositionToPoint2D(inlay.visualPosition).x.roundToInt()
      assertEquals(visibleArea.x + textWidth, inlayX)
      assertEquals(visibleArea.x + visibleArea.width, inlayX + inlay.widthInPixels)
    }
  }

  @Test
  fun `test last screen column does not include subsequent inline inlay associated with following text`() {
    // The inlay is inserted after the caret, and relates to text after the caret. It should not affect the last visible
    // column
    configureByColumns(200)
    addInlay(100, false, 5)
    typeText("100|", "ze")
    assertVisibleLineBounds(0, 20, 99)
  }

  private fun getAvailableColumns(inlay: Inlay<*>): Int {
    var res: Int? = null
    ApplicationManager.getApplication().invokeAndWait {
      val textWidth = fixture.editor.scrollingModel.visibleArea.width - inlay.widthInPixels
       res = (textWidth / EditorHelper.getPlainSpaceWidthFloat(fixture.editor)).roundToInt()
    }
    return res!!
  }
}
