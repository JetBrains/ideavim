/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.util.registry.Registry
import com.maddyhome.idea.vim.api.injector
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

    enterCommand("set clipboard-=autoselect")
    assertFalse(platformUpdatesPrimary())

    enterCommand("set clipboard+=autoselect")
    assertTrue(platformUpdatesPrimary())
  }

  @Test
  fun `test resetting clipboard restores platform PRIMARY update`() {
    configureByText("Lorem ipsum")
    enterCommand("set clipboard-=autoselect")
    assertFalse(platformUpdatesPrimary())

    enterCommand("set clipboard&")
    assertTrue(platformUpdatesPrimary())
  }

  @Test
  fun `test user-disabled platform PRIMARY update stays disabled`() {
    configureByText("Lorem ipsum")
    val registryValue = Registry.get(PRIMARY_REGISTRY_KEY)
    registryValue.setValue(false)
    try {
      enterCommand("set clipboard-=autoselect")
      enterCommand("set clipboard+=autoselect")
      assertFalse(platformUpdatesPrimary())
    } finally {
      registryValue.resetToDefault()
    }
  }

  companion object {
    private const val PRIMARY_REGISTRY_KEY = "editor.caret.update.primary.selection"
  }
}
