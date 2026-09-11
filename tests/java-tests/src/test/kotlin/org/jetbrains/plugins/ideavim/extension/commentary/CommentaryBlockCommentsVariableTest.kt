/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.commentary

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for `g:commentary_block_comments` (VIM-2629).
 *
 * vim-commentary always comments whole lines, even when `commentstring` describes a block comment.
 * IdeaVim additionally emits a real block comment whenever the range is not linewise, which is what
 * makes `gciw` able to comment out a single argument. That is useful, but it is not what
 * vim-commentary does.
 *
 * The variable picks between the two behaviours. It defaults to 1, i.e. the existing IdeaVim
 * behaviour; setting it to 0 makes every range comment whole lines, like vim-commentary.
 *
 * The variable is read per command, not when the extension is initialised, so it can be changed
 * from `:let` mid-session.
 */
class CommentaryBlockCommentsVariableTest : VimJavaTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("commentary")
  }

  // Default: block comments are on, i.e. nothing about the current behaviour changes

  @Test
  fun `test block comments are enabled by default`() {
    doCommentaryTest(
      "gciw",
      "<caret>if (condition) {\n" + "}\n",
      "/*if*/ (condition) {\n" + "}\n",
    )
  }

  @Test
  fun `test explicitly enabling the variable keeps block comments`() {
    doCommentaryTest(
      "gciw",
      "<caret>if (condition) {\n" + "}\n",
      "/*if*/ (condition) {\n" + "}\n",
      blockComments = 1,
    )
  }

  /**
   * Vimscript treats every non-zero Number as true, so only `0` may turn block comments off. A
   * config that flips the variable with something other than a literal `1` must still enable them.
   */
  @Test
  fun `test a non-zero value enables block comments`() {
    doCommentaryTest(
      "gciw",
      "<caret>if (condition) {\n" + "}\n",
      "/*if*/ (condition) {\n" + "}\n",
      blockComments = 2,
    )
  }

  @Test
  fun `test a negative value enables block comments`() {
    doCommentaryTest(
      "gciw",
      "<caret>if (condition) {\n" + "}\n",
      "/*if*/ (condition) {\n" + "}\n",
      blockComments = -1,
    )
  }

  @Test
  fun `test enabled block comments comment out a single argument`() {
    doCommentaryTest(
      "gciw",
      "foo(a, <caret>bar, c);\n",
      "foo(a, /*bar*/, c);\n",
      blockComments = 1,
    )
  }

  // Disabled: a charwise range comments the whole line, like vim-commentary

  @Test
  fun `test inner word comments the whole line when block comments are disabled`() {
    doCommentaryTest(
      "gciw",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "}\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test charwise motion comments the whole line when block comments are disabled`() {
    doCommentaryTest(
      "gcll",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "}\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test till motion comments the whole line when block comments are disabled`() {
    doCommentaryTest(
      "gct{",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "}\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test argument is not block commented when block comments are disabled`() {
    doCommentaryTest(
      "gciw",
      "foo(a, <caret>bar, c);\n",
      "//foo(a, bar, c);\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test charwise motion spanning lines comments every line when block comments are disabled`() {
    doCommentaryTest(
      "gcj",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "//}\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test charwise visual selection spanning lines comments every line when block comments are disabled`() {
    doCommentaryTest(
      "vjgc",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "//}\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test charwise visual selection within a line comments the whole line when block comments are disabled`() {
    doCommentaryTest(
      "vllgc",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "}\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test blockwise visual selection comments every line when block comments are disabled`() {
    doCommentaryTest(
      "<C-V>jgc",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "//}\n",
      blockComments = 0,
    )
  }

  // Disabled: linewise ranges are unchanged, they were already line comments

  @Test
  fun `test gcc is unchanged when block comments are disabled`() {
    doCommentaryTest(
      "gcc",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "}\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test linewise visual selection is unchanged when block comments are disabled`() {
    doCommentaryTest(
      "Vjgc",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "//}\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test inner paragraph is unchanged when block comments are disabled`() {
    doCommentaryTest(
      "gcip",
      "<caret>if (condition) {\n" + "}\n",
      "//if (condition) {\n" + "//}\n",
      blockComments = 0,
    )
  }

  // Disabled: toggling back off, repeating and the gc text object

  @Test
  fun `test a charwise motion uncomments the line when block comments are disabled`() {
    doCommentaryTest(
      "gciw" + "gciw",
      "<caret>if (condition) {\n" + "}\n",
      "if (condition) {\n" + "}\n",
      blockComments = 0,
    )
  }

  @Test
  fun `test repeat uses line comments when block comments are disabled`() {
    doCommentaryTest(
      "gciw" + "jj.",
      """
        <caret>if (condition) {
        }
        if (condition) {
        }
      """.trimIndent(),
      """
        //if (condition) {
        }
        //if (condition) {
        }
      """.trimIndent(),
      blockComments = 0,
    )
  }

  @Test
  fun `test gc text object is unaffected when block comments are disabled`() {
    doCommentaryTest(
      "dgc",
      """
        // <caret>Comment 1
        // Comment 2
        final Int value = 42;
      """.trimIndent(),
      """
        final Int value = 42;
      """.trimIndent(),
      blockComments = 0,
    )
  }

  @Test
  fun `test Commentary command is unaffected when block comments are disabled`() {
    configureByJavaText("<caret>if (condition) {\n" + "}\n")
    enterCommand("let g:commentary_block_comments = 0")
    enterCommand("1,2Commentary")
    dispatchEvents()
    assertState("//if (condition) {\n" + "//}\n")
  }

  // The variable is read per command, so it can be flipped mid-session

  @Test
  fun `test the variable is re-read for every command`() {
    configureByJavaText("<caret>if (condition) {\n" + "while (other) {\n")

    enterCommand("let g:commentary_block_comments = 0")
    typeText("gciw")
    dispatchEvents()

    enterCommand("let g:commentary_block_comments = 1")
    typeText("j", "gciw")
    dispatchEvents()

    assertState("//if (condition) {\n" + "/*while*/ (other) {\n")
  }


  private fun doCommentaryTest(keys: String, before: String, after: String, blockComments: Int? = null) {
    doTest(
      keys,
      before,
      after,
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
      afterEditorInitialized = {
        if (blockComments != null) {
          enterCommand("let g:commentary_block_comments = $blockComments")
        }
      },
    )
  }

  private fun dispatchEvents() {
    ApplicationManager.getApplication().invokeAndWait {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }
}
