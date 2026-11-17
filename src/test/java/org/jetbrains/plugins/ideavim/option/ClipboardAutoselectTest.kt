/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValueSource
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.ClipboardAutoselectChangeListener
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.annotations.TestWithPrimaryClipboard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClipboardAutoselectTest : VimTestCase() {
  private fun platformUpdatesPrimary() = Registry.`is`(PRIMARY_REGISTRY_KEY)

  private fun primaryRegisterType(): SelectionType? {
    val editor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    return injector.registerGroup.getRegister(editor, context, '*')?.type
  }

  @TestWithPrimaryClipboard
  fun `test visual selection is published to PRIMARY with autoselect`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("set clipboard=autoselect")
    typeText("vll")
    assertEquals("Lor", primarySelection.currentText)
    assertEquals(SelectionType.CHARACTER_WISE, primaryRegisterType())
  }

  @TestWithPrimaryClipboard
  fun `test linewise visual selection is published to PRIMARY with its trailing newline`() {
    configureByText("${c}Lorem\nipsum\ndolor")
    enterCommand("set clipboard=autoselect")
    typeText("Vj")
    assertEquals("Lorem\nipsum\n", primarySelection.currentText)
    assertEquals(SelectionType.LINE_WISE, primaryRegisterType())
  }

  // The text the platform puts into PRIMARY for the same selection: every caret, not the primary one only
  @TestWithPrimaryClipboard
  fun `test blockwise visual selection is published to PRIMARY with every line`() {
    configureByText("a${c}bc\ndef\nghi")
    enterCommand("set clipboard=autoselect")
    typeText("<C-V>jjl")
    assertEquals("bc\nef\nhi", primarySelection.currentText)
    assertEquals(SelectionType.BLOCK_WISE, primaryRegisterType())
  }

  @TestWithPrimaryClipboard
  fun `test visual selection is not published to PRIMARY without autoselect`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("set clipboard-=autoselect")
    typeText("vll")
    assertEquals("", primarySelection.currentText)
  }

  @Test
  fun `test visual selection is not published where there is no PRIMARY`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("set clipboard=autoselect")
    typeText("vll")
    assertEquals("", primarySelection.currentText)
  }

  // The operator changes the selection on its way, which must not reach PRIMARY
  @TestWithPrimaryClipboard
  fun `test operator on visual selection keeps the published selection`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("set clipboard=autoselect")
    typeText("vll", "d")
    assertEquals("Lor", primarySelection.currentText)
  }

  // Autoselect is not a yank: `vp` with unnamed puts the last yank, not the selection it replaces
  @TestWithPrimaryClipboard
  fun `test visual put with unnamed puts the last yank and not the published selection`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("set clipboard=unnamed,autoselect")
    typeText("yiw", "w", "viw", "p")
    assertState("Lorem Lore${c}m")
  }

  @Test
  fun `test platform PRIMARY update follows autoselect`() {
    configureByText("Lorem ipsum")
    assertTrue(platformUpdatesPrimary())

    withSystemSelection {
      enterCommand("set clipboard-=autoselect")
      assertFalse(platformUpdatesPrimary())

      enterCommand("set clipboard+=autoselect")
      assertTrue(platformUpdatesPrimary())
    }
  }

  @Test
  fun `test resetting clipboard restores platform PRIMARY update`() {
    configureByText("Lorem ipsum")
    withSystemSelection {
      enterCommand("set clipboard-=autoselect")
      assertFalse(platformUpdatesPrimary())

      enterCommand("set clipboard&")
      assertTrue(platformUpdatesPrimary())
    }
  }

  // Restored to the default, not saved as a user setting, so it's not written to the IDE settings
  @Test
  fun `test restored platform PRIMARY update is not stored`() {
    configureByText("Lorem ipsum")
    withSystemSelection {
      enterCommand("set clipboard-=autoselect")
      enterCommand("set clipboard+=autoselect")
      assertFalse(Registry.get(PRIMARY_REGISTRY_KEY).isChangedFromDefault())
    }
  }

  // The override is saved with the settings, so the next start sees it as the current value
  @Test
  fun `test platform PRIMARY update disabled by an earlier session is restored`() {
    configureByText("Lorem ipsum")
    val registryValue = Registry.get(PRIMARY_REGISTRY_KEY)
    registryValue.setValue("false", RegistryValueSource.SYSTEM)
    try {
      withSystemSelection {
        enterCommand("set clipboard-=autoselect")
        enterCommand("set clipboard+=autoselect")
        assertTrue(platformUpdatesPrimary())
      }
    } finally {
      registryValue.resetToDefault()
    }
  }

  @Test
  fun `test platform PRIMARY update is kept where there is no PRIMARY`() {
    configureByText("Lorem ipsum")
    ClipboardAutoselectChangeListener.withSystemSelectionSupported(false) {
      enterCommand("set clipboard-=autoselect")
      assertTrue(platformUpdatesPrimary())
    }
  }

  @Test
  fun `test user-disabled platform PRIMARY update stays disabled`() {
    configureByText("Lorem ipsum")
    val registryValue = Registry.get(PRIMARY_REGISTRY_KEY)
    registryValue.setValue("false", RegistryValueSource.USER)
    try {
      withSystemSelection {
        enterCommand("set clipboard-=autoselect")
        enterCommand("set clipboard+=autoselect")
        assertFalse(platformUpdatesPrimary())
      }
    } finally {
      registryValue.resetToDefault()
    }
  }

  private fun withSystemSelection(action: () -> Unit) {
    ClipboardAutoselectChangeListener.withSystemSelectionSupported(true, action)
  }

  companion object {
    private const val PRIMARY_REGISTRY_KEY = "editor.caret.update.primary.selection"
  }
}
