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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertNull

/**
 * Tests for cycling through the IntelliJ code completion lookup from Vim's insert mode.
 *
 * Vim's native insert-mode completion keys are `<C-N>` (next) and `<C-P>` (previous). In IdeaVim these
 * are handled by [com.maddyhome.idea.vim.action.window.LookupDownAction] and
 * [com.maddyhome.idea.vim.action.window.LookupUpAction], which move the selection in the active lookup
 * (the IDE's autocomplete popup) when one is open.
 *
 * This is the foundation that a "Tab cycles completions" plugin (YouCompleteMe / SuperTab style) would
 * build on. Note that `<Tab>` itself is part of the default `lookupkeys` option, so while the popup is
 * open the IDE — not IdeaVim — receives Tab (it accepts the current item rather than cycling). Making Tab
 * cycle therefore requires removing `<Tab>` from `lookupkeys` and mapping it to the same down/up logic.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "IntelliJ code completion lookup has no Neovim equivalent that can be driven via the RPC harness",
)
class CompletionTest : VimJavaTestCase() {

  @BeforeEach
  fun useDirectToVim(testInfo: TestInfo) {
    Checks.keyHandler = Checks.KeyHandlerMethod.DIRECT_TO_VIM
  }

  // Three members sharing only the "foo" prefix, so completing "foo" keeps the lookup open with several
  // candidates (no extra common prefix is inserted) and we have something to cycle through.
  private val text = """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    foo${c}
        |  }
        |}
  """.trimMargin()

  @Test
  fun `test code completion lookup opens with multiple candidates`() {
    configureByJavaText(text)
    typeText("A") // enter insert mode at the end of the "foo" line, caret right after the prefix
    completeBasic()

    assertState(Mode.INSERT)
    assertNotNull(activeLookup(), "Expected an active completion lookup")
    assertTrue(
      lookupStrings().containsAll(listOf("fooBar", "fooBaz", "fooLong")),
      "Expected the lookup to offer fooBar/fooBaz/fooLong but was ${lookupStrings()}",
    )
  }

  @Test
  fun `test Ctrl-N moves the selection down in the completion lookup`() {
    configureByJavaText(text)
    typeText("A")
    completeBasic()

    val before = selectedLookupString()
    typeText("<C-N>")
    val after = selectedLookupString()

    assertNotNull(activeLookup(), "Lookup should stay open while cycling")
    assertNotEquals(before, after, "<C-N> should move the selection to a different item")
  }

  @Test
  fun `test Ctrl-P returns to the previous completion item`() {
    configureByJavaText(text)
    typeText("A")
    completeBasic()

    val first = selectedLookupString()
    typeText("<C-N>") // move down...
    assertNotEquals(first, selectedLookupString())
    typeText("<C-P>") // ...and back up

    assertEquals(first, selectedLookupString(), "<C-P> should undo the <C-N> movement")
  }

  @Test
  fun `test Ctrl-E closes completion`() {
    configureByJavaText(text)
    typeText("A")
    completeBasic()
    typeText("<C-E>")
    assertNull(activeLookup(), "Lookup should be closed after pressing <C-E>")
    assertState(text)
  }

  @Test
  fun `test Ctrl-Y accepts completion`() {
    configureByJavaText(text)
    typeText("A")
    completeBasic()
    typeText("<C-Y>")
    assertNull(activeLookup(), "Lookup should be closed after pressing <C-E>")
    assertState(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    fooBar();${c}
        |  }
        |}
  """.trimMargin()

    )
  }

  private fun completeBasic() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.completeBasic()
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
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
