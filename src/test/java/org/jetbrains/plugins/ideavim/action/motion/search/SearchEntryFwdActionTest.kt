/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SearchEntryFwdActionTest : VimTestCase() {
  @Test
  fun `test search clears status line`() {
    configureByText("lorem ipsum")
    enterSearch("dolor")  // Shows "pattern not found message"
    assertPluginErrorMessageContains("Pattern not found: dolor")
    typeText("/")  // No <CR>
    assertStatusLineCleared()
  }

  @Test
  fun `search in visual mode`() {
    doTest(
      "v/id<CR>",
      """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin(),
      """Lorem ipsum dolor sit amet,
        |${s}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras ${c}i${se}d tellus in ex imperdiet egestas.
    """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `search in one time visual mode`() {
    doTest(
      "i<C-O>v/id<CR>",
      """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """Lorem ipsum dolor sit amet,
        |${s}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras ${c}i${se}d tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT),
    )
  }

  @Test
  fun `search in one time visual mode from replace`() {
    doTest(
      "R<C-O>v/id<CR>",
      """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """Lorem ipsum dolor sit amet,
        |${s}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras ${c}i${se}d tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.REPLACE),
    )
  }

  @Test
  fun `search in op pending`() {
    doTest(
      "d/id<CR>",
      """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin(),
      """Lorem ipsum dolor sit amet,
        |${c}id tellus in ex imperdiet egestas.
    """.trimMargin(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `search in op pending from one time mode`() {
    doTest(
      "i<C-O>d/id<CR>",
      """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin(),
      """Lorem ipsum dolor sit amet,
        |${c}id tellus in ex imperdiet egestas.
    """.trimMargin(),
      Mode.INSERT,
    )
  }

  @Disabled("Ctrl-o doesn't work yet in select mode")
  @Test
  fun `search in one time from select mode`() {
    doTest(
      "gh<C-O>/id<CR>",
      """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin(),
      """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras ${c}id tellus in ex imperdiet egestas.
    """.trimMargin(),
      Mode.SELECT(SelectionType.CHARACTER_WISE),
    )
  }
}
