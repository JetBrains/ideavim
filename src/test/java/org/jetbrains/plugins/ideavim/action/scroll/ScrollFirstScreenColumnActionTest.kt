/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.Assert
import kotlin.math.roundToInt

/*
                                                       *zs*
zs                      Scroll the text horizontally to position the cursor
                        at the start (left side) of the screen.  This only
                        works when 'wrap' is off.
 */
class ScrollFirstScreenColumnActionTest : VimTestCase() {
  fun `test scroll caret column to first screen column`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|" + "zs"))
    assertVisibleLineBounds(0, 99, 178)
  }

  fun `test scroll caret column to first screen column with sidescrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrolloffName, VimInt(10))
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|" + "zs"))
    assertVisibleLineBounds(0, 89, 168)
  }

  fun `test scroll at or near start of line`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("5|" + "zs"))
    assertVisibleLineBounds(0, 4, 83)
  }

  fun `test scroll at or near start of line with sidescrolloff does nothing`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrolloffName, VimInt(10))
    configureByColumns(200)
    typeText(injector.parser.parseKeys("5|" + "zs"))
    assertVisibleLineBounds(0, 0, 79)
  }

  @VimBehaviorDiffers(description = "Vim scrolls caret to first screen column, filling with virtual space")
  fun `test scroll end of line to first screen column`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("$" + "zs"))
    // See also editor.settings.isVirtualSpace and editor.settings.additionalColumnsCount
    assertVisibleLineBounds(0, 123, 202)
  }

  fun `test first screen column includes previous inline inlay associated with following text`() {
    // The inlay is associated with the caret, on the left, so should appear before it when scrolling columns
    configureByColumns(200)
    val inlay = addInlay(99, false, 5)
    typeText(injector.parser.parseKeys("100|" + "zs"))
    val visibleArea = myFixture.editor.scrollingModel.visibleArea
    val textWidth = visibleArea.width - inlay.widthInPixels
    val availableColumns = (textWidth / EditorHelper.getPlainSpaceWidthFloat(myFixture.editor)).roundToInt()

    // The first visible text column will be 99, with the inlay positioned to the left of it
    assertVisibleLineBounds(0, 99, 99 + availableColumns - 1)
    Assert.assertEquals(visibleArea.x, inlay.bounds!!.x)
  }

  fun `test first screen column does not include previous inline inlay associated with preceding text`() {
    // The inlay is associated with the column before the caret, so should not affect scrolling
    configureByColumns(200)
    addInlay(99, true, 5)
    typeText(injector.parser.parseKeys("100|" + "zs"))
    assertVisibleLineBounds(0, 99, 178)
  }

  fun `test first screen column does not include subsequent inline inlay associated with following text`() {
    // The inlay is associated with the column after the caret, so should not affect scrolling
    configureByColumns(200)
    val inlay = addInlay(100, false, 5)
    typeText(injector.parser.parseKeys("100|" + "zs"))
    val availableColumns = getAvailableColumns(inlay)
    assertVisibleLineBounds(0, 99, 99 + availableColumns - 1)
  }

  fun `test first screen column does not include subsequent inline inlay associated with preceding text`() {
    // The inlay is associated with the caret column, but appears to the right of the column, so does not affect scrolling
    configureByColumns(200)
    val inlay = addInlay(100, true, 5)
    typeText(injector.parser.parseKeys("100|" + "zs"))
    val availableColumns = getAvailableColumns(inlay)
    assertVisibleLineBounds(0, 99, 99 + availableColumns - 1)
  }

  private fun getAvailableColumns(inlay: Inlay<*>): Int {
    val textWidth = myFixture.editor.scrollingModel.visibleArea.width - inlay.widthInPixels
    return (textWidth / EditorHelper.getPlainSpaceWidthFloat(myFixture.editor)).roundToInt()
  }
}
