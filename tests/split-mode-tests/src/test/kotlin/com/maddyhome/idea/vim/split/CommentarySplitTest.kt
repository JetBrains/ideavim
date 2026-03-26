/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommentarySplitTest : IdeaVimStarterTestBase() {

  private var commentaryInstalled = false

  fun setUpCommentary() {
    if (commentaryInstalled) return
    pause()
    exCommand("Plug 'tpope/vim-commentary'")
    commentaryInstalled = true
  }

  private fun javaFile(name: String) = createFile(
    "src/$name.java", """
    public class $name {
        int a = 1;
        int b = 2;
        int c = 3;
        int d = 4;
    }
  """.trimIndent() + "\n"
  )

  @Test
  fun `gcc comments line and undo removes it in single step`() {
    openFile(javaFile("Comment1"))
    setUpCommentary()
    goToLine(2)
    typeVim("gcc")

    assertEditorContains("//", "Line should be commented")

    typeVim("u")

    assertEditorNotContains("//", "Undo should remove comment completely")
    assertEditorContains("int a = 1;", "Original line should be restored")
  }

  @Test
  fun `visual multi-line comment and undo`() {
    openFile(javaFile("Comment2"))
    setUpCommentary()
    goToLine(2)
    typeVim("Vjjgc")

    val text = editorText()
    val commentCount = text.lines().count { it.trimStart().startsWith("//") }
    assertEquals(3, commentCount) { "Should have exactly 3 commented lines (lines 2-4). Text: $text" }

    typeVim("u")

    assertEditorNotContains("//", "Undo should remove all comments")
  }

  @Test
  fun `uncomment with gcgc`() {
    openFile(javaFile("Comment3"))
    setUpCommentary()
    goToLine(2)
    typeVim("gcc")

    assertEditorContains("//", "Line should be commented")

    typeVim("gcgc")

    val line2 = editorText().lines().getOrNull(1) ?: ""
    assertTrue(!line2.contains("//")) { "Line should be uncommented. Line: $line2" }
  }
}
