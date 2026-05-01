/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EditFileCreateTest : VimTestCase() {

  @Test
  fun `test edit creates file if it does not exist`() {
    configureByText("\n")

    val fileName = "vim266_new_${System.currentTimeMillis()}.txt"

    typeText(commandToKeys("edit $fileName"))

    val currentFile = FileEditorManager.getInstance(fixture.project).selectedFiles.firstOrNull()

    assertNotNull(currentFile, "No file is currently selected/open")
    assertEquals(fileName, currentFile.name, "The editor should have switched to the new file")

    val foundOnDisk = LocalFileSystem.getInstance().refreshAndFindFileByPath(currentFile.path)
    assertNotNull(foundOnDisk, "File should physically exist in the test VFS")
  }
}