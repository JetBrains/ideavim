/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionUpActionTest : VimTestCase() {
  @Test
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

  @Test
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
  @Test
  fun `test caret moved outside of IdeaVim control`() {
    doTest(
      "k",
      """
        I found it in a legendary land
        all rock${c}s and lavender and tufted grass,
      """.trimIndent(),
      """
        I found it in a le${c}gendary land
        all rocks and lavender and tufted grass,
      """.trimIndent(),
    ) {
      // Simulate the caret being moved without IdeaVim knowing and therefore without vimLastColumn being updated
      // This offset is effectively "lave${c}nder"
      ApplicationManager.getApplication().invokeAndWait {
        it.caretModel.primaryCaret.moveToOffset(49)
      }
    }
  }
}
