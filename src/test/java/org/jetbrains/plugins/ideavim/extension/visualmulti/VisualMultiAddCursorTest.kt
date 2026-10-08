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
 * Add Cursor Down and Add Cursor Up (`<C-Down>` and `<C-Up>`)
 */
class VisualMultiAddCursorTest : VisualMultiTestCase() {

  @Test
  fun `test ctrl-down adds cursor below`() {
    configureByText(
      """a${c}bc
      |def
      |ghi
      """.trimMargin(),
    )

    typeText("<C-Down>")

    assertState(
      """a${c}bc
      |d${c}ef
      |ghi
      """.trimMargin(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test ctrl-up adds cursor above`() {
    configureByText(
      """abc
      |def
      |g${c}hi
      """.trimMargin(),
    )

    typeText("<C-Up>")

    assertState(
      """abc
      |d${c}ef
      |g${c}hi
      """.trimMargin(),
    )
  }

  @Test
  fun `test ctrl-down again adds cursor below the last one`() {
    configureByText(
      """a${c}bc
      |def
      |ghi
      |jkl
      """.trimMargin(),
    )

    typeText("<C-Down><C-Down>")

    assertState(
      """a${c}bc
      |d${c}ef
      |g${c}hi
      |jkl
      """.trimMargin(),
    )
  }

  @Test
  fun `test ctrl-up again adds cursor above the last one`() {
    configureByText(
      """abc
      |def
      |g${c}hi
      """.trimMargin(),
    )

    typeText("<C-Up><C-Up>")

    assertState(
      """a${c}bc
      |d${c}ef
      |g${c}hi
      """.trimMargin(),
    )
  }

  @Test
  fun `test count adds that many cursors`() {
    configureByText(
      """a${c}bc
      |def
      |ghi
      |jkl
      """.trimMargin(),
    )

    typeText("2<C-Down>")

    assertState(
      """a${c}bc
      |d${c}ef
      |g${c}hi
      |jkl
      """.trimMargin(),
    )
  }

  @Test
  fun `test ctrl-up does not add cursor where there is one`() {
    configureByText(
      """a${c}bc
      |def
      |ghi
      """.trimMargin(),
    )

    typeText("<C-Down><C-Up>")

    assertEquals(2, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test cursors edit all lines`() {
    configureByText(
      """a${c}bc
      |def
      |ghi
      """.trimMargin(),
    )

    typeText("<C-Down>" + "x")

    assertState(
      """a${c}c
      |d${c}f
      |ghi
      """.trimMargin(),
    )
  }

  // Shorter and empty lines

  @Test
  fun `test shorter line is skipped`() {
    configureByText(
      """ab${c}c
      |d
      |ghi
      """.trimMargin(),
    )

    typeText("<C-Down>")

    assertState(
      """ab${c}c
      |d
      |gh${c}i
      """.trimMargin(),
    )
  }

  @Test
  fun `test line as long as the column is skipped`() {
    configureByText(
      """ab${c}c
      |ab
      |abc
      """.trimMargin(),
    )

    typeText("<C-Down>")

    assertState(
      """ab${c}c
      |ab
      |ab${c}c
      """.trimMargin(),
    )
  }

  @Test
  fun `test count does not count skipped lines`() {
    configureByText(
      """ab${c}c
      |d
      |ghi
      |jkl
      """.trimMargin(),
    )

    typeText("2<C-Down>")

    assertState(
      """ab${c}c
      |d
      |gh${c}i
      |jk${c}l
      """.trimMargin(),
    )
  }

  @Test
  fun `test empty line is skipped`() {
    configureByText(
      """ab${c}c
      |
      |ghi
      """.trimMargin(),
    )

    typeText("<C-Down>")

    assertState(
      """ab${c}c
      |
      |gh${c}i
      """.trimMargin(),
    )
  }

  @Test
  fun `test empty line is not skipped in first column`() {
    // An empty line is not shorter than the first column
    configureByText(
      """${c}abc
      |
      |ghi
      """.trimMargin(),
   )

    typeText("<C-Down>")

    assertState(
      """${c}abc
      |$c
      |ghi
      """.trimMargin(),
    )
  }

  @Test
  fun `test VM_skip_empty_lines skips empty line in first column`() {
    configureByText(
      """${c}abc
      |
      |ghi
      """.trimMargin(),
    )
    enterCommand("let g:VM_skip_empty_lines = 1")

    typeText("<C-Down>")

    assertState(
      """${c}abc
      |
      |${c}ghi
      """.trimMargin(),
    )
  }

  @Test
  fun `test line with only spaces is not empty`() {
    configureByText(
      """${c}abc
      |${"   "}
      |ghi
      """.trimMargin(),
    )
    enterCommand("let g:VM_skip_empty_lines = 1")

    typeText("<C-Down>")

    assertState(
      """${c}abc
      |$c${"   "}
      |ghi
      """.trimMargin(),
    )
  }

  @Test
  fun `test VM_skip_empty_lines needs VM_skip_shorter_lines`() {
    configureByText(
      """${c}abc
      |
      |ghi
      """.trimMargin(),
    )
    enterCommand("let g:VM_skip_shorter_lines = 0")
    enterCommand("let g:VM_skip_empty_lines = 1")

    typeText("<C-Down>")

    assertState(
      """${c}abc
      |$c
      |ghi
      """.trimMargin(),
    )
  }

  @Test
  fun `test column is kept after shorter line`() {
    configureByText(
      """ab${c}c
      |d
      |ghi
      """.trimMargin(),
    )
    enterCommand("let g:VM_skip_shorter_lines = 0")

    typeText("<C-Down><C-Down>")

    assertState(
      """ab${c}c
      |${c}d
      |gh${c}i
      """.trimMargin(),
    )
  }

  @Test
  fun `test VM_skip_shorter_lines zero adds cursor on shorter line`() {
    configureByText(
      """ab${c}c
      |d
      |ghi
      """.trimMargin(),
    )
    enterCommand("let g:VM_skip_shorter_lines = 0")

    typeText("<C-Down>")

    assertState(
      """ab${c}c
      |${c}d
      |ghi
      """.trimMargin(),
    )
  }

  @Test
  fun `test no cursor is added when all lines below are shorter`() {
    val before = """ab${c}c
      |d
      |e
    """.trimMargin()
    configureByText(before)

    typeText("<C-Down>")

    assertState(before)
    assertEquals(1, fixture.editor.caretModel.caretCount)
  }

  // First and last line

  @Test
  fun `test ctrl-down on last line does nothing`() {
    val before = """abc
      |d${c}ef
    """.trimMargin()
    configureByText(before)

    typeText("<C-Down>")

    assertState(before)
  }

  @Test
  fun `test ctrl-up on first line does nothing`() {
    val before = """a${c}bc
      |def
    """.trimMargin()
    configureByText(before)

    typeText("<C-Up>")

    assertState(before)
  }

  // Session and mappings

  @Test
  fun `test ctrl-down starts session`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("<C-Down>")
    assertEquals(2, fixture.editor.caretModel.caretCount)

    // Inside the session, <Esc> in Visual mode exits it and keeps one cursor
    typeText("v" + "<Esc>")
    assertEquals(1, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test VM_default_mappings zero removes ctrl-down mapping`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )
    reinitAfter("let g:VM_default_mappings = 0")

    typeText("<C-Down>")

    assertEquals(1, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test VM_maps changes Add Cursor Down key`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Add Cursor Down'] = '<M-j>'")

    typeText("<M-j>")

    assertState(
      """a${c}bc
      |d${c}ef
      """.trimMargin(),
    )
  }

  @Test
  fun `test Add Cursor Up plug mapping`() {
    configureByText(
      """abc
      |d${c}ef
      """.trimMargin(),
    )

    typeText("<Plug>(VM-Add-Cursor-Up)")

    assertState(
      """a${c}bc
      |d${c}ef
      """.trimMargin(),
    )
  }
}
