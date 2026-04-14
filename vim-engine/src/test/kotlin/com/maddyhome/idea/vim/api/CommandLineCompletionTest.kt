/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CommandLineCompletionTest {

  @Test
  fun `test nextMatch returns first match`() {
    val completion = CommandLineCompletion("edit ", 5, listOf("foo.txt", "bar.txt"))
    assertEquals("foo.txt", completion.nextMatch())
    assertEquals(0, completion.currentIndex)
  }

  @Test
  fun `test nextMatch cycles through all matches`() {
    val completion = CommandLineCompletion("edit ", 5, listOf("aaa", "bbb", "ccc"))
    assertEquals("aaa", completion.nextMatch())
    assertEquals("bbb", completion.nextMatch())
    assertEquals("ccc", completion.nextMatch())
    assertEquals("aaa", completion.nextMatch())
  }

  @Test
  fun `test nextMatch returns null for empty matches`() {
    val completion = CommandLineCompletion("edit ", 5, emptyList())
    assertNull(completion.nextMatch())
  }

  @Test
  fun `test previousMatch returns last match`() {
    val completion = CommandLineCompletion("edit ", 5, listOf("aaa", "bbb", "ccc"))
    assertEquals("ccc", completion.previousMatch())
    assertEquals(2, completion.currentIndex)
  }

  @Test
  fun `test previousMatch cycles backwards`() {
    val completion = CommandLineCompletion("edit ", 5, listOf("aaa", "bbb", "ccc"))
    assertEquals("ccc", completion.previousMatch())
    assertEquals("bbb", completion.previousMatch())
    assertEquals("aaa", completion.previousMatch())
    assertEquals("ccc", completion.previousMatch())
  }

  @Test
  fun `test previousMatch returns null for empty matches`() {
    val completion = CommandLineCompletion("edit ", 5, emptyList())
    assertNull(completion.previousMatch())
  }

  @Test
  fun `test forward then backward`() {
    val completion = CommandLineCompletion("edit ", 5, listOf("aaa", "bbb", "ccc"))
    assertEquals("aaa", completion.nextMatch())
    assertEquals("bbb", completion.nextMatch())
    assertEquals("aaa", completion.previousMatch())
  }

  @Test
  fun `test single match cycles to itself`() {
    val completion = CommandLineCompletion("edit ", 5, listOf("only.txt"))
    assertEquals("only.txt", completion.nextMatch())
    assertEquals("only.txt", completion.nextMatch())
  }

  @Test
  fun `test expectedText tracks applied text`() {
    val completion = CommandLineCompletion("edit ", 5, listOf("foo.txt"))
    assertEquals("edit ", completion.expectedText)

    completion.updateExpectedText("edit foo.txt")
    assertEquals("edit foo.txt", completion.expectedText)
  }

  @Test
  fun `test originalText is preserved`() {
    val completion = CommandLineCompletion("edit fo", 5, listOf("foo.txt"))
    completion.nextMatch()
    completion.updateExpectedText("edit foo.txt")
    assertEquals("edit fo", completion.originalText)
  }

  @Test
  fun `test completionStart offset`() {
    val completion = CommandLineCompletion("edit fo", 5, listOf("foo.txt"))
    assertEquals(5, completion.completionStart)
  }

  @Test
  fun `test displayNames strips directory prefix`() {
    val completion = CommandLineCompletion("edit dir/", 5, listOf("dir/alpha.txt", "dir/beta.txt"))
    assertEquals(listOf("alpha.txt", "beta.txt"), completion.displayNames)
  }

  @Test
  fun `test displayNames preserves names without directory`() {
    val completion = CommandLineCompletion("edit ", 5, listOf("foo.txt", "bar.txt"))
    assertEquals(listOf("foo.txt", "bar.txt"), completion.displayNames)
  }

  @Test
  fun `test displayNames strips nested directory prefix`() {
    val completion = CommandLineCompletion("edit src/main/", 5, listOf("src/main/java/", "src/main/kotlin/"))
    assertEquals(listOf("java/", "kotlin/"), completion.displayNames)
  }

  @Test
  fun `test displayNames with partial file prefix`() {
    val completion = CommandLineCompletion("edit dir/al", 5, listOf("dir/alpha.txt"))
    assertEquals(listOf("alpha.txt"), completion.displayNames)
  }
}
