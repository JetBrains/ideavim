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
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals

/*
                                                       *zs*
zs                      Scroll the text horizontally to position the cursor
                        at the start (left side) of the screen.  This only
                        works when 'wrap' is off.
 */
class ScrollFirstScreenColumnActionTest : VimTestCase() {
  @Test
  fun `test scroll caret column to first screen column`() {
    configureByColumns(200)
    typeText("100|", "zs")
    assertVisibleLineBounds(0, 99, 178)
  }

  @Test
  fun `test scroll caret column to first screen column with sidescrolloff`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")
    typeText("100|", "zs")
    assertVisibleLineBounds(0, 89, 168)
  }

  @Test
  fun `test scroll at or near start of line`() {
    configureByColumns(200)
    typeText("5|", "zs")
    assertVisibleLineBounds(0, 4, 83)
  }

  @Test
  fun `test scroll at or near start of line with sidescrolloff does nothing`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")
    typeText("5|", "zs")
    assertVisibleLineBounds(0, 0, 79)
  }

  @VimBehaviorDiffers(description = "Vim scrolls caret to first screen column, filling with virtual space")
  @Test
  fun `test scroll end of line to first screen column`() {
    configureByColumns(200)
    typeText("$", "zs")
    // See also editor.settings.isVirtualSpace and editor.settings.additionalColumnsCount
    assertVisibleLineBounds(0, 123, 202)
  }

  @Test
  fun `test first screen column includes previous inline inlay associated with following text`() {
    // The inlay is associated with the caret, on the left, so should appear before it when scrolling columns
    configureByColumns(200)
    val inlay = addInlay(99, false, 5)
    typeText("100|", "zs")
    ApplicationManager.getApplication().invokeAndWait {
      val visibleArea = fixture.editor.scrollingModel.visibleArea
      val textWidth = visibleArea.width - inlay.widthInPixels
      val availableColumns = (textWidth / EditorHelper.getPlainSpaceWidthFloat(fixture.editor)).roundToInt()

      // The first visible text column will be 99, with the inlay positioned to the left of it
      assertVisibleLineBounds(0, 99, 99 + availableColumns - 1)
      assertEquals(visibleArea.x, inlay.bounds!!.x)
    }
  }

  @Test
  fun `test first screen column does not include previous inline inlay associated with preceding text`() {
    // The inlay is associated with the column before the caret, so should not affect scrolling
    configureByColumns(200)
    addInlay(99, true, 5)
    typeText("100|", "zs")
    assertVisibleLineBounds(0, 99, 178)
  }

  @Test
  fun `test first screen column does not include subsequent inline inlay associated with following text`() {
    // The inlay is associated with the column after the caret, so should not affect scrolling
    configureByColumns(200)
    val inlay = addInlay(100, false, 5)
    typeText("100|", "zs")
    ApplicationManager.getApplication().invokeAndWait {
      val availableColumns = getAvailableColumns(inlay)
      assertVisibleLineBounds(0, 99, 99 + availableColumns - 1)
    }
  }

  @Test
  fun `test first screen column does not include subsequent inline inlay associated with preceding text`() {
    // The inlay is associated with the caret column, but appears to the right of the column, so does not affect scrolling
    configureByColumns(200)
    val inlay = addInlay(100, true, 5)
    typeText("100|", "zs")
    ApplicationManager.getApplication().invokeAndWait {
      val availableColumns = getAvailableColumns(inlay)
      assertVisibleLineBounds(0, 99, 99 + availableColumns - 1)
    }
  }

  private fun getAvailableColumns(inlay: Inlay<*>): Int {
    val textWidth = fixture.editor.scrollingModel.visibleArea.width - inlay.widthInPixels
    return (textWidth / EditorHelper.getPlainSpaceWidthFloat(fixture.editor)).roundToInt()
  }
}
