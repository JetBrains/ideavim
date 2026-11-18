/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.TextAttributes
import com.maddyhome.idea.vim.ui.OutputPanel
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Font
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestWithoutNeovim(reason = SkipNeovimReason.IDEAVIM_API_USED)
class CommandLineColorsTest : VimTestCase() {
  private val schemeForeground = Color(0x10, 0x20, 0x30)
  private val schemeBackground = Color(0xF0, 0xE0, 0xD0)

  private fun setEditorColorScheme() {
    val scheme = EditorColorsManager.getInstance().globalScheme.clone() as EditorColorsScheme
    scheme.setAttributes(
      HighlighterColors.TEXT,
      TextAttributes(schemeForeground, schemeBackground, null, null, Font.PLAIN),
    )
    ApplicationManager.getApplication().invokeAndWait {
      (fixture.editor as EditorEx).colorsScheme = scheme
    }
  }

  @Test
  fun `test command line uses background from editor colour scheme`() {
    configureByText("lorem ipsum")
    setEditorColorScheme()

    typeText(":")

    val panel = ExEntryPanel.getOrCreatePanelInstance()
    assertEquals(schemeForeground, panel.entry.foreground)
    assertEquals(schemeBackground, panel.entry.background)
    assertEquals(schemeBackground, panel.background)
  }

  @Test
  fun `test search command line uses background from editor colour scheme`() {
    configureByText("lorem ipsum")
    setEditorColorScheme()

    typeText("/")

    val panel = ExEntryPanel.getOrCreatePanelInstance()
    assertEquals(schemeForeground, panel.entry.foreground)
    assertEquals(schemeBackground, panel.background)
  }

  @Test
  fun `test output panel uses background from editor colour scheme`() {
    configureByText("lorem ipsum")
    setEditorColorScheme()

    enterCommand("echo 'hello'")

    val panel = assertNotNull(OutputPanel.tryGetInstance(fixture.editor))
    assertEquals(schemeForeground, panel.foreground)
    assertEquals(schemeBackground, panel.background)
  }
}
