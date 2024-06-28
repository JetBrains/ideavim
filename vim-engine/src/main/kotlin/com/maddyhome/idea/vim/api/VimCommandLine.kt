/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import javax.swing.KeyStroke
import kotlin.math.min

interface VimCommandLine {
  val caret: VimCommandLineCaret

  val label: String
  val isReplaceMode: Boolean

  fun toggleReplaceMode()

  /**
   * The actual text present in the command line, excluding special characters like the `?` displayed during digraph input.
   * This text represents the real content that is being processed or executed.
   */
  val actualText: String
    get() = if (promptCharacterOffset == null) visibleText else {
      visibleText.removeRange(promptCharacterOffset!!, promptCharacterOffset!! + 1)
    }

  /**
   * The text content displayed in the command line, including any additional characters or symbols
   * that might be shown to the user, such as the `?` during digraph input.
   * This is the text that the user sees on the screen.
   */
  val visibleText: String
  var promptCharacterOffset: Int?

  fun setText(string: String)
  fun insertText(offset: Int, string: String) {
    val newText = if (isReplaceMode) {
      val endOffset = min(offset + string.length, actualText.length)
      StringBuilder(actualText).replace(offset, endOffset, string)
    } else {
      StringBuilder(actualText).insert(offset, string)
    }.toString()
    setText(newText)
  }

  fun handleKey(key: KeyStroke)

  /**
   * Text to show while composing a digraph or inserting a literal or register
   * <p>
   * The prompt character is inserted directly into the text of the text field, rather than drawn over the top of the
   * current character. When the action has been completed, the new character(s) are either inserted or overwritten,
   * depending on the insert/overwrite status of the text field. This mimics Vim's behaviour.
   *
   * @param promptCharacter The character to show as prompt
   */
  fun setPromptCharacter(char: Char) {
    val stringBuilder = StringBuilder(actualText)

    val offset = promptCharacterOffset ?: caret.offset // TODO is there a case where caret is not at the [promptCharacterOffset]?
    promptCharacterOffset = offset
    stringBuilder.insert(offset, char)
    setText(stringBuilder.toString())

    caret.offset = offset
  }
  fun clearPromptCharacter() {
    setText(actualText)
    caret.offset = min(caret.offset, visibleText.length)
    promptCharacterOffset = null
  }

  fun clearCurrentAction()

  fun deactivate(refocusOwningEditor: Boolean, resetCaret: Boolean)

  // FIXME I don't want it to conflict with Swings `requestFocus` and can suggest a better name
  fun focus()
}