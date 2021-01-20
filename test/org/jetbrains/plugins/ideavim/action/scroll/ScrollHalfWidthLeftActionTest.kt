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

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

/*
For the following four commands the cursor follows the screen.  If the
character that the cursor is on is moved off the screen, the cursor is moved
to the closest character that is on the screen.  The value of 'sidescroll' is
not used.

                                                       *zH*
zH                      Move the view on the text half a screenwidth to the
                        left, thus scroll the text half a screenwidth to the
                        right.  This only works when 'wrap' is off.

[count] is used but undocumented.
 */
class ScrollHalfWidthLeftActionTest : VimTestCase() {
  fun `test scroll half page width`() {
    configureByColumns(200)
    typeText(parseKeys("zL"))
    assertVisibleLineBounds(0, 40, 119)
  }

  fun `test scroll keeps cursor in place if already in scrolled area`() {
    configureByColumns(200)
    typeText(parseKeys("50|", "zL"))
    assertPosition(0, 49)
    assertVisibleLineBounds(0, 40, 119)
  }

  fun `test scroll moves cursor if moves off screen 1`() {
    configureByColumns(200)
    typeText(parseKeys("zL"))
    assertPosition(0, 40)
    assertVisibleLineBounds(0, 40, 119)
  }

  fun `test scroll moves cursor if moves off screen 2`() {
    configureByColumns(200)
    typeText(parseKeys("10|", "zL"))
    assertPosition(0, 40)
    assertVisibleLineBounds(0, 40, 119)
  }

  fun `test scroll count half page widths`() {
    configureByColumns(300)
    typeText(parseKeys("3zL"))
    assertPosition(0, 120)
    assertVisibleLineBounds(0, 120, 199)
  }

  fun `test scroll half page width with sidescrolloff`() {
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    typeText(parseKeys("zL"))
    assertPosition(0, 50)
    assertVisibleLineBounds(0, 40, 119)
  }

  fun `test scroll half page width ignores sidescroll`() {
    OptionsManager.sidescroll.set(10)
    configureByColumns(200)
    typeText(parseKeys("zL"))
    assertPosition(0, 40)
    assertVisibleLineBounds(0, 40, 119)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at end of line")
  fun `test scroll at end of line does not use full virtual space`() {
    configureByColumns(200)
    typeText(parseKeys("200|", "ze", "zL"))
    assertPosition(0, 199)
    assertVisibleLineBounds(0, 123, 202)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at end of line")
  fun `test scroll near end of line does not use full virtual space`() {
    configureByColumns(200)
    typeText(parseKeys("190|", "ze", "zL"))
    assertPosition(0, 189)
    assertVisibleLineBounds(0, 123, 202)
  }

  fun `test scroll includes inlay visual column in half page width`() {
    configureByColumns(200)
    addInlay(20, true, 5)
    typeText(parseKeys("zL"))
    // The inlay is included in the count of scrolled visual columns
    assertPosition(0, 39)
    assertVisibleLineBounds(0, 39, 118)
  }

  fun `test scroll with inlay in scrolled area and left of the cursor`() {
    configureByColumns(200)
    addInlay(20, true, 5)
    typeText(parseKeys("30|", "zL"))
    // The inlay is included in the count of scrolled visual columns
    assertPosition(0, 39)
    assertVisibleLineBounds(0, 39, 118)
  }
}