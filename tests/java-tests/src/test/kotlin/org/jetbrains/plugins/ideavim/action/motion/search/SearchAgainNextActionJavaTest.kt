/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.search

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.getVisualLineCount
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SearchAgainNextActionJavaTest : VimJavaTestCase() {
  @Test
  fun `test search unfolds text`() {
    configureByJavaText(
      """
      // My long comment that will be the longest line in the text
      /* 
      pu${c}pa
      */
    """.trimIndent()
    )

    ApplicationManager.getApplication().invokeAndWait {
      val foldingModel = fixture.editor.foldingModel
      foldingModel.runBatchFoldingOperation {
        val foldRegion = foldingModel.addFoldRegion(61, 71, "pupa")
        foldRegion!!.isExpanded = false
      }
      assertEquals(2, fixture.editor.vim.getVisualLineCount())
    }

    typeText("/pupa<CR>")
    ApplicationManager.getApplication().invokeAndWait {
      assertEquals(4, fixture.editor.vim.getVisualLineCount())
    }
  }
}
