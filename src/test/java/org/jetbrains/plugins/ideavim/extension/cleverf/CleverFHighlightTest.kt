/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.cleverf

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

/**
 * Tests for clever-f's `g:clever_f_mark_char`: the characters the search can jump to on the cursor line are
 * highlighted until the search ends.
 */
class CleverFHighlightTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("clever-f")
  }

  @Test
  fun `test f highlights all matches on the line`() {
    configureByText("${c}xa ya za")
    typeText("fa")
    assertHighlightedOffsets(1, 4, 7)
  }

  @Test
  fun `test f does not highlight matches on other lines`() {
    configureByText("${c}xa\na")
    typeText("fa")
    assertHighlightedOffsets(1)
  }

  @Test
  fun `test f highlights the line it jumped to`() {
    configureByText("${c}x\na a")
    typeText("fa")
    assertHighlightedOffsets(2, 4)
  }

  @Test
  fun `test f highlights matches when there is no match to jump to`() {
    configureByText("xa ${c}y")
    typeText("fa")
    assertHighlightedOffsets(1)
  }

  @Test
  fun `test t highlights the characters before matches`() {
    configureByText("${c}xa ya")
    typeText("ta")
    assertHighlightedOffsets(0, 3)
  }

  @Test
  fun `test T highlights the characters after matches`() {
    configureByText("xa y${c}a")
    typeText("Ta")
    assertHighlightedOffsets(2)
  }

  @Test
  fun `test highlight stays while repeating`() {
    configureByText("${c}xa ya za")
    typeText("faf")
    assertHighlightedOffsets(1, 4, 7)
  }

  @Test
  fun `test highlight in visual mode`() {
    configureByText("${c}xa ya za")
    typeText("vfa")
    assertHighlightedOffsets(1, 4, 7)
  }

  @Test
  fun `test other motion removes highlight`() {
    configureByText("${c}xa ya za")
    typeText("fal")
    assertHighlightedOffsets()
  }

  @Test
  fun `test text change removes highlight`() {
    configureByText("${c}xa ya za")
    typeText("fax")
    assertHighlightedOffsets()
  }

  @Test
  fun `test entering insert mode removes highlight`() {
    configureByText("${c}xa ya za")
    typeText("fai")
    assertHighlightedOffsets()
  }

  @Test
  fun `test reset mapping removes highlight`() {
    configureByText("${c}xa ya za")
    enterCommand("nmap Q <Plug>(clever-f-reset)")
    typeText("faQ")
    assertHighlightedOffsets()
  }

  @Test
  fun `test disabling the extension removes highlight`() {
    configureByText("${c}xa ya za")
    typeText("fa")
    enterCommand("set noclever-f")
    assertHighlightedOffsets()
  }

  @Test
  fun `test no highlight in operator-pending mode`() {
    configureByText("${c}xa ya za")
    typeText("dfa")
    assertHighlightedOffsets()
  }

  @Test
  fun `test no highlight with clever_f_mark_char disabled`() {
    configureByText("${c}xa ya za")
    enterCommand("let g:clever_f_mark_char = 0")
    typeText("fa")
    assertHighlightedOffsets()
  }

  private fun assertHighlightedOffsets(vararg offsets: Int) {
    val highlighted = fixture.editor.markupModel.allHighlighters.map { it.startOffset }.sorted()
    assertEquals(offsets.toList(), highlighted)
  }
}
