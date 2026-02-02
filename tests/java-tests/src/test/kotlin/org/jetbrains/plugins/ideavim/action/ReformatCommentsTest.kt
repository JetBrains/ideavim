/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "IdeaVim applies IDE code formatting before textwidth-based wrapping, and uses custom comment-aware wrapping logic",
)
class ReformatCommentsTest : VimTestCase() {

  @Test
  fun testGqWrapsLongJavaLineComment() {
    configureByText(
      JavaFileType.INSTANCE,
      """
      class Test {
          // ${c}This is a very long comment that should be wrapped
          void method() {}
      }
      """.trimIndent(),
    )
    enterCommand("set textwidth=35")
    typeText(injector.parser.parseKeys("gqq"))
    assertState(
      """
      class Test {
          ${c}// This is a very long comment
          // that should be wrapped
          void method() {}
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testGqWrapsLongJavaBlockComment() {
    configureByText(
      JavaFileType.INSTANCE,
      """
      class Test {
          /* ${c}This is a very long block comment that should be wrapped by the formatter */
          void method() {}
      }
      """.trimIndent(),
    )
    enterCommand("set textwidth=50")
    typeText(injector.parser.parseKeys("gqq"))
    assertState(
      """
      class Test {
          ${c}/* This is a very long block comment that
           * should be wrapped by the formatter */
          void method() {}
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testGqWrapsJavaDocComment() {
    configureByText(
      JavaFileType.INSTANCE,
      """
      class Test {
          /**
           * ${c}This is a very long JavaDoc comment that should be wrapped when it exceeds previously defined textwidth
           */
          void method() {}
      }
      """.trimIndent(),
    )
    enterCommand("set textwidth=50")
    typeText(injector.parser.parseKeys("gqj"))
    assertState(
      """
      class Test {
          /**
           ${c}* This is a very long JavaDoc comment that
           * should be wrapped when it exceeds
           * previously defined textwidth
           */
          void method() {}
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testGqWrapsLongPlainTextLine() {
    configureByText(
      PlainTextFileType.INSTANCE,
      "${c}This is a very long line of plain text that should be wrapped when it exceeds the textwidth setting",
    )
    enterCommand("set textwidth=40")
    typeText(injector.parser.parseKeys("gqq"))
    assertState(
      """
      ${c}This is a very long line of plain text
      that should be wrapped when it exceeds
      the textwidth setting
      """.trimIndent(),
    )
  }

  @Test
  fun testGwWrapsLongPlainTextLinePreservingCursor() {
    configureByText(
      PlainTextFileType.INSTANCE,
      "This is a very long line of ${c}plain text that should be wrapped when it exceeds the textwidth setting",
    )
    enterCommand("set textwidth=40")
    typeText(injector.parser.parseKeys("gww"))
    assertState(
      """
      This is a very long line of ${c}plain text
      that should be wrapped when it exceeds
      the textwidth setting
      """.trimIndent(),
    )
  }

  @Test
  fun testGqWrapsMultipleLongLines() {
    configureByText(
      PlainTextFileType.INSTANCE,
      """
      ${c}This is the first very long line that needs wrapping to fit within textwidth.
      This is the second very long line that also needs wrapping to fit within textwidth.
      """.trimIndent(),
    )
    enterCommand("set textwidth=40")
    typeText(injector.parser.parseKeys("gqj"))
    assertState(
      """
      ${c}This is the first very long line that
      needs wrapping to fit within textwidth.
      This is the second very long line that
      also needs wrapping to fit within
      textwidth.
      """.trimIndent(),
    )
  }

  @Test
  fun testGqVisualWrapsSelectedText() {
    configureByText(
      PlainTextFileType.INSTANCE,
      """
      ${c}This is a very long line that should be wrapped when formatted with gq in visual mode.
      This line should not be affected.
      """.trimIndent(),
    )
    enterCommand("set textwidth=40")
    typeText(injector.parser.parseKeys("Vgq"))
    assertState(
      """
      ${c}This is a very long line that should be
      wrapped when formatted with gq in visual
      mode.
      This line should not be affected.
      """.trimIndent(),
    )
  }

  @Test
  fun testGqWrapsLongMarkdownParagraph() {
    configureByTextX(
      "test.md",
      """
      # Header

      ${c}This is a very long paragraph in a markdown file that should be wrapped when it exceeds the textwidth setting.

      Another paragraph.
      """.trimIndent(),
    )
    enterCommand("set textwidth=50")
    typeText(injector.parser.parseKeys("gqq"))
    assertState(
      """
      # Header

      ${c}This is a very long paragraph in a markdown file
      that should be wrapped when it exceeds the
      textwidth setting.

      Another paragraph.
      """.trimIndent(),
    )
  }

  @Test
  fun testGwWrapsMarkdownPreservingCursor() {
    configureByTextX(
      "test.md",
      """
      This is a very long ${c}markdown line that should be wrapped but cursor position should be preserved.
      """.trimIndent(),
    )
    enterCommand("set textwidth=40")
    typeText(injector.parser.parseKeys("gww"))
    assertState(
      """
      This is a very long ${c}markdown line that
      should be wrapped but cursor position
      should be preserved.
      """.trimIndent(),
    )
  }
}
