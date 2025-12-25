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
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for VIM-2656: Incorrect shift motion behavior with tabs
 */
class ShiftRightTabsTest : VimJavaTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test shift right with tabs should add full tab`() {
    val before = """
      |public class C {
      |.${c}println("hello");
      |}
    """.trimMargin().dotToTab()

    val after = """
      |public class C {
      |..println("hello");
      |}
    """.trimMargin().dotToTab()

    usingTabs {
      configureByJavaText(before)
      typeText(">>")
      assertState(after)
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test shift right with tabs from no indent`() {
    val before = """
      |public class C {
      |${c}println("hello");
      |}
    """.trimMargin()

    val after = """
      |public class C {
      |.println("hello");
      |}
    """.trimMargin().dotToTab()

    usingTabs {
      configureByJavaText(before)
      typeText(">>")
      assertState(after)
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test shift right multiple times with tabs`() {
    val before = """
      |public class C {
      |${c}println("hello");
      |}
    """.trimMargin()

    val after = """
      |public class C {
      |...println("hello");
      |}
    """.trimMargin().dotToTab()

    usingTabs {
      configureByJavaText(before)
      typeText(">>".repeat(3))
      assertState(after)
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test shift right with existing misaligned spaces should use full tabs`() {
    // VIM-2656: Starting with 2 spaces, >> should result in full tab indent, not mixed
    val before = """
      |public class C {
      |  ${c}println("hello");
      |}
    """.trimMargin()

    val after = """
      |public class C {
      |.println("hello");
      |}
    """.trimMargin().dotToTab()

    usingTabs {
      configureByJavaText(before)
      typeText(">>")
      assertState(after)
    }
  }

  private fun usingTabs(action: () -> Unit) {
    val testSettings = CodeStyle.createTestSettings()
    val javaSettings = testSettings.getIndentOptions(JavaFileType.INSTANCE)
    javaSettings.USE_TAB_CHARACTER = true
    javaSettings.TAB_SIZE = 4
    javaSettings.INDENT_SIZE = 4
    CodeStyle.setTemporarySettings(fixture.project, testSettings)
    try {
      action()
    } finally {
      CodeStyle.dropTemporarySettings(fixture.project)
    }
  }
}
