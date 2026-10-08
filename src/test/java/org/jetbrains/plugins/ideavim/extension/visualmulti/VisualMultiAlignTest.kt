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
import org.junit.jupiter.api.Test

/**
 * Align (`\\a`): spaces before the cursors, so that they are all in the same column
 */
class VisualMultiAlignTest : VisualMultiTestCase() {

  @Test
  fun `test align regions`() {
    configureByText(
      """x ${c}foo
      |yyy foo
      """.trimMargin(),
    )

    typeText("<C-n><C-n>" + "<Bslash><Bslash>a")

    assertState(
      """x   ${c}foo
      |yyy ${c}foo
      """.trimMargin(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test align cursors`() {
    configureByText(
      """${c}a b
      |aaa b
      """.trimMargin(),
    )

    typeText("<C-Down>" + "w" + "<Bslash><Bslash>a")

    assertState(
      """a   ${c}b
      |aaa ${c}b
      """.trimMargin(),
    )
  }

  @Test
  fun `test aligned cursors stay`() {
    val before = """a ${c}b
      |a b
    """.trimMargin()
    configureByText(before)

    typeText("<C-Down>" + "<Bslash><Bslash>a")

    assertState(
      """a ${c}b
      |a ${c}b
      """.trimMargin(),
    )
  }

  @Test
  fun `test leader a outside session is still Visual Add`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("Vj" + "<Bslash><Bslash>a")

    assertState(
      """${s}abc$se
      |${s}def$se
      """.trimMargin(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test VM_maps changes Align key`() {
    configureByText(
      """x ${c}foo
      |yyy foo
      """.trimMargin(),
    )
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Align'] = '<C-a>'")

    typeText("<C-n><C-n>" + "<C-a>")

    assertState(
      """x   ${c}foo
      |yyy ${c}foo
      """.trimMargin(),
    )
  }
}
