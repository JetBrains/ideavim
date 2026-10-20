/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry
import com.maddyhome.idea.vim.newapi.IjClipboardManager
import com.maddyhome.idea.vim.newapi.PrimarySelectionWriter
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClipboardAutoselectTest : VimTestCase() {
  private class RecordingPrimaryWriter : PrimarySelectionWriter {
    val writes = mutableListOf<String>()

    override fun write(text: String, transferableData: List<Any>): Boolean {
      writes.add(text)
      return true
    }

    override fun ownsSelection(): Boolean = writes.isNotEmpty()
  }

  private fun platformUpdatesPrimary() = Registry.`is`(PRIMARY_REGISTRY_KEY)

  private fun publishVisualSelection(writer: RecordingPrimaryWriter) {
    val manager = IjClipboardManager(writer) { true }
    try {
      ApplicationManager.getApplication().invokeAndWait {
        val editor = fixture.editor.vim
        manager.onVisualSelectionChange(editor, editor.primaryCaret())
      }
    } finally {
      manager.dispose()
    }
  }

  @Test
  fun `test visual selection is published to PRIMARY with autoselect`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("set clipboard=autoselect")
    typeText("vll")

    val writer = RecordingPrimaryWriter()
    publishVisualSelection(writer)
    assertEquals(listOf("Lor"), writer.writes)
  }

  @Test
  fun `test visual selection is not published to PRIMARY without autoselect`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("set clipboard-=autoselect")
    typeText("vll")

    val writer = RecordingPrimaryWriter()
    publishVisualSelection(writer)
    assertEquals(emptyList(), writer.writes)
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
