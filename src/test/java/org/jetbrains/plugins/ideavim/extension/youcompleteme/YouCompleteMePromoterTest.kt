/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.youcompleteme

import com.intellij.codeInsight.lookup.LookupArranger
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionWrapper
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.actions.TabAction
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.key.VimActionsPromoter
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Verifies the decisive action-ordering logic that makes `<Tab>` cycle the completion lookup with the
 * `youcompleteme` extension enabled.
 *
 * The keystroke-based tests in `tests/java-tests` cannot cover this: the test harness dispatches `<Tab>` by
 * invoking [VimShortcutKeyAction] directly, bypassing both the real IDE
 * [com.intellij.openapi.actionSystem.ActionPromoter] chain and the lookup's own "accept item" action. In the real
 * IDE, when a lookup is open the lookup's accept action competes with [VimShortcutKeyAction] for the Tab shortcut,
 * and [VimActionsPromoter] decides the order.
 *
 * This test exercises [VimActionsPromoter] directly: with a lookup open and `<Tab>` mapped in Insert mode, the
 * promoter must put [VimShortcutKeyAction] first so IdeaVim handles Tab (cycling) instead of the IDE accepting.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class YouCompleteMePromoterTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("youcompleteme")
  }

  @Test
  fun `test promoter puts VimShortcutKeyAction first when lookup is open and Tab is mapped`() {
    configureByText("foo${c}")
    typeText("A") // Insert mode

    showLookup()
    try {
      assertNotNull(activeLookup(), "Test setup: a lookup should be active")

      val vimWrapper = AnActionWrapper(VimShortcutKeyAction())
      // Order mimics the real IDE: the lookup's contextual "accept item" action comes before our action.
      val actions = listOf(acceptItemAction, vimWrapper, TabAction())

      val promoted = promote(actions)

      assertSame(vimWrapper, promoted?.firstOrNull(), "VimShortcutKeyAction should be promoted to run first")
    } finally {
      hideLookup()
    }
  }

  @Test
  fun `test promoter keeps default ordering when no lookup is open`() {
    configureByText("foo${c}")
    typeText("A") // Insert mode, but no lookup

    val vimWrapper = AnActionWrapper(VimShortcutKeyAction())
    val actions = listOf(acceptItemAction, vimWrapper, TabAction())

    val promoted = promote(actions)

    // Without an active lookup the special case does not apply: the contextual action keeps its place and
    // VimShortcutKeyAction is ordered just before TabAction.
    assertSame(acceptItemAction, promoted?.firstOrNull(), "Contextual action should keep priority without a lookup")
    assertTrue(
      promoted!!.indexOf(vimWrapper) < promoted.indexOfFirst { it is TabAction },
      "VimShortcutKeyAction should still be ordered before TabAction",
    )
  }

  private val acceptItemAction = object : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {}
  }

  private fun promote(actions: List<AnAction>): List<AnAction>? {
    val context = SimpleDataContext.builder().add(CommonDataKeys.EDITOR, fixture.editor).build()
    var result: List<AnAction>? = null
    ApplicationManager.getApplication().invokeAndWait {
      result = VimActionsPromoter().promote(actions, context)
    }
    return result
  }

  private fun activeLookup(): LookupImpl? {
    var lookup: LookupImpl? = null
    ApplicationManager.getApplication().invokeAndWait {
      lookup = LookupManager.getActiveLookup(fixture.editor) as? LookupImpl
    }
    return lookup
  }

  private fun showLookup() {
    ApplicationManager.getApplication().invokeAndWait {
      val items: Array<LookupElement> = arrayOf(
        LookupElementBuilder.create("fooBar"),
        LookupElementBuilder.create("fooBaz"),
      )
      val lookup = LookupManager.getInstance(fixture.project)
        .createLookup(fixture.editor, items, "foo", LookupArranger.DefaultArranger()) as LookupImpl
      lookup.showLookup()
    }
  }

  private fun hideLookup() {
    ApplicationManager.getApplication().invokeAndWait {
      LookupManager.hideActiveLookup(fixture.project)
    }
  }
}
