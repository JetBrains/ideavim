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

class PutReformatOnPasteTest : VimJavaTestCase() {
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
  fun `test linewise put does not reindent when reformat on paste is disabled`() {
    withReformatOnPaste(CodeInsightSettings.NO_REFORMAT) {
      configureByJavaText(
        """class C {
    int a;
<caret>int b;
    int c;
}
""",
      )
      typeText("yyp")
      assertState(
        """class C {
    int a;
int b;
<caret>int b;
    int c;
}
""",
      )
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test linewise put still reindents when reformat on paste is enabled`() {
    withReformatOnPaste(CodeInsightSettings.INDENT_EACH_LINE) {
      configureByJavaText(
        """class C {
    int a;
<caret>int b;
    int c;
}
""",
      )
      typeText("yyp")
      assertState(
        """class C {
    int a;
int b;
    <caret>int b;
    int c;
}
""",
      )
    }
  }
}
