/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ui

import com.intellij.mock.MockEditorFactory
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser
import com.maddyhome.idea.vim.ui.VimRcFileState
import org.jetbrains.plugins.ideavim.VimTestCase

class ReloadVimRcTest : VimTestCase() {
  private val editorFactory = MockEditorFactory()

  override fun setUp() {
    super.setUp()
    VimRcFileState.clear()
  }

  fun `test equalTo`() {
    val file = """
      map x y
    """.trimIndent()

    val lines = convertFileToLines(file)
    VimRcFileState.saveFileState("", lines)

    val document = editorFactory.createDocument(file)

    assertTrue(VimRcFileState.equalTo(document))
  }

  fun `test equalTo with whitespaces`() {
    val s = " "  // Just to see whitespaces in the following code
    val origFile = """
      map x y
      set myPlugin
      map z t
    """.trimIndent()
    val changedFile = """
      map x y
      set myPlugin$s$s$s$s$s$s
      
      
            map z t
    """.trimIndent()

    val lines = convertFileToLines(origFile)
    VimRcFileState.saveFileState("", lines)

    val document = editorFactory.createDocument(changedFile)

    assertTrue(VimRcFileState.equalTo(document))
  }

  fun `test equalTo add line`() {
    val origFile = """
      map x y
      set myPlugin
    """.trimIndent()
    val changedFile = """
      map x y
      set myPlugin
      map z t
    """.trimIndent()

    val lines = convertFileToLines(origFile)
    VimRcFileState.saveFileState("", lines)

    val document = editorFactory.createDocument(changedFile)

    assertFalse(VimRcFileState.equalTo(document))
  }

  fun `test equalTo remove line`() {
    val origFile = """
      map x y
      set myPlugin
    """.trimIndent()
    val changedFile = """
      map x y
    """.trimIndent()

    val lines = convertFileToLines(origFile)
    VimRcFileState.saveFileState("", lines)

    val document = editorFactory.createDocument(changedFile)

    assertFalse(VimRcFileState.equalTo(document))
  }

  private fun convertFileToLines(file: String): List<String> {
    val res = ArrayList<String>()
    file.split("\n").forEach { VimScriptParser.lineProcessor(it, res) }
    return res
  }
}
