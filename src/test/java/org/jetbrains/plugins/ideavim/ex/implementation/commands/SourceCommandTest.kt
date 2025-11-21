/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.key
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.MappingInfo
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.commands.SourceCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourceCommandTest : VimTestCase() {
  @TempDir
  var tempDir: File? = null

  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("source ~/.vimrc")
    assertTrue(command is SourceCommand)
    assertEquals("~/.vimrc", command.argument)
  }

  @Test
  fun `loading ideavimrc configuration via API`() {
    configureByText("")
    try {

      val layerPreCheck = injector.keyGroup.getKeyMappingLayer(MappingMode.NORMAL)
      val mappingPreCheck = layerPreCheck.getLayer(listOf(key("x")))
      assertNull(mappingPreCheck) // Make sure we don't yet have a mapping from x

      val file = File(tempDir, "text.txt")
      file.writeText(
        """
        map x y
      """.trimIndent()
      )

      injector.vimscriptExecutor.executeFile(file, fixture.editor.vim, true)
      val layer = injector.keyGroup.getKeyMappingLayer(MappingMode.NORMAL)
      val mapping = layer.getLayer(listOf(key("x"))) as MappingInfo
      assertEquals(MappingOwner.IdeaVim.InitScript, mapping.owner)
    } finally {
      injector.keyGroup.removeKeyMapping(MappingMode.NXO, listOf(key("x")))
    }
  }

  @Test
  fun `loading NOT ideavimrc configuration via API`() {
    configureByText("")
    try {

      val layerPreCheck = injector.keyGroup.getKeyMappingLayer(MappingMode.NORMAL)
      val mappingPreCheck = layerPreCheck.getLayer(listOf(key("x")))
      assertNull(mappingPreCheck) // Make sure we don't yet have a mapping from x

      val file = File(tempDir, "text.txt")
      file.writeText(
        """
        map x y
      """.trimIndent()
      )

      injector.vimscriptExecutor.executeFile(file, fixture.editor.vim, false)
      val layer = injector.keyGroup.getKeyMappingLayer(MappingMode.NORMAL)
      val mapping = layer.getLayer(listOf(key("x"))) as MappingInfo
      assertEquals(MappingOwner.IdeaVim.Other, mapping.owner)
    } finally {
      injector.keyGroup.removeKeyMapping(MappingMode.NXO, listOf(key("x")))
    }
  }

  // Environment variable expansion tests

  @Test
  fun `test source expands environment variable`() {
    configureByText("")

    // Create a test file in a subdirectory named after PATH env var (to verify expansion)
    val testEnvVar = System.getenv("USER") ?: System.getenv("USERNAME") ?: "testuser"
    val subDir = File(tempDir, testEnvVar)
    subDir.mkdirs()
    val testFile = File(subDir, "config.vim")
    testFile.writeText("map x y")

    try {
      // Source using $USER environment variable
      enterCommand("source ${tempDir!!.absolutePath}/\$USER/config.vim")

      // Verify the file was sourced by checking the mapping was created
      val layer = injector.keyGroup.getKeyMappingLayer(MappingMode.NORMAL)
      val mapping = layer.getLayer(listOf(key("x"))) as? MappingInfo
      assertTrue(mapping != null, "Mapping should exist, proving file was sourced with env var expansion")
    } finally {
      injector.keyGroup.removeKeyMapping(MappingMode.NXO, listOf(key("x")))
    }
  }

  @Test
  fun `test source expands environment variable with braces`() {
    configureByText("")

    val testEnvVar = System.getenv("USER") ?: System.getenv("USERNAME") ?: "testuser"
    val subDir = File(tempDir, testEnvVar)
    subDir.mkdirs()
    val testFile = File(subDir, "config.vim")
    testFile.writeText("map z w")

    try {
      enterCommand("source ${tempDir!!.absolutePath}/\${USER}/config.vim")

      val layer = injector.keyGroup.getKeyMappingLayer(MappingMode.NORMAL)
      val mapping = layer.getLayer(listOf(key("z"))) as? MappingInfo
      assertTrue(mapping != null, "File should be sourced using \${VAR} syntax")
    } finally {
      injector.keyGroup.removeKeyMapping(MappingMode.NXO, listOf(key("z")))
    }
  }

  @Test
  fun `test source expands tilde to home directory`() {
    configureByText("")

    // Create a test file in home directory
    val home = System.getProperty("user.home")
    val testFile = File(home, ".ideavim_test_source.vim")
    testFile.writeText("map a b")

    try {
      enterCommand("source ~/.ideavim_test_source.vim")

      val layer = injector.keyGroup.getKeyMappingLayer(MappingMode.NORMAL)
      val mapping = layer.getLayer(listOf(key("a"))) as? MappingInfo
      assertTrue(mapping != null, "File should be sourced using tilde expansion")
    } finally {
      injector.keyGroup.removeKeyMapping(MappingMode.NXO, listOf(key("a")))
      testFile.delete()
    }
  }

  @Test
  fun `test source expands mixed tilde and environment variable`() {
    configureByText("")

    val home = System.getProperty("user.home")
    val testEnvVar = System.getenv("USER") ?: System.getenv("USERNAME") ?: "testuser"

    // Create directory structure: ~/USER_VALUE/
    val subDir = File(home, testEnvVar)
    subDir.mkdirs()
    val testFile = File(subDir, "test.vim")
    testFile.writeText("map c d")

    try {
      enterCommand("source ~/\$USER/test.vim")

      val layer = injector.keyGroup.getKeyMappingLayer(MappingMode.NORMAL)
      val mapping = layer.getLayer(listOf(key("c"))) as? MappingInfo
      assertTrue(mapping != null, "File should be sourced with both tilde and env var expanded")
    } finally {
      injector.keyGroup.removeKeyMapping(MappingMode.NXO, listOf(key("c")))
      testFile.delete()
      subDir.delete()
    }
  }

  @Test
  fun `test source non-existent file shows error message`() {
    configureByText("")

    val nonExistentFile = File(tempDir, "non_existent_file.vim")
    // Make sure the file does NOT exist
    if (nonExistentFile.exists()) {
      nonExistentFile.delete()
    }

    // Execute source command for non-existent file
    enterCommand("source ${nonExistentFile.absolutePath}")

    // Verify that error was indicated (no exception thrown, graceful handling)
    // The command should complete without crashing
    assertPluginError(true)
  }
}