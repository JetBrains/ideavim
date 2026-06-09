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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(
  reason = SkipNeovimReason.PLUGIN,
  description = "youcompleteme is an IdeaVim extension driving the IntelliJ completion lookup; no Neovim equivalent",
)
class YouCompleteMeTest : VimJavaTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("youcompleteme")
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
  fun `test Tab moves the selection down in the completion lookup`() {
    configureByJavaText(text)
    typeText("A") // enter insert mode at the end of the "foo" line, caret right after the prefix
    completeBasic()

    val before = selectedLookupString()
    typeText("<Tab>")
    val after = selectedLookupString()

    assertNotNull(activeLookup(), "Lookup should stay open while cycling with Tab")
    assertNotEquals(before, after, "<Tab> should move the selection to the next completion item")
  }

  @Test
  fun `test Shift-Tab returns to the previous completion item`() {
    configureByJavaText(text)
    typeText("A")
    completeBasic()

    val first = selectedLookupString()
    typeText("<Tab>") // next...
    assertNotEquals(first, selectedLookupString())
    typeText("<S-Tab>") // ...and back

    assertEquals(first, selectedLookupString(), "<S-Tab> should move the selection to the previous item")
  }

  @Test
  fun `test Tab inserts indentation when no completion popup is open`() {
    configureByJavaText(
      """
        |class Foo {
        |  void test() {
        |${c}int x = 1;
        |  }
        |}
      """.trimMargin(),
    )
    typeText("i")
    typeText("<Tab>")

    assertState(Mode.INSERT)
    assertNull(activeLookup(), "No completion popup should be open in this context")
    // Tab must still indent the line rather than being swallowed by the plugin.
    assertState(
      """
        |class Foo {
        |  void test() {
        |    ${c}int x = 1;
        |  }
        |}
      """.trimMargin(),
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

  private fun selectedLookupString(): String? {
    var selected: String? = null
    ApplicationManager.getApplication().invokeAndWait {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      selected = (LookupManager.getActiveLookup(fixture.editor) as? LookupImpl)?.currentItem?.lookupString
    }
    return selected
  }
}
