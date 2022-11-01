/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.helper.vimLastColumn
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionUpActionTest : VimTestCase() {
  fun `test last column empty`() {
    val keys = "k"
    val before = """
            I found it in a legendary land
            all rocks and lave${c}nder and tufted grass,
    """.trimIndent()
    val after = """
            I found it in a le${c}gendary land
            all rocks and lavender and tufted grass,
    """.trimIndent()
    doTest(keys, before, after)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  fun `test last column is incorrect`() {
    val before = """
            I found it in a legendary land
            all rocks and lave${c}nder and tufted grass,
    """.trimIndent()
    val after = """
            I found it in a le${c}gendary land
            all rocks and lavender and tufted grass,
    """.trimIndent()
    doTest("k", before, after) {
      it.caretModel.primaryCaret.vimLastColumn = 5
    }
  }

  fun `test last column to shorter line`() {
    val keys = "kkkjjj"
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted ${c}grass,
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted ${c}grass,
    """.trimIndent()
    doTest(keys, before, after)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  fun `test last column wrong lastColumn`() {
    val before = """
            I found it in a legendary land
            all rocks and lavender and tufted ${c}grass,
    """.trimIndent()
    val after = """
            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
    """.trimIndent()
    doTest("k", before, after) {
      it.caretModel.primaryCaret.vimLastColumn = 0
    }
  }
}
