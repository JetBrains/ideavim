/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchEntryFwdActionTest : VimTestCase() {
  @Test
  fun `test search clears status line`() {
    configureByText("lorem ipsum")
    enterSearch("dolor")  // Shows "pattern not found message"
    assertPluginErrorMessage("E486: Pattern not found: dolor")
    typeText("/")  // No <CR>
    assertStatusLineCleared()
  }

  @Test
  fun `test search not found shows only error message on output panel`() {
    configureByText("lorem ipsum dolor sit amet")
    enterSearch("nonexistent")
    val panelText = injector.outputPanel.getCurrentOutputPanel()?.text ?: ""
    assertEquals("E486: Pattern not found: nonexistent", panelText)
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

  @Test
  fun `test escape after search not found closes panel without inserting escape char`() {
    configureByText("lorem ipsum dolor sit amet")
    typeText("/notfound")

    val panel = ExEntryPanel.getOrCreatePanelInstance()
    assertTrue(panel.isActive)

    typeText("<Esc>")

    assertFalse(panel.isActive)
    assertMode(Mode.NORMAL())
    // The panel text should not contain ^[ (escape character written as text)
    assertFalse(panel.text.contains("\u001B"), "Panel text should not contain escape character")
    assertFalse(panel.text.contains("^["), "Panel text should not contain ^[ literal")
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
