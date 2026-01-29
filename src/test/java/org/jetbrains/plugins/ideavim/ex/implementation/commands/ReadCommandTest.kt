/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File

class ReadCommandTest : VimTestCase() {

  private lateinit var testFile: File
  private lateinit var testFilePath: String

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    // Create a temporary file for testing
    testFile = File.createTempFile("ideavim_read_test", ".txt")
    testFile.deleteOnExit()
    testFilePath = testFile.absolutePath
  }

  // ============ Basic :read functionality ============

  @Test
  fun `test read file inserts content below current line`() {
    testFile.writeText("inserted line")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    assertState(
      """
      line 1
      line 2
      ${c}inserted line
      line 3
    """.trimIndent()
    )
  }

  @Test
  fun `test read file with multiple lines`() {
    testFile.writeText("inserted line 1\ninserted line 2\ninserted line 3")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    assertState(
      """
      line 1
      line 2
      ${c}inserted line 1
      inserted line 2
      inserted line 3
      line 3
    """.trimIndent()
    )
  }

  @Test
  fun `test read empty file`() {
    testFile.writeText("")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    // Empty file should not change the buffer
    assertState(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
  }

  // ============ Line address tests ============

  @Test
  fun `test read with line 0 inserts at top of file`() {
    testFile.writeText("inserted line")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("0read $testFilePath")
    assertState(
      """
      line 1
      ${c}inserted line
      line 2
      line 3
    """.trimIndent()
    )
  }

  @Test
  fun `test read with dollar sign inserts at end of file`() {
    testFile.writeText("inserted line")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("${'$'}read $testFilePath")
    assertState(
      """
      line 1
      line 2
      line 3
      ${c}inserted line
      
    """.trimIndent()
    )
  }

  @Test
  fun `test read with specific line number`() {
    testFile.writeText("inserted line")
    configureByText(
      """
      line 1
      line 2
      line ${c}3
      line 4
    """.trimIndent()
    )
    enterCommand("1read $testFilePath")
    assertState(
      """
      line 1
      ${c}inserted line
      line 2
      line 3
      line 4
    """.trimIndent()
    )
  }

  @Test
  fun `test read with last line number`() {
    testFile.writeText("inserted line")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("3read $testFilePath")
    assertState(
      """
      line 1
      line 2
      line 3
      ${c}inserted line
      
    """.trimIndent()
    )
  }

  // ============ Cursor position tests ============

  @Test
  fun `test cursor moves to first inserted line`() {
    testFile.writeText("first inserted\nsecond inserted")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    // Cursor should be on the first character of the first inserted line
    assertState(
      """
      line 1
      line 2
      ${c}first inserted
      second inserted
      line 3
    """.trimIndent()
    )
  }

  @Test
  fun `test cursor position after read at line 0`() {
    testFile.writeText("inserted content")
    configureByText(
      """
      line ${c}1
      line 2
    """.trimIndent()
    )
    enterCommand("0read $testFilePath")
    assertState(
      """
      line 1
      ${c}inserted content
      line 2
    """.trimIndent()
    )
  }

  // ============ Error handling tests ============

  @Test
  fun `test read nonexistent file shows error`() {
    configureByText("line ${c}1")
    enterCommand("read /nonexistent/file/path/that/does/not/exist.txt")
    assertPluginError(true)
  }

  @Test
  fun `test read with no filename shows error`() {
    configureByText("line ${c}1")
    enterCommand("read")
    assertPluginError(true)
  }

  // ============ Undo tests ============

  @Test
  fun `test read can be undone`() {
    testFile.writeText("inserted line")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    assertState(
      """
      line 1
      line 2
      ${c}inserted line
      line 3
    """.trimIndent()
    )

    typeText("u")
    assertState(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
  }

  @Test
  fun `test read multiple lines can be undone in single step`() {
    testFile.writeText("inserted 1\ninserted 2\ninserted 3")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    assertState(
      """
      line 1
      line 2
      ${c}inserted 1
      inserted 2
      inserted 3
      line 3
    """.trimIndent()
    )

    typeText("u")
    assertState(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
  }

  // ============ Edge cases ============

  @Test
  fun `test read into empty buffer`() {
    testFile.writeText("inserted line")
    configureByText("$c")
    enterCommand("read $testFilePath")
    assertState(
      """
      ${c}inserted line
      
    """.trimIndent()
    )
  }

  @Test
  fun `test read file with trailing newline`() {
    testFile.writeText("inserted line\n")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    // Vim typically strips the trailing newline when reading
    assertState(
      """
      line 1
      line 2
      ${c}inserted line
      line 3
    """.trimIndent()
    )
  }

  @Test
  fun `test read file with only newlines`() {
    testFile.writeText("\n\n")
    configureByText(
      """
      line 1
      line ${c}2
      line 3
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    assertState(
      """
      line 1
      line 2
      $c

      line 3
    """.trimIndent()
    )
  }

  @Test
  fun `test read preserves indentation from file`() {
    testFile.writeText("    indented line\n\tTabbed line")
    configureByText(
      """
      line ${c}1
      line 2
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    assertState(
      """
      line 1
          ${c}indented line
      	Tabbed line
      line 2
    """.trimIndent()
    )
  }

  @Test
  fun `test read at end of buffer without trailing newline`() {
    testFile.writeText("inserted line")
    configureByText(
      """
      line 1
      line ${c}2
    """.trimIndent()
    )
    enterCommand("${'$'}read $testFilePath")
    assertState(
      """
      line 1
      line 2
      ${c}inserted line
      
    """.trimIndent()
    )
  }

  @Test
  fun `test read single line file`() {
    testFile.writeText("single line")
    configureByText(
      """
      first
      ${c}second
      third
    """.trimIndent()
    )
    enterCommand("read $testFilePath")
    assertState(
      """
      first
      second
      ${c}single line
      third
    """.trimIndent()
    )
  }

  // ============ Command abbreviations ============

  @Test
  fun `test r abbreviation works`() {
    testFile.writeText("inserted line")
    configureByText(
      """
      line ${c}1
      line 2
    """.trimIndent()
    )
    enterCommand("r $testFilePath")
    assertState(
      """
      line 1
      ${c}inserted line
      line 2
    """.trimIndent()
    )
  }

  // ============ Path handling ============

  @Test
  fun `test read with absolute path`() {
    testFile.writeText("absolute path content")
    configureByText("line ${c}1")
    enterCommand("read ${testFile.absolutePath}")
    assertState(
      """
      line 1
      ${c}absolute path content
      
    """.trimIndent()
    )
  }

  // ============ Special characters in content ============

  @Test
  fun `test read file with special characters`() {
    testFile.writeText("special chars: <>&\"'`$\\")
    configureByText("line ${c}1")
    enterCommand("read $testFilePath")
    assertState(
      """
      line 1
      ${c}special chars: <>&"'`$\
      
    """.trimIndent()
    )
  }

  @Test
  fun `test read file with unicode content`() {
    testFile.writeText("Unicode: 你好世界 🎉 émojis")
    configureByText("line ${c}1")
    enterCommand("read $testFilePath")
    assertState(
      """
      line 1
      ${c}Unicode: 你好世界 🎉 émojis
      
    """.trimIndent()
    )
  }
}
