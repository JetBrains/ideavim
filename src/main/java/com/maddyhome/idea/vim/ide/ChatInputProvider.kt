/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ide

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import javax.swing.KeyStroke

/**
 * Describes the prompt input of an AI chat, where Vim has to behave differently from a regular editor.
 *
 * Each chat that needs this lives in its own optional module (e.g. `ideavim-air`), so everything chat-specific is in
 * one place, and every place where IdeaVim treats a chat prompt differently can be found from the usages of this
 * interface.
 */
interface ChatInputProvider {
  /**
   * Whether [editor] is the prompt input of this chat
   */
  fun isChatInput(editor: Editor): Boolean

  /**
   * Whether the chat owns [keyStroke] in its prompt input, e.g. to send the message, so Vim passes the key on to the
   * chat in every mode, unless the user maps it
   */
  fun isChatKey(editor: Editor, keyStroke: KeyStroke): Boolean = false

  /**
   * The chat's own action that breaks the line at the caret, for a prompt input where the platform's line-break
   * actions don't work. Used for `o`, `O` and `<CR>` in Insert mode. Null to use the platform's actions.
   */
  fun getLineBreakAction(editor: Editor): AnAction? = null

  companion object {
    internal val EP_NAME: ExtensionPointName<ChatInputProvider> =
      ExtensionPointName.create("IdeaVIM.chatInputProvider")

    /**
     * The provider of the chat whose prompt input is [editor], if any. Viewers are never a prompt input, so code
     * snippets rendered in chat answers keep their native behaviour.
     */
    @JvmStatic
    fun forEditor(editor: Editor): ChatInputProvider? {
      if (editor.isViewer) return null
      return EP_NAME.extensionList.firstOrNull { it.isChatInput(editor) }
    }
  }
}
