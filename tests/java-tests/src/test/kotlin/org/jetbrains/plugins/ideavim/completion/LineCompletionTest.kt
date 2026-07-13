/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.completion

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for Vim's insert-mode whole-line completion, `<C-X><C-L>` (`:help i_CTRL-X_CTRL-L`).
 *
 * In Vim, `<C-X><C-L>` searches the buffer for whole lines that begin with the text before the caret and
 * offers them as completions; repeating `<C-L>` cycles to the next matching line.
 *
 * These tests assert the IdeaVim (IntelliJ-lookup) behaviour model: `<C-X><C-L>` opens the IDE lookup
 * populated with the matching whole lines, and each subsequent `<C-L>` only moves the selection within
 * that lookup — the buffer text is NOT modified until an item is accepted (mirroring how the existing
 * `<C-N>`/`<C-P>` completion works, see [CompletionTest]).
 *
 * NOTE: `<C-X><C-L>` line completion is not implemented yet, so these tests currently fail — they are
 * written first to pin down the intended behaviour.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "IntelliJ code completion lookup has no Neovim equivalent that can be driven via the RPC harness",
)
class LineCompletionTest : VimJavaTestCase() {

  @BeforeEach
  fun useDirectToVim(testInfo: TestInfo) {
    Checks.keyHandler = Checks.KeyHandlerMethod.DIRECT_TO_VIM
  }

  // Three whole lines share the "return " prefix, plus a fourth line where we type that prefix and trigger
  // completion. Line completion should therefore keep the lookup open with the three matching whole lines.
  private val text = """
        |fun compute(): Int {
        |  return result
        |  return value
        |  return count
        |  return ${c}
        |}
  """.trimMargin()

  // The whole lines the lookup should offer (Vim inserts the entire matching line).
  private val matchingLines = listOf("return result", "return value", "return count")

  @Test
  fun `test Ctrl-X enters the CTRL-X completion sub-mode`() {
    configureByText("\n")
    enterCommand("inoremap <expr> q mode(1)")
    typeText("i<C-X>q<Esc>")
    assertState("ix\n")
  }

  @Test
  fun `test Ctrl-X Ctrl-L opens a lookup listing the matching whole lines`() {
    configureByJavaText(text)
    typeText("i") // enter insert mode with the caret right after the "return " prefix
    typeText("<C-X><C-L>")

    assertState(Mode.INSERT)
    assertNotNull(activeLookup(), "Expected <C-X><C-L> to open a completion lookup")
    assertTrue(
      lookupStrings().containsAll(matchingLines),
      "Expected the lookup to offer the matching whole lines $matchingLines but was ${lookupStrings()}",
    )
  }

  @Test
  fun `test Ctrl-X Ctrl-L selects a matching line without modifying the buffer`() {
    configureByJavaText(text)
    typeText("i")
    typeText("<C-X><C-L>")

    assertNotNull(activeLookup(), "Lookup should be open")
    assertTrue(
      matchingLines.contains(selectedLookupString()),
      "Selected item ${selectedLookupString()} should be one of the matching lines $matchingLines",
    )
    // Lookup-selection model: nothing is inserted until the item is accepted, so the buffer is unchanged.
    assertState(text)
  }

  @Test
  fun `test each Ctrl-L moves the selection to another matching line and keeps the lookup open`() {
    configureByJavaText(text)
    typeText("i")
    typeText("<C-X><C-L>")

    // State after the initial <C-X><C-L>.
    assertState(Mode.INSERT)
    assertNotNull(activeLookup(), "Lookup should be open after <C-X><C-L>")
    var previous = selectedLookupString()
    assertNotNull(previous, "There should be a selected item")

    // Press <C-L> for each remaining match; the selection must change every time and the lookup must stay
    // open, while the buffer text remains untouched.
    repeat(matchingLines.size - 1) {
      typeText("<C-L>")

      assertState(Mode.INSERT)
      assertNotNull(activeLookup(), "Lookup should stay open while cycling with <C-L>")
      assertState(text)
      val current = selectedLookupString()
      assertNotEquals(previous, current, "Each <C-L> should move the selection to a different line")
      previous = current
    }
  }

  @Test
  fun `test repeating Ctrl-L visits every matching line`() {
    configureByJavaText(text)
    typeText("i")
    typeText("<C-X><C-L>")

    val visited = linkedSetOf<String>()
    selectedLookupString()?.let { visited.add(it) }
    repeat(matchingLines.size - 1) {
      typeText("<C-L>")
      selectedLookupString()?.let { visited.add(it) }
    }

    assertTrue(
      visited.containsAll(matchingLines),
      "Cycling with <C-L> should visit every matching line $matchingLines but only saw $visited",
    )
  }

  private fun activeLookup(): LookupImpl? {
    var lookup: LookupImpl? = null
    ApplicationManager.getApplication().invokeAndWait {
      lookup = LookupManager.getActiveLookup(fixture.editor) as? LookupImpl
    }
    return lookup
  }

  private fun lookupStrings(): List<String> = fixture.lookupElementStrings ?: emptyList()

  private fun selectedLookupString(): String? {
    var selected: String? = null
    ApplicationManager.getApplication().invokeAndWait {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      selected = (LookupManager.getActiveLookup(fixture.editor) as? LookupImpl)?.currentItem?.lookupString
    }
    return selected
  }
}
