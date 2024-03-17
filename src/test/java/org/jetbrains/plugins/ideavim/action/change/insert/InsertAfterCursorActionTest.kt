/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertAfterCursorActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @Test
  fun `test insert after cursor with inlay with preceding text places caret between inlay and preceding text`() {
    configureByText("I found it i${c}n a legendary land")
    // Inlay is at vp 13. Preceding text is at 12. Caret should be between preceding and inlay = 13
    // I found it in|:<inlay> a legendary land
    addInlay(13, true, 5)
    typeText(injector.parser.parseKeys("a"))
    assertVisualPosition(0, 13)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @Test
  fun `test insert after cursor with inlay with following text places caret between inlay and following text`() {
    configureByText("I found it$c in a legendary land")
    // Inlay is at offset 11, following text is at vp 12
    // I found it <inlay>:|in a legendary land
    addInlay(11, false, 5)
    typeText(injector.parser.parseKeys("a"))
    assertVisualPosition(0, 12)
  }
}
