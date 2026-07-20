/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

@Suppress("SpellCheckingInspection")
class LoadKeymapCommandTest : VimTestCase() {
  @TempDir
  var tempDir: Path? = null

  private fun sourceScript(content: String) {
    val file = tempDir!!.resolve("keymap.vim")
    file.writeText(content)
    // indicateErrors = true so exceptions thrown while sourcing surface via injector.messages
    // (and are visible to assertPluginError), instead of only being logged.
    injector.vimscriptExecutor.executeFile(file, fixture.editor.vim, false, indicateErrors = true)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap creates language mappings from the table`() {
    configureByText("\n")
    sourceScript(
      """
        loadkeymap
        a x
        b y
      """.trimIndent(),
    )
    assertMappingExists("a", "x", MappingMode.L)
    assertMappingExists("b", "y", MappingMode.L)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap skips comment and blank lines`() {
    configureByText("\n")
    sourceScript(
      """
        loadkeymap
        " this is a comment
        a x

        b y
      """.trimIndent(),
    )
    assertMappingExists("a", "x", MappingMode.L)
    assertMappingExists("b", "y", MappingMode.L)
    assertNoMapping("\"", MappingMode.L)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap ignores trailing description column`() {
    configureByText("\n")
    sourceScript(
      """
        loadkeymap
        a x CYRILLIC SMALL LETTER
      """.trimIndent(),
    )
    // The mapping is 'a' -> 'x', not 'a' -> 'x CYRILLIC SMALL LETTER'.
    assertMappingExists("a", "x", MappingMode.L)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap only applies to typed text when iminsert is on`() {
    configureByText("${c}\n")
    sourceScript(
      """
        loadkeymap
        q й
      """.trimIndent(),
    )
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("q")
    assertState("й\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap parses tab-separated pairs`() {
    configureByText("${c}\n")
    sourceScript("loadkeymap\nq\tй\n")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("q")
    assertState("й\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap with tab separator drops the description column`() {
    configureByText("${c}\n")
    // Exactly the shape of runtime/keymap/russian-jcukenwin.vim lines.
    sourceScript("loadkeymap\nq\tй\tCYRILLIC SMALL LETTER SHORT I\n")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("q")
    // Only 'й' is inserted — not the description text.
    assertState("й\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap drops the description column when typed`() {
    configureByText("${c}\n")
    sourceScript(
      """
        loadkeymap
        a x DESCRIPTION
      """.trimIndent(),
    )
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("a")
    // 'a' maps to 'x', not to "x DESCRIPTION".
    assertState("x\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap skips whitespace-only lines`() {
    configureByText("\n")
    sourceScript("loadkeymap\na x\n   \nb y\n")
    assertPluginError(false)
    assertMappingExists("a", "x", MappingMode.L)
    assertMappingExists("b", "y", MappingMode.L)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap outside a sourced file reports E105`() {
    configureByText("\n")
    enterCommand("loadkeymap")
    assertPluginError(true)
    assertPluginErrorMessage("E105: Using :loadkeymap not in a sourced file")
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test loadkeymap reports E791 for an entry with a missing to value`() {
    configureByText("\n")
    sourceScript("loadkeymap\nq\n")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: q")
  }
}
