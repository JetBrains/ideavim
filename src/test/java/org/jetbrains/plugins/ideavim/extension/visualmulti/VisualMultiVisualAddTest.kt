/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Visual Add (`\\a`): the Visual selection becomes regions
 */
class VisualMultiVisualAddTest : VisualMultiTestCase() {

  @Test
  fun `test characterwise selection becomes a region`() {
    configureByText("${c}qwe asd qwe")

    typeText("vll" + "<Bslash><Bslash>a")

    assertState("${s}qwe$se asd qwe")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test n finds next occurrence of added region`() {
    configureByText("${c}qwe asd qwe")

    typeText("vll" + "<Bslash><Bslash>a" + "n")

    assertState("${s}qwe$se asd ${s}qwe$se")
  }

  @Test
  fun `test linewise selection becomes a region on each line`() {
    configureByText(
      """a${c}bc
      |def
      |ghi
      """.trimMargin(),
    )

    typeText("Vj" + "<Bslash><Bslash>a")

    assertState(
      """${s}abc$se
      |${s}def$se
      |ghi
      """.trimMargin(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test linewise selection skips empty lines`() {
    configureByText(
      """a${c}bc
      |
      |ghi
      """.trimMargin(),
    )

    typeText("Vjj" + "<Bslash><Bslash>a")

    assertState(
      """${s}abc$se
      |
      |${s}ghi$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test blockwise selection becomes a region on each line`() {
    configureByText(
      """a${c}bcd
      |efgh
      """.trimMargin(),
    )

    typeText("<C-v>jl" + "<Bslash><Bslash>a")

    assertState(
      """a${s}bc${se}d
      |e${s}fg${se}h
      """.trimMargin(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test regions are changed together`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("Vj" + "<Bslash><Bslash>a" + "cx<Esc>")

    assertState(
      """${c}x
      |${c}x
      """.trimMargin(),
    )
  }

  @Test
  fun `test visual add starts session`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("Vj" + "<Bslash><Bslash>a")
    assertEquals(2, fixture.editor.caretModel.caretCount)

    // Inside the session, <Esc> in Visual mode exits it and keeps one cursor
    typeText("<Esc>")
    assertEquals(1, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test VM_maps changes Visual Add key`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Visual Add'] = '<C-a>'")

    typeText("Vj" + "<C-a>")

    assertState(
      """${s}abc$se
      |${s}def$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test Visual Add plug mapping`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("Vj" + "<Plug>(VM-Visual-Add)")

    assertState(
      """${s}abc$se
      |${s}def$se
      """.trimMargin(),
    )
  }
}
