/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * VIM-4351 in split mode: `<C-O>` brings back the diff tab a jump was recorded in
 *
 * The diff tab, its editors and the jump list live on the frontend, while opening a file goes through the backend.
 * The project is a Git repository with one changed file, and every test compares it with its version in the repository.
 */
class DiffJumpSplitTest : IdeaVimStarterTestBase() {
  private val before = (1..20).joinToString("\n") { "Line $it" } + "\n"
  private val after = before.replace("Line 3\n", "Line 3\nAdded line\n")

  override fun beforeContextCreated() {
    projectDir.resolve(".idea").createDirectories()
    projectDir.resolve(".idea/vcs.xml").writeText(
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <project version="4">
        <component name="VcsDirectoryMappings">
          <mapping directory="" vcs="Git" />
        </component>
      </project>
      """.trimIndent(),
    )
    createFile("src/Changed.txt", before)
    createFile("src/Other.txt", "Other line 1\nOther line 2\n")
    git("init", "-q")
    git("add", ".")
    git("commit", "-q", "-m", "Initial")
    createFile("src/Changed.txt", after)
  }

  @AfterEach
  fun closeEditors() {
    closeAllEditors()
  }

  @Test
  fun `control-O returns to the right side of the diff`() {
    openDiffOfChangedFile()
    typeInDiffSide(after) { typeText("10G"); typeText(":e src/Other.txt", delayBetweenCharsInMs = 50); enter() }
    waitForOtherFile()

    ctrlOInFile("Other.txt")

    assertDiffShown()
    assertEquals(10, waitForCaretLineInDiffSide(after, 10), "Caret on the right side of the diff")
  }

  @Test
  fun `control-O returns to the left side of the diff`() {
    openDiffOfChangedFile()
    typeInDiffSide(before) { typeText("8G"); typeText(":e src/Other.txt", delayBetweenCharsInMs = 50); enter() }
    waitForOtherFile()

    ctrlOInFile("Other.txt")

    assertDiffShown()
    assertEquals(8, waitForCaretLineInDiffSide(before, 8), "Caret on the left side of the diff")
  }

  @Test
  fun `control-O from the right side of a closed diff opens the file`() {
    openDiffOfChangedFile()
    typeInDiffSide(after) { typeText("10G"); typeText(":e src/Other.txt", delayBetweenCharsInMs = 50); enter() }
    waitForOtherFile()
    closeOtherEditors()

    ctrlOInFile("Other.txt")

    assertCaretInFileAfter("Changed.txt", 9, "<C-O> should open the file at the line of the jump")
    assertCaretInFileBefore("Changed.txt", 11, "<C-O> should open the file at the line of the jump")
  }

  @Test
  fun `control-O inside the diff moves back within the same side`() {
    openDiffOfChangedFile()
    typeInDiffSide(after) {
      typeText("5GG")
      hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_O)
    }

    assertDiffShown()
    assertEquals(5, waitForCaretLineInDiffSide(after, 5), "Caret on the right side of the diff")
  }

  private fun openDiffOfChangedFile() {
    openFile("src/Changed.txt")
    compareWithRepository()
  }

  private fun waitForOtherFile() {
    check(waitUntil(timeoutMs = 15_000, pollMs = 500) { !isDiffShown() }) { ":e did not leave the diff" }
    openFileInCurrentWindow("src/Other.txt", "Other.txt")
  }

  private fun assertDiffShown() {
    assertTrue(waitUntil(timeoutMs = 10_000) { isDiffShown() }, "<C-O> should bring back the diff, not the plain file")
  }

  private fun waitForCaretLineInDiffSide(sideText: String, line: Int): Int {
    var actual = 0
    waitUntil { actual = caretLineInDiffSide(sideText); actual == line }
    return actual
  }

  private fun git(vararg args: String) {
    val process = ProcessBuilder("git", *args).directory(projectDir.toFile()).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor() == 0) { "git ${args.joinToString(" ")} failed: $output" }
  }
}
