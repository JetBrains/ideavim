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
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertDeletePreviousWordActionTest : VimTestCase() {
  // VIM-1655
  fun `test deleted word is not yanked`() {
    doTest(
      listOf("yiw", "3wea", "<C-W>", "<ESC>p"),
      """
            I found ${c}it in a legendary land
      """.trimIndent(),
      """
            I found it in a i${c}t land
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test word removed`() {
    doTest(
      listOf("i", "<C-W>"),
      """
            I found${c} it in a legendary land
      """.trimIndent(),
      """
            I ${c} it in a legendary land
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun `test non alpha chars`() {
    doTest(
      listOf("i", "<C-W>", "<C-W>", "<C-W>", "<C-W>"),
      """
            I found (it)${c} in a legendary land
      """.trimIndent(),
      """
            I ${c} in a legendary land
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  fun `test indents and spaces`() {
    doTest(
      listOf("i", "<C-W>", "<C-W>", "<C-W>", "<C-W>"),
      """
            A Discovery
            
                 I${c} found it in a legendary land
      """.trimIndent(),
      """
            A Discovery${c} found it in a legendary land
      """.trimIndent(),
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  @VimBehaviorDiffers(
    """
            If (found) {
               if (it) {
                  legendary
               }
            ${c}
  """
  )
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
      VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-569 |a| |i_CTRL-W|
  fun `test delete previous word dot eol`() {
    doTest(
      listOf("a", "<C-W>"),
      "this is a sentence<caret>.\n", "this is a sentence<caret>\n", VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE
    )
  }

  // VIM-569 |a| |i_CTRL-W|
  fun `test delete previous word last after whitespace`() {
    doTest(
      listOf("A", "<C-W>"),
      "<caret>this is a sentence\n", "this is a <caret>\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-513 |A| |i_CTRL-W|
  fun `test delete previous word eol`() {
    doTest(
      listOf("A", "<C-W>"),
      "<caret>\$variable\n", "$<caret>\n", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE
    )
  }

  // VIM-112 |i| |i_CTRL-W|
  fun `test insert delete previous word`() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "one two three" + "<C-W>"),
      "hello\n" + "<caret>\n"
    )
    assertState("hello\n" + "one two \n")
  }

  fun `test insert delete previous word action`() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "<C-W>" + "<ESC>"),
      "one tw<caret>o three<caret> four   <caret>\n"
    )
    assertState("one<caret> o<caret> <caret> \n")
  }
}
