/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.state.mode.Mode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Visual Cursors (`\\c`): a cursor on each line of the Visual selection
 */
class VisualMultiVisualCursorsTest : VisualMultiTestCase() {

  @Test
  fun `test cursor on each line at selection start column`() {
    configureByText(
      """a${c}bc
      |def
      |ghi
      """.trimMargin(),
    )

    typeText("vjj" + "<Bslash><Bslash>c")

    assertState(
      """a${c}bc
      |d${c}ef
      |g${c}hi
      """.trimMargin(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test selection made upwards starts at its top`() {
    configureByText(
      """abc
      |def
      |g${c}hi
      """.trimMargin(),
    )

    typeText("vkk" + "<Bslash><Bslash>c")

    assertState(
      """a${c}bc
      |d${c}ef
      |g${c}hi
      """.trimMargin(),
    )
  }

  @Test
  fun `test shorter line is skipped`() {
    configureByText(
      """ab${c}c
      |d
      |ghi
      """.trimMargin(),
    )

    typeText("vjj" + "<Bslash><Bslash>c")

    assertState(
      """ab${c}c
      |d
      |gh${c}i
      """.trimMargin(),
    )
  }

  @Test
  fun `test linewise selection puts cursors in first column`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("Vj" + "<Bslash><Bslash>c")

    assertState(
      """${c}abc
      |${c}def
      """.trimMargin(),
    )
  }

  @Test
  fun `test linewise selection skips empty lines`() {
    configureByText(
      """a${c}bc
      |
      |ghi
      """.trimMargin(),
    )

    typeText("Vjj" + "<Bslash><Bslash>c")

    assertState(
      """${c}abc
      |
      |${c}ghi
      """.trimMargin(),
    )
  }

  @Test
  fun `test single line selection leaves one cursor at its start`() {
    configureByText("a${c}bcd")

    typeText("vl" + "<Bslash><Bslash>c")

    assertState("a${c}bcd")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test cursors edit all lines`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("vj" + "<Bslash><Bslash>c" + "x")

    assertState(
      """a${c}c
      |d${c}f
      """.trimMargin(),
    )
  }

  @Test
  fun `test visual cursors start session`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("vj" + "<Bslash><Bslash>c")
    assertEquals(2, fixture.editor.caretModel.caretCount)

    // Inside the session, <Esc> in Visual mode exits it and keeps one cursor
    typeText("v" + "<Esc>")
    assertEquals(1, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test VM_maps changes Visual Cursors key`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Visual Cursors'] = '<C-c>'")

    typeText("vj" + "<C-c>")

    assertState(
      """a${c}bc
      |d${c}ef
      """.trimMargin(),
    )
  }

  @Test
  fun `test Visual Cursors plug mapping`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("vj" + "<Plug>(VM-Visual-Cursors)")

    assertState(
      """a${c}bc
      |d${c}ef
      """.trimMargin(),
    )
  }

  @Test
  fun `test VM_default_mappings zero removes Visual Cursors mapping`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )
    reinitAfter("let g:VM_default_mappings = 0")

    typeText("vj" + "<Bslash><Bslash>c")

    assertEquals(1, fixture.editor.caretModel.caretCount)
  }
}
