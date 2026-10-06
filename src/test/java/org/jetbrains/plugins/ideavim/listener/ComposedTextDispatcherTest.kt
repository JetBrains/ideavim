/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.listener

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.event.InputMethodEvent
import java.text.AttributedString
import kotlin.test.assertEquals

/**
 * A dead key is simulated with the events the OS sends for it: the pending character as composed text, then the final
 * text as committed text once the next key is pressed (space for the dead key character itself).
 */
@TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
class ComposedTextDispatcherTest : VimTestCase() {
  @Test
  fun `test dead key does not replace the Visual selection`() {
    configureByText("'sam${c}ple text'")
    typeText("vi")
    typeDeadKey('\'', "'")
    assertState("'${s}sample tex${c}t${se}'")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test dead key used as a Visual command`() {
    configureByText("'sam${c}ple text'")
    typeText("v")
    typeDeadKey('~', "~")
    assertState("'sam${c}Ple text'")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test dead key composing a character for a motion`() {
    configureByText("${c}cafe café")
    typeText("f")
    dispatch(inputMethodEvent("'", committedCount = 0))
    assertEquals("cafe café", fixture.editor.document.text)
    dispatch(inputMethodEvent("é", committedCount = 1))
    assertState("cafe caf${c}é")
  }

  @Test
  fun `test committed part of a mixed event reaches IdeaVim`() {
    configureByText("'sam${c}ple text'")
    typeText("vi")
    dispatch(inputMethodEvent("'~", committedCount = 1))
    assertState("'${s}sample tex${c}t${se}'")
  }

  @Test
  fun `test composed text is shown in Insert mode`() {
    configureByText("${c}cafe")
    typeText("i")
    dispatch(inputMethodEvent("'", committedCount = 0))
    assertEquals("'cafe", fixture.editor.document.text)
    dispatch(inputMethodEvent("é", committedCount = 1))
    assertState("é${c}cafe")
    assertMode(Mode.INSERT)
  }

  private fun typeDeadKey(deadKey: Char, committed: String) {
    dispatch(inputMethodEvent(deadKey.toString(), committedCount = 0))
    dispatch(inputMethodEvent(committed, committedCount = committed.length))
  }

  private fun inputMethodEvent(text: String, committedCount: Int) = InputMethodEvent(
    fixture.editor.contentComponent,
    InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
    AttributedString(text).iterator,
    committedCount,
    null,
    null,
  )

  private fun dispatch(event: InputMethodEvent) {
    ApplicationManager.getApplication().invokeAndWait { IdeEventQueue.getInstance().dispatchEvent(event) }
  }
}
