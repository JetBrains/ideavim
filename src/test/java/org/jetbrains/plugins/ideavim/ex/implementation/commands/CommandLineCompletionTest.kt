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
import kotlin.test.assertEquals

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
    tempDir.resolve("subdir/notes.md").createFile()

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
  fun `test tab does not file-complete echo argument even when prefix matches a real file`() {
    // The argument prefix would match `alpha.txt` if file completion ran -- it must not.
    typeText(":echo $tempPath/a<Tab>")
    assertExText("echo $tempPath/a")
  }

  @Test
  fun `test tab does not file-complete let argument even when prefix matches a real file`() {
    typeText(":let $tempPath/b<Tab>")
    assertExText("let $tempPath/b")
  }

  @Test
  fun `test tab does not file-complete map argument even when prefix matches a real file`() {
    typeText(":map $tempPath/s<Tab>")
    assertExText("map $tempPath/s")
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

  // --- Arrow key completion cycling tests ---

  @Test
  fun `test right arrow cycles forward after tab`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/bravo.kt")
  }

  @Test
  fun `test left arrow cycles backward after tab`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Left>")
    assertExText("edit $tempPath/bravo.kt")
  }

  @Test
  fun `test right arrow wraps around to first match`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Right>")
    assertExText("edit $tempPath/beta.txt")
  }

  @Test
  fun `test left arrow wraps around to last match`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Left>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Left>")
    assertExText("edit $tempPath/beta.txt")
  }

  @Test
  fun `test right then left returns to same match`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Left>")
    assertExText("edit $tempPath/beta.txt")
  }

  @Test
  fun `test tab then right continues cycling`() {
    typeText(":edit $tempPath/<Tab>")
    assertExText("edit $tempPath/alpha.txt")

    typeText("<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/bravo.kt")
  }

  @Test
  fun `test tab then left goes back`() {
    typeText(":edit $tempPath/<Tab>")
    assertExText("edit $tempPath/alpha.txt")

    typeText("<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Left>")
    assertExText("edit $tempPath/alpha.txt")
  }

  @Test
  fun `test shift tab then left continues backward`() {
    typeText(":edit $tempPath/b<S-Tab>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Left>")
    assertExText("edit $tempPath/beta.txt")
  }

  @Test
  fun `test shift tab then right reverses direction`() {
    typeText(":edit $tempPath/b<S-Tab>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Right>")
    assertExText("edit $tempPath/beta.txt")
  }

  @Test
  fun `test right arrow with single match stays on same item`() {
    typeText(":edit $tempPath/al<Tab>")
    assertExText("edit $tempPath/alpha.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/alpha.txt")
  }

  @Test
  fun `test left arrow with single match stays on same item`() {
    typeText(":edit $tempPath/al<Tab>")
    assertExText("edit $tempPath/alpha.txt")

    typeText("<Left>")
    assertExText("edit $tempPath/alpha.txt")
  }

  @Test
  fun `test right arrow without completion moves caret`() {
    typeText(":edit foo")
    assertExText("edit foo")

    val offsetBefore = exEntryPanel.caret.offset
    typeText("<Left>")
    assertEquals(offsetBefore - 1, exEntryPanel.caret.offset)

    typeText("<Right>")
    assertEquals(offsetBefore, exEntryPanel.caret.offset)
  }

  @Test
  fun `test typing after arrow completion invalidates session`() {
    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("x")
    assertExText("edit $tempPath/bravo.ktx")

    // Arrow key now moves caret instead of cycling
    val offsetBefore = exEntryPanel.caret.offset
    typeText("<Left>")
    assertEquals(offsetBefore - 1, exEntryPanel.caret.offset)
  }

  @Test
  fun `test arrow keys with no matches do not change text`() {
    typeText(":edit $tempPath/zzz<Tab>")
    assertExText("edit $tempPath/zzz")

    // No active completion, so arrows move caret
    val offsetBefore = exEntryPanel.caret.offset
    typeText("<Left>")
    assertEquals(offsetBefore - 1, exEntryPanel.caret.offset)
  }

  @Test
  fun `test mixed tab and arrow key cycling`() {
    typeText(":edit $tempPath/<Tab>")
    assertExText("edit $tempPath/alpha.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Tab>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Left>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<S-Tab>")
    assertExText("edit $tempPath/alpha.txt")
  }

  @Test
  fun `test arrow cycles through all files with empty prefix`() {
    typeText(":edit $tempPath/<Tab>")
    assertExText("edit $tempPath/alpha.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Right>")
    assertExText("edit $tempPath/subdir/")

    // Wraps
    typeText("<Right>")
    assertExText("edit $tempPath/alpha.txt")
  }

  @Test
  fun `test left arrow cycles all files backwards with empty prefix`() {
    typeText(":edit $tempPath/<Tab>")
    assertExText("edit $tempPath/alpha.txt")

    typeText("<Left>")
    assertExText("edit $tempPath/subdir/")

    typeText("<Left>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Left>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Left>")
    assertExText("edit $tempPath/alpha.txt")
  }

  // --- Subdirectory completion tests ---

  @Test
  fun `test tab completes inside subdirectory`() {
    typeText(":edit $tempPath/subdir/ne<Tab>")
    assertExText("edit $tempPath/subdir/nested.txt")
  }

  @Test
  fun `test tab cycles through files in subdirectory`() {
    typeText(":edit $tempPath/subdir/n<Tab>")
    assertExText("edit $tempPath/subdir/nested.txt")

    typeText("<Tab>")
    assertExText("edit $tempPath/subdir/notes.md")
  }

  @Test
  fun `test right arrow cycles in subdirectory`() {
    typeText(":edit $tempPath/subdir/n<Tab>")
    assertExText("edit $tempPath/subdir/nested.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/subdir/notes.md")

    typeText("<Right>")
    assertExText("edit $tempPath/subdir/nested.txt")
  }

  @Test
  fun `test left arrow cycles backwards in subdirectory`() {
    typeText(":edit $tempPath/subdir/n<Tab>")
    assertExText("edit $tempPath/subdir/nested.txt")

    typeText("<Left>")
    assertExText("edit $tempPath/subdir/notes.md")
  }

  @Test
  fun `test tab completes command name from abbreviation`() {
    typeText(":vs<Tab>")
    assertExText("vsplit")
  }

  @Test
  fun `test tab completes command name with single match`() {
    typeText(":tabc<Tab>")
    assertExText("tabclose")
  }

  @Test
  fun `test tab on full command name with no longer match keeps it unchanged`() {
    typeText(":edit<Tab>")
    assertExText("edit")
  }

  @Test
  fun `test tab cycles through command names sharing a prefix`() {
    typeText(":set<Tab>")
    assertExText("set")

    typeText("<Tab>")
    assertExText("setglobal")

    typeText("<Tab>")
    assertExText("sethandler")

    typeText("<Tab>")
    assertExText("setlocal")
  }

  @Test
  fun `test tab wraps after last command name match`() {
    typeText(":set<Tab>")
    typeText("<Tab>")
    typeText("<Tab>")
    typeText("<Tab>")
    assertExText("setlocal")

    typeText("<Tab>")
    assertExText("set")
  }

  @Test
  fun `test shift tab cycles command names backwards`() {
    typeText(":set<S-Tab>")
    assertExText("setlocal")

    typeText("<S-Tab>")
    assertExText("sethandler")
  }

  @Test
  fun `test right arrow cycles command names forward after tab`() {
    typeText(":set<Tab>")
    assertExText("set")

    typeText("<Right>")
    assertExText("setglobal")
  }

  @Test
  fun `test left arrow cycles command names backward after tab`() {
    typeText(":set<Tab>")
    assertExText("set")

    typeText("<Left>")
    assertExText("setlocal")
  }

  @Test
  fun `test tab on unknown command prefix does not change text`() {
    typeText(":xyzzy<Tab>")
    assertExText("xyzzy")
  }

  @Test
  fun `test typing after command name completion invalidates session`() {
    typeText(":set<Tab>")
    assertExText("set")

    typeText(" foo")
    assertExText("set foo")

    // `set` has no argument completion type registered, so Tab in argument position is a no-op.
    typeText("<Tab>")
    assertExText("set foo")
  }
}
