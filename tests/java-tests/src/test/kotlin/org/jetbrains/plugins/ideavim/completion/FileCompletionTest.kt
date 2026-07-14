/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.completion

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

/**
 * Tests for Vim's insert-mode file-name completion, `<C-X><C-F>` (`:help i_CTRL-X_CTRL-F`).
 *
 * In Vim, `<C-X><C-F>` treats the text before the caret as a (partial) path and completes matching file and
 * directory names. The leader is split at the last path separator, the trailing part is globbed with `*`, and
 * directories are offered with a trailing `/` (Vim's `EW_DIR|EW_ADDSLASH`; see `ins_compl_files()` in
 * `insexpand.c`). Repeating `<C-F>` cycles to the next match.
 *
 * These tests assert the IdeaVim (IntelliJ-lookup) behaviour model, exactly like [LineCompletionTest]:
 * `<C-X><C-F>` opens the IDE lookup populated with the matching names, and each subsequent `<C-F>` only moves the
 * selection within that lookup — the buffer is NOT modified until an item is accepted.
 *
 * Setup mirrors [org.jetbrains.plugins.ideavim.ex.implementation.commands.CommandLineCompletionTest]: file
 * completion reads the real [LocalFileSystem] (via `project.basePath` for relative paths), so candidates are
 * created in a real [TempDir] and referenced with an absolute path prefix. Files added to the light fixture's
 * in-memory VFS would not be found.
 *
 * NOTE: `<C-X><C-F>` file completion is not implemented yet, so these tests currently fail — they are written
 * first to pin down the intended behaviour.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "IntelliJ code completion lookup has no Neovim equivalent that can be driven via the RPC harness",
)
class FileCompletionTest : VimJavaTestCase() {

  @TempDir
  lateinit var tempDir: Path

  private lateinit var tempPath: String

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    Checks.keyHandler = Checks.KeyHandlerMethod.DIRECT_TO_VIM
    tempPath = tempDir.absolutePathString()
  }

  private fun createFooFiles() {
    tempDir.resolve("foo.txt").createFile()
    tempDir.resolve("foobar.kt").createFile()
    tempDir.resolve("foolib").createDirectories() // a directory that matches "foo"
    tempDir.resolve("baz.txt").createFile()
    // Make sure the VFS knows about the freshly created files
    LocalFileSystem.getInstance().refreshAndFindFileByNioFile(tempDir)
  }

  @Test
  fun `test Ctrl-X Ctrl-F opens a lookup listing the matching file names`() {
    createFooFiles()
    configureByText("$tempPath/foo${c}\n") // leader before the caret is "<tmp>/foo"
    typeText("i")
    typeText("<C-X><C-F>")

    assertState(Mode.INSERT)
    assertNotNull(activeLookup(), "Expected <C-X><C-F> to open a completion lookup")
    assertTrue(
      lookupStrings().containsAll(listOf("$tempPath/foo.txt", "$tempPath/foobar.kt")),
      "Expected the lookup to offer files starting with \"foo\" but was ${lookupStrings()}",
    )
    assertFalse(
      lookupStrings().contains("$tempPath/baz.txt"),
      "A non-matching file should not be offered but was ${lookupStrings()}",
    )
  }

  @Test
  fun `test Ctrl-X Ctrl-F selects a matching file without modifying the buffer`() {
    createFooFiles()
    configureByText("$tempPath/foo${c}\n")
    typeText("i")
    typeText("<C-X><C-F>")

    assertNotNull(activeLookup(), "Lookup should be open")
    assertTrue(
      selectedLookupString()?.startsWith("$tempPath/foo") == true,
      "Selected item ${selectedLookupString()} should be one of the matching files",
    )
    // Lookup-selection model: nothing is inserted until the item is accepted, so the buffer is unchanged.
    assertState("$tempPath/foo${c}\n")
  }

  @Test
  fun `test each Ctrl-F moves the selection to another matching file and keeps the lookup open`() {
    createFooFiles()
    configureByText("$tempPath/foo${c}\n")
    typeText("i")
    typeText("<C-X><C-F>")

    assertState(Mode.INSERT)
    assertNotNull(activeLookup(), "Lookup should be open after <C-X><C-F>")
    var previous = selectedLookupString()
    assertNotNull(previous, "There should be a selected item")

    // Three names match "foo" (foo.txt, foobar.kt, foolib); each <C-F> moves to a different one and the lookup
    // stays open, while the buffer text remains untouched.
    repeat(2) {
      typeText("<C-F>")

      assertState(Mode.INSERT)
      assertNotNull(activeLookup(), "Lookup should stay open while cycling with <C-F>")
      assertState("$tempPath/foo${c}\n")
      val current = selectedLookupString()
      assertNotEquals(previous, current, "Each <C-F> should move the selection to a different file")
      previous = current
    }
  }

  @Test
  fun `test repeating Ctrl-F visits every matching file`() {
    createFooFiles()
    configureByText("$tempPath/foo${c}\n")
    typeText("i")
    typeText("<C-X><C-F>")

    val visited = linkedSetOf<String>()
    selectedLookupString()?.let { visited.add(it) }
    repeat(2) {
      typeText("<C-F>")
      selectedLookupString()?.let { visited.add(it) }
    }

    assertTrue(
      visited.contains("$tempPath/foo.txt") && visited.contains("$tempPath/foobar.kt"),
      "Cycling with <C-F> should visit the matching files but only saw $visited",
    )
  }

  // File completion is only reachable via the CTRL-X sub-mode (`i_CTRL-X_CTRL-F`). A bare `<C-F>` in ordinary
  // insert mode is not a Vim command and must NOT open the file-completion lookup.
  @Test
  fun `test Ctrl-F without preceding Ctrl-X does not open a lookup`() {
    createFooFiles()
    configureByText("$tempPath/foo${c}\n")
    typeText("i") // plain insert mode, NOT the CTRL-X sub-mode
    typeText("<C-F>")

    assertNull(activeLookup(), "A bare <C-F> outside the CTRL-X sub-mode should not open file completion")
    assertState(Mode.INSERT)
    assertState("$tempPath/foo${c}\n")
  }

  // Vim offers directories with a trailing slash (EW_DIR|EW_ADDSLASH), so a following <C-F> descends into them.
  @Test
  fun `test Ctrl-X Ctrl-F offers directories with a trailing slash`() {
    createFooFiles()
    configureByText("$tempPath/foo${c}\n")
    typeText("i")
    typeText("<C-X><C-F>")

    assertNotNull(activeLookup(), "Expected <C-X><C-F> to open a completion lookup")
    assertTrue(
      lookupStrings().contains("$tempPath/foolib/"),
      "Expected the directory to be offered with a trailing slash but was ${lookupStrings()}",
    )
  }

  // A leader containing a path separator resolves the directory part; only the trailing segment is matched.
  @Test
  fun `test Ctrl-X Ctrl-F completes names inside a subdirectory`() {
    tempDir.resolve("sub").createDirectories()
    tempDir.resolve("sub/alpha.txt").createFile()
    tempDir.resolve("sub/album.txt").createFile()
    tempDir.resolve("sub/other.txt").createFile()
    LocalFileSystem.getInstance().refreshAndFindFileByNioFile(tempDir)

    configureByText("$tempPath/sub/al${c}\n") // directory part "<tmp>/sub/", file part "al"
    typeText("i")
    typeText("<C-X><C-F>")

    assertNotNull(activeLookup(), "Expected <C-X><C-F> to open a completion lookup")
    val offered = lookupStrings()
    assertTrue(
      offered.containsAll(listOf("$tempPath/sub/alpha.txt", "$tempPath/sub/album.txt")),
      "Expected the sub-directory entries matching \"al\" but was $offered",
    )
    assertFalse(
      offered.contains("$tempPath/sub/other.txt"),
      "A non-matching entry in the sub-directory should not be offered but was $offered",
    )
  }

  // Vim matches the text BEFORE the caret only; text after the caret must not narrow the offered set.
  @Test
  fun `test Ctrl-X Ctrl-F matches only the text before the caret`() {
    createFooFiles()
    configureByText("$tempPath/foo${c}bar.kt\n") // caret sits between "foo" and "bar.kt"
    typeText("i")
    typeText("<C-X><C-F>")

    assertNotNull(activeLookup(), "Expected <C-X><C-F> to open a completion lookup")
    assertTrue(
      lookupStrings().contains("$tempPath/foo.txt"),
      "Expected the lookup to match the pre-caret prefix \"foo\" but was ${lookupStrings()}",
    )
  }

  @Test
  fun `test Ctrl-Y accepts the selected file completion`() {
    createFooFiles()
    configureByText("$tempPath/foo${c}\n")
    typeText("i")
    typeText("<C-X><C-F>")
    val selected = selectedLookupString()
    assertNotNull(selected, "There should be a selected item to accept")

    typeText("<C-Y>")

    assertNull(activeLookup(), "Lookup should close after accepting with <C-Y>")
    assertState(Mode.INSERT)
    assertState("$selected${c}\n")
  }

  private fun activeLookup(): LookupImpl? {
    var lookup: LookupImpl? = null
    ApplicationManager.getApplication().invokeAndWait {
      lookup = LookupManager.getActiveLookup(fixture.editor) as? LookupImpl
    }
    return lookup
  }

  private fun lookupStrings(): List<String> = fixture.lookupElementStrings ?: emptyList()

  private fun selectedLookupString(): String? {
    var selected: String? = null
    ApplicationManager.getApplication().invokeAndWait {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      selected = (LookupManager.getActiveLookup(fixture.editor) as? LookupImpl)?.currentItem?.lookupString
    }
    return selected
  }
}
