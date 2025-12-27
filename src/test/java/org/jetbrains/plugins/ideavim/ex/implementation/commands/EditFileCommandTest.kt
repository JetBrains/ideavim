/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.EditFileCommand
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EditFileCommandTest : VimTestCase() {
  @TempDir
  lateinit var tempDir: File

  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("edit ~/.ideavimrc")
    assertTrue(command is EditFileCommand)
    assertEquals("~/.ideavimrc", command.argument)
  }

  @Test
  fun `command parsing 2`() {
    val command = injector.vimscriptParser.parseCommand("browse ~/.ideavimrc")
    assertTrue(command is EditFileCommand)
    assertEquals("~/.ideavimrc", command.argument)
  }

  @VimBehaviorDiffers(
    description = "Vim creates a new buffer without creating the file on disk; IdeaVim creates the file immediately because IntelliJ requires an existing file to open an editor",
    shouldBeFixed = false,
  )
  @Test
  fun `test edit creates new file when it does not exist`() {
    configureByText("original content")
    val newFileName = "newfile_${System.currentTimeMillis()}.txt"
    val newFilePath = File(tempDir, newFileName).absolutePath

    // Make sure the file does NOT exist before
    val fileBeforeEdit = File(newFilePath)
    if (fileBeforeEdit.exists()) {
      fileBeforeEdit.delete()
    }

    enterCommand("edit $newFilePath")

    // File should have been created
    val fileAfterEdit = File(newFilePath)
    assertTrue(fileAfterEdit.exists(), "File should have been created by :edit command")

    // Should not show error
    assertPluginError(false)

    // The file should now be open
    ApplicationManager.getApplication().invokeAndWait {
      val currentFile = fileManager.currentFile
      assertNotNull(currentFile, "A file should be open")
      assertEquals(newFileName, currentFile.name)
    }

    // Clean up
    fileAfterEdit.delete()
  }

  @VimBehaviorDiffers(
    description = "Vim creates a new buffer without creating the file on disk; IdeaVim creates the file immediately because IntelliJ requires an existing file to open an editor",
    shouldBeFixed = false,
  )
  @Test
  fun `test edit creates file in nested directories`() {
    configureByText("original content")
    val nestedPath = "subdir1/subdir2/nested_${System.currentTimeMillis()}.txt"
    val newFilePath = File(tempDir, nestedPath).absolutePath

    // Make sure the file and directories do NOT exist before
    val fileBeforeEdit = File(newFilePath)
    if (fileBeforeEdit.exists()) {
      fileBeforeEdit.delete()
    }

    enterCommand("edit $newFilePath")

    // File should have been created with parent directories
    val fileAfterEdit = File(newFilePath)
    assertTrue(fileAfterEdit.exists(), "File should have been created with parent directories by :edit command")
    assertTrue(fileAfterEdit.parentFile.exists(), "Parent directories should have been created")

    // Should not show error
    assertPluginError(false)

    // Clean up
    fileAfterEdit.delete()
    File(tempDir, "subdir1/subdir2").delete()
    File(tempDir, "subdir1").delete()
  }

  @Test
  fun `test edit opens existing file without creating new one`() {
    configureByText("original content")
    val existingFileName = "existing_${System.currentTimeMillis()}.txt"
    val existingFile = File(tempDir, existingFileName)
    existingFile.writeText("existing content")

    val lastModified = existingFile.lastModified()

    enterCommand("edit ${existingFile.absolutePath}")

    // File should still exist with same modification time (not recreated)
    assertTrue(existingFile.exists(), "File should still exist")
    assertEquals(lastModified, existingFile.lastModified(), "File should not have been recreated")

    // Should not show error
    assertPluginError(false)

    // The file should now be open
    ApplicationManager.getApplication().invokeAndWait {
      val currentFile = fileManager.currentFile
      assertNotNull(currentFile, "A file should be open")
      assertEquals(existingFileName, currentFile.name)
    }

    // Clean up
    existingFile.delete()
  }
}