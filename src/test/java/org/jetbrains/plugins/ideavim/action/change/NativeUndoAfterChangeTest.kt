/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import com.intellij.idea.TestFor
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Native IDE undo (Ctrl+Z, the `$Undo` action) after a `S`/`C` change must restore the original content.
 *
 * Regression from VIM-4245, which normalized carets after every non-IdeaVim IDE action. That normalization ran in
 * `afterActionPerformed` of the undo action itself and moved the caret off the position recorded with the edit. The
 * platform undo treats caret movement as a separate undo step by default, so every Ctrl+Z was consumed restoring the
 * caret and the text was never reverted (VIM-4287).
 */
class NativeUndoAfterChangeTest : VimTestCase() {
  @Test
  @TestFor(issues = ["VIM-4287"])
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun `native undo after S restores original line`() {
    configureByText("Lorem ${c}ipsum dolor sit amet")
    typeText("S", "replacement", "<Esc>")
    assertState("replacemen${c}t")

    performNativeUndo("Lorem ipsum dolor sit amet")

    assertState("Lorem ${c}ipsum dolor sit amet")
  }

  @Test
  @TestFor(issues = ["VIM-4287"])
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun `native undo after C restores original text to end of line`() {
    configureByText("Lorem ${c}ipsum dolor sit amet")
    typeText("C", "replacement", "<Esc>")
    assertState("Lorem replacemen${c}t")

    performNativeUndo("Lorem ipsum dolor sit amet")

    assertState("Lorem ${c}ipsum dolor sit amet")
  }

  /**
   * Presses native undo (`$Undo`) until the document is restored to [expected], dispatched through the action system so
   * [com.maddyhome.idea.vim.listener.IdeaSpecifics.VimActionListener] fires, exactly like a real Ctrl+Z. The guard
   * bounds the loop: with the regression present the text is never reverted, so the loop must terminate on its own and
   * let the assertion fail rather than hang. It also stops as soon as the text matches, so it never over-undoes into
   * the file-setup commands.
   */
  private fun performNativeUndo(expected: String) {
    ApplicationManager.getApplication().invokeAndWait {
      var guard = 0
      while (fixture.editor.document.text != expected && guard++ < 20) {
        fixture.performEditorAction(IdeActions.ACTION_UNDO)
      }
    }
  }
}
