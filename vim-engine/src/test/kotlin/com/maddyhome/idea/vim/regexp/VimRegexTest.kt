/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.VimEditor
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VimRegexTest {

  @Test
  fun `test single word contains match in editor`() {
    assertContainsMatchIn(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "dolor",
      true
    )
  }

  @Test
  fun `test single word does not contain match in editor`() {
    assertContainsMatchIn(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "IdeaVim",
      false
    )
  }

  private fun assertContainsMatchIn(text: CharSequence, pattern: String, expectedResult : Boolean) {
    val editor = buildEditor(text)
    val regex = VimRegex(pattern)
    val matchResult = regex.containsMatchIn(editor)
    assertEquals(expectedResult, matchResult)
  }

  private fun buildEditor(text: CharSequence) : VimEditor {
    return VimEditorMock(text)
  }
}