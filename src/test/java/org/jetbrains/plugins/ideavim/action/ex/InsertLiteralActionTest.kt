/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class InsertLiteralActionTest : VimExTestCase() {
  @Test
  fun `test CTRL-V inserts literal character`() {
    typeText(":<C-V>123<C-V>080")
    assertExText("{P")
  }

  @Test
  fun `test CTRL-V inserts octal literal character`() {
    typeText(":<C-V>o123")
    assertExText("S")
  }

  @Test
  fun `test CTRL-V inserts unicode literal character`() {
    typeText(":<C-V>u00A9")
    assertExText("©")
  }

  @Test
  fun `test CTRL-Q inserts literal character`() {
    typeText(":<C-Q>123<C-Q>080")
    assertExText("{P")
  }

  @Test
  fun `test CTRL-Q inserts octal literal character`() {
    typeText(":<C-Q>o123")
    assertExText("S")
  }

  @Test
  fun `test CTRL-Q inserts unicode literal character`() {
    typeText(":<C-Q>u00a9")
    assertExText("©")
  }

  @Test
  fun `test insert literal character before existing text`() {
    typeText(":set<Home><C-V>u00A9")
    assertExText("©set")
    assertExOffset(1)
  }

  @Test
  fun `test insert literal Escape character`() {
    typeText(":normal I[<C-V><Esc>A]<C-V><Esc>")
    assertExText("normal I[" + Char(27) + "A]" + Char(27))
  }

  @Test
  fun `test insert literal newline character`() {
    // CR should be \n but Vim treats that as a NULL char, so we insert \r
    typeText(":nmap p <C-V><CR>")
    assertExText("nmap p \r")
  }

  @Test
  fun `test insert literal control character`() {
    typeText(":nmap p <C-V><C-D>")
    assertExText("nmap p " + Char(4))
  }

  @Test
  fun `test insert literal tab character`() {
    typeText(":nmap p <C-V><C-I>")
    assertExText("nmap p \t")
  }

  @Test
  fun `test insert literal CTRL-V character`() {
    typeText(":nmap p <C-V><C-V>")
    assertExText("nmap p " + Char(22))
  }

  @Disabled
  @Test
  fun `test insert literal CTRL-C character`() {
    // TODO: IdeaVim handles <C-C> before handling digraphs/literals
//    typeText(":nmap p <C-V><C-C>")
//    assertExText("nmap p " + Char(3))
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test prompt to insert literal character`() {
    typeText(":<C-V>")
    assertRenderedExText("^")
    assertExOffset(0)
  }

  @Test
  fun `test prompt while inserting literal character`() {
    typeText(":<C-V>o")
    assertRenderedExText("^")
    assertExOffset(0)

    typeText("1")
    assertRenderedExText("^")
    assertExOffset(0)

    typeText("2")
    assertRenderedExText("^")
    assertExOffset(0)

    typeText("3")
    assertExText("S")
    assertRenderedExText("S")
    assertExOffset(1)
  }
}
