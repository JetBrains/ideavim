/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.WriteCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WriteCommandTest : VimTestCase() {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("write")
    assertTrue(command is WriteCommand)
  }

  @Test
  fun `write buffer to new file creates it with buffer content`() {
    val target = tempDir.resolve("written.txt").absolutePathString()
    configureByText("hello world\nsecond line\n")

    enterCommand("write $target")

    assertPluginError(false)
    assertEquals("hello world\nsecond line\n", readFile(target))
  }

  @Test
  fun `write ignores trailing whitespace in filename`() {
    val target = tempDir.resolve("trailing.txt").absolutePathString()
    configureByText("content\n")

    // Trailing spaces after the filename must not become part of the name.
    enterCommand("write $target   ")

    assertPluginError(false)
    assertEquals("content\n", readFile(target))
    // The path with trailing spaces must not exist as a separate file.
    assertNull(LocalFileSystem.getInstance().refreshAndFindFileByPath("$target   "))
  }

  @Test
  fun `write with range writes only the given lines`() {
    val target = tempDir.resolve("range.txt").absolutePathString()
    configureByText("line1\nline2\nline3\nline4\n")

    enterCommand("1,2write $target")

    assertPluginError(false)
    assertEquals("line1\nline2\n", readFile(target))
  }

  @Test
  fun `write without bang to existing file reports error and does not overwrite`() {
    val target = tempDir.resolve("existing.txt")
    Files.writeString(target, "original\n")
    val targetPath = target.absolutePathString()
    LocalFileSystem.getInstance().refreshAndFindFileByPath(targetPath)
    configureByText("new content\n")

    enterCommand("write $targetPath")

    // Vim: E13 File exists (add ! to override)
    assertPluginError(true)
    assertEquals("original\n", readFile(targetPath), "File must not be overwritten without !")
  }

  @Test
  fun `write with bang overwrites existing file`() {
    val target = tempDir.resolve("overwrite.txt")
    Files.writeString(target, "original\n")
    val targetPath = target.absolutePathString()
    LocalFileSystem.getInstance().refreshAndFindFileByPath(targetPath)
    configureByText("new content\n")

    enterCommand("write! $targetPath")

    assertPluginError(false)
    assertEquals("new content\n", readFile(targetPath))
  }

  private fun readFile(path: String): String {
    val file: VirtualFile = assertNotNull(
      LocalFileSystem.getInstance().refreshAndFindFileByPath(path),
      "Expected file to exist on disk: $path",
    )
    return String(file.contentsToByteArray())
  }
}
