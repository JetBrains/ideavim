/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
      set nu
      set relativenumber" another comment
    """.trimIndent()
    val changedFile = """
      " comment
      map x y
      set ${s}${s}${s}nu
      set relativenumber
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
