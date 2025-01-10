/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ui

import com.intellij.mock.MockEditorFactory
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.ui.VimRcFileState
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.nio.file.Files

class ReloadVimRcTest : VimTestCase() {
  private val editorFactory = MockEditorFactory()

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    VimRcFileState.clear()
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test equalTo`() {
    val file = """
      map x y
    """.trimIndent()

    VimRcFileState.saveFileState("", file)

    val document = editorFactory.createDocument(file)

    kotlin.test.assertTrue(VimRcFileState.equalTo(document))
  }

  // TODO
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  @Disabled
  fun `test equalTo with whitespaces`() {
    val s = " " // Just to see whitespaces in the following code
    """
      map x y
      set myPlugin
      map z t
    """.trimIndent()
    """
      map x y
      set myPlugin$s$s$s$s$s$s


            map z t
    """.trimIndent()

//    val lines = convertFileToLines(origFile)
//    VimRcFileState.saveFileState("", lines)
//
//    val document = editorFactory.createDocument(changedFile)
//
//    assertTrue(VimRcFileState.equalTo(document))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
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

    kotlin.test.assertTrue(VimRcFileState.equalTo(document))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
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

    kotlin.test.assertFalse(VimRcFileState.equalTo(document))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
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

    kotlin.test.assertFalse(VimRcFileState.equalTo(document))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `state is updated when we execute ideavimrc from source`() {
    configureByText("")
    val origFile = """
      map x y
      set myPlugin
    """.trimIndent()
    val changedFile = """
      map x y
    """.trimIndent()

    VimRcFileState.saveFileState("", origFile)

    val tempUpdatedFile = Files.createTempFile("xyz", ".txt").toFile()
    tempUpdatedFile.writeText(changedFile)

    val document = editorFactory.createDocument(changedFile)

    injector.vimscriptExecutor.executeFile(tempUpdatedFile, fixture.editor.vim, true)

    kotlin.test.assertTrue(VimRcFileState.equalTo(document))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `state is updated when we execute NOT ideavimrc from source`() {
    configureByText("")
    val origFile = """
      map x y
      set myPlugin
    """.trimIndent()
    val changedFile = """
      map x y
    """.trimIndent()

    VimRcFileState.saveFileState("", origFile)

    val tempUpdatedFile = Files.createTempFile("xyz", ".txt").toFile()
    tempUpdatedFile.writeText(changedFile)

    val document = editorFactory.createDocument(changedFile)

    injector.vimscriptExecutor.executeFile(tempUpdatedFile, fixture.editor.vim, false)

    kotlin.test.assertFalse(VimRcFileState.equalTo(document))
  }
}
