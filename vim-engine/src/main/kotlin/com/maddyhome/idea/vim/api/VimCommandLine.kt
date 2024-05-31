/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.impl.state.toMappingMode
import javax.swing.KeyStroke
import kotlin.math.min

public interface VimCommandLine {
  public val caret: VimCommandLineCaret

  public val label: String

  /**
   * The actual text present in the command line, excluding special characters like the `?` displayed during digraph input.
   * This text represents the real content that is being processed or executed.
   */
  public val actualText: String
    get() = if (promptCharacterOffset == null) visibleText else {
      visibleText.removeRange(promptCharacterOffset!!, promptCharacterOffset!! + 1)
    }

  /**
   * The text content displayed in the command line, including any additional characters or symbols
   * that might be shown to the user, such as the `?` during digraph input.
   * This is the text that the user sees on the screen.
   */
  public val visibleText: String
  public var promptCharacterOffset: Int?

  public fun setText(string: String)
  public fun handleKey(key: KeyStroke)

  /**
   * Text to show while composing a digraph or inserting a literal or register
   * <p>
   * The prompt character is inserted directly into the text of the text field, rather than drawn over the top of the
   * current character. When the action has been completed, the new character(s) are either inserted or overwritten,
   * depending on the insert/overwrite status of the text field. This mimics Vim's behaviour.
   *
   * @param promptCharacter The character to show as prompt
   */
  public fun setPromptCharacter(char: Char) {
    val stringBuilder = StringBuilder(actualText)

    val offset = promptCharacterOffset ?: caret.offset // TODO is there a case where caret is not at the [promptCharacterOffset]?
    promptCharacterOffset = offset
    stringBuilder.insert(offset, char)
    setText(stringBuilder.toString())

    caret.offset = offset
  }
  public fun clearPromptCharacter() {
    setText(actualText)
    caret.offset = min(caret.offset, visibleText.length)
    promptCharacterOffset = null
  }

  public fun clearCurrentAction()

  public fun deactivate(refocusOwningEditor: Boolean, resetCaret: Boolean)
}