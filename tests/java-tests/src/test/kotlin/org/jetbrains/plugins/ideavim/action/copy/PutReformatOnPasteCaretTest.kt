/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.codeInsight.CodeInsightSettings
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

/**
 * `gp` leaves the caret just after the pasted text. Where it lands must not depend on the IDE's "Reformat on paste"
 * setting - that setting decides whether the pasted lines are reindented, not where the caret ends up.
 */
class PutReformatOnPasteCaretTest : VimJavaTestCase() {
  private fun withReformatOnPaste(value: Int, block: () -> Unit) {
    val settings = CodeInsightSettings.getInstance()
    val original = settings.REFORMAT_ON_PASTE
    settings.REFORMAT_ON_PASTE = value
    try {
      block()
    } finally {
      settings.REFORMAT_ON_PASTE = original
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test gp leaves caret at start of following line when reformat on paste is disabled`() {
    withReformatOnPaste(CodeInsightSettings.NO_REFORMAT) {
      configureByText(
        """${c}one
          |two
        """.trimMargin(),
      )
      typeText("yygp")
      assertState(
        """one
          |one
          |${c}two
        """.trimMargin(),
      )
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test gp leaves caret at start of following line when reformat on paste is enabled`() {
    withReformatOnPaste(CodeInsightSettings.INDENT_EACH_LINE) {
      configureByText(
        """${c}one
          |two
        """.trimMargin(),
      )
      typeText("yygp")
      assertState(
        """one
          |one
          |${c}two
        """.trimMargin(),
      )
    }
  }
}
