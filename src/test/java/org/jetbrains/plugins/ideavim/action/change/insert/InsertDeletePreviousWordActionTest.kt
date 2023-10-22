/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertDeletePreviousWordActionTest : VimTestCase() {
  // VIM-1655
  @Test
  fun `test deleted word is not yanked`() {
    doTest(
      listOf("yiw", "3wea", "<C-W>", "<ESC>p"),
      """
            I found ${c}it in a legendary land
      """.trimIndent(),
      """
            I found it in a i${c}t land
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test word removed`() {
    doTest(
      listOf("i", "<C-W>"),
      """
            I found${c} it in a legendary land
      """.trimIndent(),
      """
            I ${c} it in a legendary land
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test non alpha chars`() {
    doTest(
      listOf("i", "<C-W>", "<C-W>", "<C-W>", "<C-W>"),
      """
            I found (it)${c} in a legendary land
      """.trimIndent(),
      """
            I ${c} in a legendary land
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test indents and spaces`() {
    doTest(
      listOf("i", "<C-W>", "<C-W>", "<C-W>", "<C-W>"),
      """
            Lorem Ipsum
            
                 I${c} found it in a legendary land
      """.trimIndent(),
      """
            Lorem Ipsum${c} found it in a legendary land
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @VimBehaviorDiffers(
    """
            If (found) {
               if (it) {
                  legendary
               }
            ${c}
  """,
  )
  @Test
  fun `test delete starting from the line end`() {
    doTest(
      listOf("i", "<C-W>"),
      """
            If (found) {
               if (it) {
                  legendary
               }
            }${c}
      """.trimIndent(),
      """
            If (found) {
               if (it) {
                  legendary
               ${c}
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  // VIM-569 |a| |i_CTRL-W|
  @Test
  fun `test delete previous word dot eol`() {
    doTest(
      listOf("a", "<C-W>"),
      "this is a sentence<caret>.\n",
      "this is a sentence<caret>\n",
      Mode.INSERT,
    )
  }

  // VIM-569 |a| |i_CTRL-W|
  @Test
  fun `test delete previous word last after whitespace`() {
    doTest(
      listOf("A", "<C-W>"),
      "<caret>this is a sentence\n",
      "this is a <caret>\n",
      Mode.INSERT,
    )
  }

  // VIM-513 |A| |i_CTRL-W|
  @Test
  fun `test delete previous word eol`() {
    doTest(
      listOf("A", "<C-W>"),
      "<caret>\$variable\n",
      "$<caret>\n",
      Mode.INSERT,
    )
  }

  // VIM-112 |i| |i_CTRL-W|
  @Test
  fun `test insert delete previous word`() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "one two three" + "<C-W>"),
      "hello\n" + "<caret>\n",
    )
    assertState("hello\n" + "one two \n")
  }

  @Test
  fun `test insert delete previous word action`() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "<C-W>" + "<ESC>"),
      "one tw<caret>o three<caret> four   <caret>\n",
    )
    assertState("one<caret> o<caret> <caret> \n")
  }
}
