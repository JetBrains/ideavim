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
import com.maddyhome.idea.vim.ui.VimRcFileState
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class ReloadVimRcTest : VimTestCase() {
  private val editorFactory = MockEditorFactory()

  override fun setUp() {
    super.setUp()
    VimRcFileState.clear()
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test equalTo`() {
    val file = """
      map x y
    """.trimIndent()

    VimRcFileState.saveFileState("", file)

    val document = editorFactory.createDocument(file)

    assertTrue(VimRcFileState.equalTo(document))
  }

  // TODO
//  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
//  fun `test equalTo with whitespaces`() {
//    val s = " " // Just to see whitespaces in the following code
//    val origFile = """
//      map x y
//      set myPlugin
//      map z t
//    """.trimIndent()
//    val changedFile = """
//      map x y
//      set myPlugin$s$s$s$s$s$s
//
//
//            map z t
//    """.trimIndent()
//
//    val lines = convertFileToLines(origFile)
//    VimRcFileState.saveFileState("", lines)
//
//    val document = editorFactory.createDocument(changedFile)
//
//    assertTrue(VimRcFileState.equalTo(document))
//  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test equalTo with whitespaces and comments`() {
    val s = " " // Just to see whitespaces in the following code
    val origFile = """
      map x y|"comment
      let g:x = 5
      let g:y = 3 " another comment
    """.trimIndent()
    val changedFile = """
      " comment
      map x y
      let g:x = ${s}${s}${s}5
      let g:y = 3
    """.trimIndent()

    VimRcFileState.saveFileState("", origFile)

    val document = editorFactory.createDocument(changedFile)

    assertTrue(VimRcFileState.equalTo(document))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
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

    VimRcFileState.saveFileState("", origFile)

    val document = editorFactory.createDocument(changedFile)

    assertFalse(VimRcFileState.equalTo(document))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test equalTo remove line`() {
    val origFile = """
      map x y
      set myPlugin
    """.trimIndent()
    val changedFile = """
      map x y
    """.trimIndent()

    VimRcFileState.saveFileState("", origFile)

    val document = editorFactory.createDocument(changedFile)

    assertFalse(VimRcFileState.equalTo(document))
  }
}
