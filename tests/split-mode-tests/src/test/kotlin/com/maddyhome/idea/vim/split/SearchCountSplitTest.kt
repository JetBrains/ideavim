/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Test

/**
 * The search count is computed in the background and shown in the output panel when it's ready. Unit tests count
 * synchronously, so this is where the background path runs for real.
 */
class SearchCountSplitTest : IdeaVimStarterTestBase() {

  @Test
  fun `search shows the match count`() {
    openFile(createFile("src/SearchCount.txt", "foo\nbar foo\nbaz\nfoo\n"))

    typeVim("/foo\n")
    assertCaretAtLine(2)
    assertSearchCount("[2/3]")

    typeVim("n")
    assertCaretAtLine(4)
    assertSearchCount("[3/3]")
  }

  @Test
  fun `search count of a large file is shown once it is counted`() {
    val lines = (0 until 200_000).joinToString("\n", postfix = "\n") { i ->
      if (i == 50_000 || i == 150_000) "[$i] needle" else "[$i] LogTemp: Display: message payload lorem ipsum"
    }
    openFile(createFile("src/SearchCountLarge.txt", lines))

    typeVim("/needle\n")
    assertCaretAtLine(50_001)
    assertSearchCount("[1/2]", timeoutMs = 30_000)

    typeVim("n")
    assertCaretAtLine(150_001)
    assertSearchCount("[2/2]", timeoutMs = 30_000)
  }
}
