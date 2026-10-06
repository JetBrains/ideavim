/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.AWTEvent
import java.awt.Component
import java.awt.event.InputMethodEvent
import java.text.AttributedCharacterIterator
import java.text.AttributedString

/**
 * Keeps the composed text of dead keys and input methods out of the document while a command is typed (VIM-1503).
 *
 * A dead key, e.g. `'` in the U.S. International layout, does not produce a character straight away. The OS first sends
 * the pending character as composed text, and the editor inserts that into the document, replacing the selection - so
 * `vi'` deletes the selected character. The character only reaches IdeaVim once the next key commits it.
 *
 * In Normal, Visual and Operator-pending modes, the composed text is dropped and only the committed text gets through,
 * which matches Vim running in a terminal: `'` followed by space is `'`, and `'` followed by `e` is `é`. Insert, Replace
 * and Select modes are left alone, because there the composed text is meant to be typed into the document.
 */
internal object ComposedTextDispatcher : IdeEventQueue.NonLockedEventDispatcher {
  override fun dispatch(e: AWTEvent): Boolean {
    if (e !is InputMethodEvent || e.id != InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) return false
    val component = e.source as? EditorComponentImpl ?: return false
    if (!isTypingCommand(component.editor)) return false

    val text = e.text ?: return false
    val committedCount = e.committedCharacterCount
    if (text.endIndex - text.beginIndex <= committedCount) return false

    if (committedCount > 0) {
      // The committed part is a typed key that IdeaVim has to see. The composed rest stays with the input method
      component.dispatchEvent(committedOnly(component, e, text, committedCount))
    }
    return true
  }

  private fun isTypingCommand(editor: EditorImpl): Boolean {
    if (editor.isIdeaVimDisabledHere) return false
    // A composition the editor already shows (e.g. started in Insert mode) has to go through, so the editor removes it
    if (editor.composedTextRange != null) return false
    val mode = editor.vim.mode
    return mode is Mode.NORMAL || mode is Mode.VISUAL || mode is Mode.OP_PENDING
  }

  private fun committedOnly(
    component: Component,
    e: InputMethodEvent,
    text: AttributedCharacterIterator,
    committedCount: Int,
  ): InputMethodEvent {
    val committed = buildString {
      var c = text.first()
      repeat(committedCount) {
        append(c)
        c = text.next()
      }
    }
    return InputMethodEvent(component, e.id, e.`when`, AttributedString(committed).iterator, committedCount, null, null)
  }
}
