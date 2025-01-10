/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf
import org.junit.jupiter.api.Test

class PutTextBeforeCursorActionTest : VimTestCase() {
  /**
   * @author Oskar Persson
   */
  @Test
  fun `test put visual text character to line twice with separate commands large P`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "Discovery",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "P"))
    typeText(injector.parser.parseKeys("V" + "P"))
    val after = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }
}
