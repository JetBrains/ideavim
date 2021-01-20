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
z<Left>      or                                        *zh* *z<Left>*
zh                      Move the view on the text [count] characters to the
                        left, thus scroll the text [count] characters to the
                        right.  This only works when 'wrap' is off.
 */
class ScrollColumnRightActionTest : VimTestCase() {
  fun `test scrolls column to right`() {
    configureByColumns(200)
    typeText(parseKeys("100|", "zh"))
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 58, 137)
  }

  fun `test scrolls column to right with zLeft`() {
    configureByColumns(200)
    typeText(parseKeys("100|", "z<Left>"))
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 58, 137)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at the end of line. IdeaVim will scroll up to length of longest line")
  fun `test scroll last column to right moves cursor 1`() {
    configureByColumns(200)
    typeText(parseKeys("$"))
    // Assert we got initial scroll correct
    // We'd need virtual space to scroll this. We're over 200 due to editor.settings.additionalColumnsCount
    assertVisibleLineBounds(0, 123, 202)

    typeText(parseKeys("zh"))
    assertPosition(0, 199)
    assertVisibleLineBounds(0, 122, 201)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at the end of line. IdeaVim will scroll up to length of longest line")
  fun `test scroll last column to right moves cursor 2`() {
    configureByText(buildString {
      repeat(300) { append("0") }
      appendln()
      repeat(200) { append("0") }
    })
    typeText(parseKeys("j$"))
    // Assert we got initial scroll correct
    // Note, this matches Vim - we've scrolled to centre (but only because the line above allows us to scroll without
    // virtual space)
    assertVisibleLineBounds(1, 159, 238)

    typeText(parseKeys("zh"))
    assertPosition(1, 199)
    assertVisibleLineBounds(1, 158, 237)
  }

  fun `test scrolls count columns to right`() {
    configureByColumns(200)
    typeText(parseKeys("100|", "10zh"))
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 49, 128)
  }

  fun `test scrolls count columns to right with zLeft`() {
    configureByColumns(200)
    typeText(parseKeys("100|", "10z<Left>"))
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 49, 128)
  }

  fun `test scrolls column to right with sidescrolloff moves cursor`() {
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    typeText(parseKeys("100|", "ze", "zh"))
    assertPosition(0, 98)
    assertVisibleLineBounds(0, 29, 108)
  }

  fun `test scroll column to right ignores sidescroll`() {
    OptionsManager.sidescroll.set(10)
    configureByColumns(200)
    typeText(parseKeys("100|"))
    // Assert we got initial scroll correct
    // sidescroll=10 means we don't get the sidescroll jump of half a screen and the cursor is positioned at the right edge
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 20, 99)

    typeText(parseKeys("zh")) // Moves cursor, but not by sidescroll jump
    assertPosition(0, 98)
    assertVisibleLineBounds(0, 19, 98)
  }

  fun `test scroll column to right on first page does nothing`() {
    configureByColumns(200)
    typeText(parseKeys("10|", "zh"))
    assertPosition(0, 9)
    assertVisibleLineBounds(0, 0, 79)
  }

  fun `test scroll column to right correctly scrolls inline inlay associated with preceding text`() {
    configureByColumns(200)
    addInlay(130, true, 5)
    typeText(parseKeys("100|"))
    // Text at end of line is:              89:inlay0123
    assertVisibleLineBounds(0, 59, 133) // 75 characters wide
    typeText(parseKeys("3zh"))  //    89:inlay0
    assertVisibleLineBounds(0, 56, 130) // 75 characters
    typeText(parseKeys("zh"))   //     89:inlay
    assertVisibleLineBounds(0, 55, 129) // 75 characters
    typeText(parseKeys("zh"))   //            8
    assertVisibleLineBounds(0, 49, 128) // 80 characters
  }

  fun `test scroll column to right correctly scrolls inline inlay associated with following text`() {
    configureByColumns(200)
    addInlay(130, false, 5)
    typeText(parseKeys("100|"))
    // Text at end of line is:              89inlay:0123
    assertVisibleLineBounds(0, 59, 133) // 75 characters wide
    typeText(parseKeys("3zh"))  //    89inlay:0
    assertVisibleLineBounds(0, 56, 130) // 75 characters
    typeText(parseKeys("zh"))   //           89
    assertVisibleLineBounds(0, 50, 129) // 80 characters
    typeText(parseKeys("zh"))   //            9
    assertVisibleLineBounds(0, 49, 128) // 80 characters
  }

  fun `test scroll column to right with preceding inline inlay moves cursor at end of screen`() {
    configureByColumns(200)
    addInlay(90, false, 5)
    typeText(parseKeys("100|", "ze", "zh"))
    assertPosition(0, 98)
    assertVisibleLineBounds(0, 24, 98)
    typeText(parseKeys("zh"))
    assertPosition(0, 97)
    assertVisibleLineBounds(0, 23, 97)
    typeText(parseKeys("zh"))
    assertPosition(0, 96)
    assertVisibleLineBounds(0, 22, 96)
    typeText(parseKeys("zh"))
    assertPosition(0, 95)
    assertVisibleLineBounds(0, 21, 95)
  }
}