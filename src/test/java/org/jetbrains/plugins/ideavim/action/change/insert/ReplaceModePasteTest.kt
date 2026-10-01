/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.intellij.codeInsight.editorActions.PasteHandler
import com.intellij.idea.TestFor
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.ide.CopyPasteManager
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.datatransfer.StringSelection

// Pasting with the IDE paste action (Cmd+V/Ctrl+V) while in Replace mode. Vim treats the pasted text as if it was
// typed, so it overwrites the characters under the caret, and backspace puts the original characters back. Neovim does
// the same in `vim.paste` (see the Replace mode branch in runtime/lua/vim/_core/editor.lua):
//   - each pasted character replaces one character of the current line
//   - once the end of the line is reached, the rest of the text is appended
//   - a line break doesn't replace anything, it splits the line and replacing continues on the new line
@TestFor(issues = ["VIM-1314"])
@TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
class ReplaceModePasteTest : VimTestCase() {
  private fun pasteViaIde(text: String) {
    CopyPasteManager.getInstance().setContents(StringSelection(text))
    fixture.performEditorAction(IdeActions.ACTION_PASTE)
  }

  @Test
  fun `test paste in replace mode overwrites characters`() {
    configureByText("${c}123456789")
    typeText("R")
    pasteViaIde("abc")
    assertState("abc${c}456789")
    assertMode(Mode.REPLACE)
  }

  @Test
  fun `test paste in replace mode in the middle of the line overwrites characters`() {
    configureByText("123${c}456789")
    typeText("R")
    pasteViaIde("abc")
    assertState("123abc${c}789")
  }

  @Test
  fun `test paste in replace mode longer than the rest of the line appends the remaining text`() {
    configureByText("12${c}34")
    typeText("R")
    pasteViaIde("abcde")
    assertState("12abcde${c}")
  }

  @Test
  fun `test paste in replace mode does not overwrite the next line`() {
    configureByText(
      """
        12${c}34
        5678
      """.trimIndent()
    )
    typeText("R")
    pasteViaIde("abcde")
    assertState(
      """
        12abcde${c}
        5678
      """.trimIndent()
    )
  }

  @Test
  fun `test paste in replace mode with a line break splits the line and keeps replacing`() {
    configureByText("${c}123456789")
    typeText("R")
    pasteViaIde("abc\ndef")
    assertState(
      """
        abc
        def${c}789
      """.trimIndent()
    )
  }

  @Test
  fun `test paste in replace mode after typed characters`() {
    configureByText("${c}123456789")
    typeText("R", "xy")
    pasteViaIde("abc")
    assertState("xyabc${c}6789")
  }

  @Test
  fun `test backspace after paste in replace mode restores one character`() {
    configureByText("${c}123456789")
    typeText("R")
    pasteViaIde("abc")
    typeText("<BS>")
    assertState("ab${c}3456789")
  }

  @Test
  fun `test backspace after paste in replace mode restores all replaced characters`() {
    configureByText("${c}123456789")
    typeText("R")
    pasteViaIde("abc")
    typeText("<BS>".repeat(3))
    assertState("${c}123456789")
  }

  @Test
  fun `test backspace after paste in replace mode deletes characters appended past the end of the line`() {
    configureByText("12${c}34")
    typeText("R")
    pasteViaIde("abcde")
    typeText("<BS>".repeat(3))
    assertState("12ab${c}")
  }

  @Test
  fun `test backspace after paste in replace mode longer than the line restores the original line`() {
    configureByText("12${c}34")
    typeText("R")
    pasteViaIde("abcde")
    typeText("<BS>".repeat(5))
    assertState("12${c}34")
  }

  @Test
  fun `test backspace after typing and pasting in replace mode restores the original text`() {
    configureByText("${c}123456789")
    typeText("R", "xy")
    pasteViaIde("abc")
    typeText("<BS>".repeat(5))
    assertState("${c}123456789")
  }

  @Test
  fun `test backspace after paste in replace mode with a line break restores the original line`() {
    configureByText("${c}123456789")
    typeText("R")
    pasteViaIde("abc\ndef")
    typeText("<BS>".repeat(7))
    assertState("${c}123456789")
  }

  @Test
  fun `test paste in replace mode when another paste handler pastes the text itself`() {
    // Some plugins (e.g. Jupyter) register a paste handler extending PasteHandler, which pastes the text without
    // calling the other paste handlers
    val actionManager = EditorActionManager.getInstance()
    val originalHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_PASTE)
    actionManager.setActionHandler(IdeActions.ACTION_EDITOR_PASTE, PasteHandler(originalHandler))
    try {
      configureByText("${c}123456789")
      typeText("R")
      pasteViaIde("abc")
      assertState("abc${c}456789")
    } finally {
      actionManager.setActionHandler(IdeActions.ACTION_EDITOR_PASTE, originalHandler)
    }
  }

  @Test
  fun `test paste in insert mode still inserts the text`() {
    configureByText("${c}123456789")
    typeText("i")
    pasteViaIde("abc")
    assertState("abc${c}123456789")
  }

  @Test
  fun `test undo after paste in replace mode restores the original text`() {
    configureByText("${c}123456789")
    typeText("R")
    pasteViaIde("abc")
    typeText("<Esc>", "u")
    assertState("${c}123456789")
  }
}
