/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.air

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.air.AirChatInputProvider
import com.maddyhome.idea.vim.ide.ChatInputProvider

/**
 * Turns an editor into an AIR prompt input in a test. The AIR plugin isn't in the test IDE, so the `ideavim-air` module
 * isn't loaded and its provider is registered by hand. The document is marked the way AIR marks it.
 */
internal object AirPrompt {
  private const val PROMPT_DOCUMENT_KEY_NAME = "AIR_PROMPT_INPUT_DOCUMENT"

  @Suppress("DEPRECATION", "UNCHECKED_CAST")
  private val promptDocumentKey: Key<Boolean> =
    (Key.findKeyByName(PROMPT_DOCUMENT_KEY_NAME) ?: Key.create<Boolean>(PROMPT_DOCUMENT_KEY_NAME)) as Key<Boolean>

  fun mark(editor: Editor, disposable: Disposable) {
    ChatInputProvider.EP_NAME.point.registerExtension(AirChatInputProvider(), disposable)
    editor.document.putUserData(promptDocumentKey, true)
  }
}
