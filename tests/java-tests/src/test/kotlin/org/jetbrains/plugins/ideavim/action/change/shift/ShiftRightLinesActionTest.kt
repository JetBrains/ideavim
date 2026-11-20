/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.shift

import com.intellij.application.options.CodeStyle
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

class ShiftRightLinesActionTest : VimJavaTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `add indent for java file`() {

    val before = """
      |public class C {
      |.I${c}nteger a;
      |}
    """.trimMargin().dotToTab()
    val after = """
      |public class C {
      |.....Integer a;
      |}
    """.trimMargin().dotToTab()

    usingTabs {
      configureByJavaText(before)
      typeText(">>".repeat(4))
      assertState(after)
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `repeated visual shift with tabs when editor tab size differs`() {
    val before = """
      |${c}ONE
      |TWO
      |THREE
    """.trimMargin()
    val after = """
      |...${c}ONE
      |...TWO
      |...THREE
    """.trimMargin().dotToTab()

    usingTabs {
      configureByJavaText(before)
      ApplicationManager.getApplication().invokeAndWait { fixture.editor.settings.setTabSize(3) }
      typeText("V2j>gv>gv>gv>gv>gv<gv>gv<gv<")
      assertState(after)
    }
  }

  private fun usingTabs(action: () -> Unit) {
    val testSettings = CodeStyle.createTestSettings()
    val javaSettings = testSettings.getIndentOptions(JavaFileType.INSTANCE)
    javaSettings.USE_TAB_CHARACTER = true
    CodeStyle.setTemporarySettings(fixture.project, testSettings)
    try {
      action()
    } finally {
      CodeStyle.dropTemporarySettings(fixture.project)
    }
  }
}