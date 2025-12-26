/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.EditFileCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EditFileCommandTest : VimTestCase() {
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

  @Test
  fun `test edit creates new buffer for non-existent file`() {
    configureByText("initial text")
    val initialEditor = fixture.editor
    val initialFile = fixture.file.virtualFile

    enterCommand("edit nonexistent.txt")

    // Get the file editor manager to check what was opened
    val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(fixture.project)
    val openFiles = fileEditorManager.openFiles

    // Find the newly opened file
    val newFile = openFiles.find { it.name == "nonexistent.txt" }
    assertNotNull(newFile, "Expected 'nonexistent.txt' to be opened")

    // Verify that it's a different file from the initial one
    assertTrue(newFile !== initialFile)

    // Get the editor for the new file
    val editors = fileEditorManager.getEditors(newFile!!)
    assertTrue(editors.isNotEmpty(), "Expected editor for nonexistent.txt")

    val newEditor = (editors[0] as com.intellij.openapi.fileEditor.TextEditor).editor

    // Verify that the new buffer is empty
    assertEquals("", newEditor.document.text)
  }
}