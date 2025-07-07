/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.codeInsight.folding.impl.FoldingUtil
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

class VisualInsertActionJavaTest : VimJavaTestCase() {
  // VIM-1110 |CTRL-V| |v_b_i| |zc|
  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test block insert after folds`() {
    configureByJavaText(
      """$c/**
 * Something to fold.
 */
foo
bar
""",
    )

    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.foldingModel.runBatchFoldingOperation {
        CodeFoldingManager.getInstance(fixture.project).updateFoldRegions(fixture.editor)
        FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, 0)!!.isExpanded = false
      }
    }

    typeText(injector.parser.parseKeys("j" + "<C-V>" + "j" + "I" + "X" + "<Esc>"))
    assertState(
      """/**
 * Something to fold.
 */
${c}Xfoo
Xbar
""",
    )
  }
}