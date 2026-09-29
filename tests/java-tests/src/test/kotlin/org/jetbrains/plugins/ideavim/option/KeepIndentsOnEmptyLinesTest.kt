/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.application.options.CodeStyle
import com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for "Keep indents on empty lines" in Settings | Editor | Code Style.
 *
 * IdeaVim only deletes an untouched indent for `cc` and `S`; `o`, `O` and Enter keep theirs whatever the setting says
 * (see [AutoIndentOptionJavaTest]). This setting is therefore what decides the `cc` and `S` case: with it on, the
 * emptied line keeps its indent instead of being left completely empty as Vim leaves it. It is off by default.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.IDEAVIM_WORKS_INTENTIONALLY_DIFFERENT,
  description = "Vim always deletes the indent, the IDE setting asks for it to be kept",
)
class KeepIndentsOnEmptyLinesTest : VimJavaTestCase() {

  private val indent = " ".repeat(8)

  private fun configureIndentedStatement() {
    configureByJavaText(
      """
            class Main {
                void f() {
                    int ${c}x = 1;
                }
            }
      """.trimIndent(),
    )
  }

  private fun typeWithKeepIndentsOnEmptyLines(value: Boolean, keys: String) {
    val indentOptions = CodeStyle.getSettings(fixture.project).getIndentOptions(JavaFileType.INSTANCE)
    val old = indentOptions.KEEP_INDENTS_ON_EMPTY_LINES
    indentOptions.KEEP_INDENTS_ON_EMPTY_LINES = value
    try {
      typeText(keys)
    } finally {
      indentOptions.KEEP_INDENTS_ON_EMPTY_LINES = old
    }
  }

  /**
   * The lines are joined by hand rather than written as a raw string, because a kept indent is trailing white space,
   * which an editor may silently strip from the expected text.
   */
  private fun codeWithEmptiedLine(line: String) = mutableListOf(
    "class Main {", "    void f() {", line, "    }", "}",
  ).joinToString("\n")

  private fun assertEmptiedLineKeptItsIndent() {
    assertState(codeWithEmptiedLine(indent))
    assertPosition(2, indent.length - 1)
  }

  @Test
  fun `test cc and immediate escape keeps the indent when the IDE is asked to`() {
    configureIndentedStatement()
    typeWithKeepIndentsOnEmptyLines(true, "cc<Esc>")
    assertEmptiedLineKeptItsIndent()
  }

  @Test
  fun `test S and immediate escape keeps the indent when the IDE is asked to`() {
    configureIndentedStatement()
    typeWithKeepIndentsOnEmptyLines(true, "S<Esc>")
    assertEmptiedLineKeptItsIndent()
  }

  @Test
  fun `test cc and immediate escape leaves an empty line when the setting is off`() {
    configureIndentedStatement()
    typeWithKeepIndentsOnEmptyLines(false, "cc<Esc>")
    assertState(codeWithEmptiedLine("$c"))
  }
}
