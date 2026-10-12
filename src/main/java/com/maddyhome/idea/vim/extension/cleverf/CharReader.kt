/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputKeyStroke
import com.maddyhome.idea.vim.newapi.ij
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Reads the character to search for. A key from `g:clever_f_repeat_last_char_inputs` (`<CR>` by default) stands for
 * the last searched character.
 */
internal class CharReader(private val session: CleverFSession) {
  /** Returns the character to search for, or null if the input was cancelled or there is nothing to repeat */
  fun read(editor: VimEditor): Char? {
    val key = inputKeyStroke(editor.ij)
    return when {
      isEscape(key) -> null
      isRepeatLastCharInput(key) -> session.lastChar ?: null.also { injector.messages.indicateError() }
      else -> key.keyChar.takeUnless { it == KeyEvent.CHAR_UNDEFINED }
    }
  }

  private fun isRepeatLastCharInput(key: KeyStroke): Boolean {
    val inputs = CleverFSettings.repeatLastCharInputs ?: return isEnter(key)
    return inputs.any { input -> if (input == "\r" || input == "\n") isEnter(key) else isSameKey(input, key) }
  }

  private fun isSameKey(input: String, key: KeyStroke): Boolean =
    injector.parser.parseKeys(input).singleOrNull() == key || input == key.keyChar.toString()

  private fun isEscape(key: KeyStroke): Boolean =
    key.keyCode == KeyEvent.VK_ESCAPE || key.keyChar == KeyEvent.VK_ESCAPE.toChar()

  private fun isEnter(key: KeyStroke): Boolean =
    key.keyCode == KeyEvent.VK_ENTER || key.keyChar == '\n' || key.keyChar == '\r'
}
