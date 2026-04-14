/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.plugins.ideavim.action.ex.VimExTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

class CommandLineCompletionTest : VimExTestCase() {

  @TempDir
  lateinit var tempDir: Path

  private lateinit var tempPath: String

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    createTestFiles()
    tempPath = tempDir.absolutePathString()
  }

  private fun createTestFiles() {
    tempDir.resolve("alpha.txt").createFile()
    tempDir.resolve("beta.txt").createFile()
    tempDir.resolve("bravo.kt").createFile()
    tempDir.resolve("subdir").createDirectories()
    tempDir.resolve("subdir/nested.txt").createFile()

    // Make sure VFS knows about these files
    LocalFileSystem.getInstance().refreshAndFindFileByNioFile(tempDir)
  }

  @Test
  fun `test tab completes first file match`() {
    typeText(":edit $tempPath/a<Tab>")
    assertExText("edit $tempPath/alpha.txt")
  }

  @Test
  fun `test tab cycles through matches`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Tab>")
    assertExText("edit $tempPath/bravo.kt")
  }

  @Test
  fun `test tab wraps around to first match`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Tab>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Tab>")
    assertExText("edit $tempPath/beta.txt")
  }

  @Test
  fun `test shift tab cycles backwards`() {
    typeText(":edit $tempPath/b<S-Tab>")
    assertExText("edit $tempPath/bravo.kt")
  }

  @Test
  fun `test shift tab then tab`() {
    typeText(":edit $tempPath/b<S-Tab>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Tab>")
    assertExText("edit $tempPath/beta.txt")
  }

  @Test
  fun `test tab completes directory with trailing slash`() {
    typeText(":edit $tempPath/s<Tab>")
    assertExText("edit $tempPath/subdir/")
  }

  @Test
  fun `test tab lists all files when prefix is empty`() {
    typeText(":edit $tempPath/<Tab>")
    // First match alphabetically
    assertExText("edit $tempPath/alpha.txt")
  }

  @Test
  fun `test tab with no matches does not change text`() {
    typeText(":edit $tempPath/zzz<Tab>")
    assertExText("edit $tempPath/zzz")
  }

  @Test
  fun `test tab does nothing in search mode`() {
    typeText("/search<Tab>")
    // Tab is not handled by the completion action in search mode,
    // but it's still consumed by the action framework -- no literal tab inserted
    assertExText("search")
  }

  @Test
  fun `test tab works with abbreviated command`() {
    typeText(":e $tempPath/a<Tab>")
    assertExText("e $tempPath/alpha.txt")
  }

  @Test
  fun `test tab works with write command`() {
    typeText(":w $tempPath/a<Tab>")
    assertExText("w $tempPath/alpha.txt")
  }

  @Test
  fun `test tab does not complete for commands without file completion`() {
    typeText(":set foo<Tab>")
    // Tab is consumed by the action but set has no completion type registered
    assertExText("set foo")
  }

  @Test
  fun `test typing after completion invalidates session`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    // Type a character -- this changes text, invalidating the completion session
    typeText("x")
    assertExText("edit $tempPath/beta.txtx")

    // Tab starts a fresh completion for prefix "beta.txtx" -- no matches
    typeText("<Tab>")
    assertExText("edit $tempPath/beta.txtx")
  }

  @Test
  fun `test undo after completion resumes cycling`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    // Type and undo -- text reverts to the expected completion text
    typeText("x")
    typeText("<BS>")
    assertExText("edit $tempPath/beta.txt")

    // Tab resumes cycling since text matches the active completion
    typeText("<Tab>")
    assertExText("edit $tempPath/bravo.kt")
  }

  @Test
  fun `test completion with single match`() {
    typeText(":edit $tempPath/al<Tab>")
    assertExText("edit $tempPath/alpha.txt")

    // Tab again cycles (single match wraps to itself)
    typeText("<Tab>")
    assertExText("edit $tempPath/alpha.txt")
  }

  @Test
  fun `test completion is case insensitive`() {
    typeText(":edit $tempPath/A<Tab>")
    assertExText("edit $tempPath/alpha.txt")
  }
}
