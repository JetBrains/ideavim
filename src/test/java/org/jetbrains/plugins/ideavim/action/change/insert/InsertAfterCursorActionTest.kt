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

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertAfterCursorActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  fun `test insert after cursor with inlay with preceding text places caret between inlay and preceding text`() {
    configureByText("I found it i${c}n a legendary land")
    // Inlay is at vp 13. Preceding text is at 12. Caret should be between preceding and inlay = 13
    // I found it in|:<inlay> a legendary land
    addInlay(13, true, 5)
    typeText(parseKeys("a"))
    assertVisualPosition(0, 13)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  fun `test insert after cursor with inlay with following text places caret between inlay and following text`() {
    configureByText("I found it$c in a legendary land")
    // Inlay is at offset 11, following text is at vp 12
    // I found it <inlay>:|in a legendary land
    addInlay(11, false, 5)
    typeText(parseKeys("a"))
    assertVisualPosition(0, 12)
  }
}
