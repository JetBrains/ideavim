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
}