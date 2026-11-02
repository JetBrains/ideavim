/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.air

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.ide.ChatInputProvider
import javax.swing.KeyStroke

/**
 * The prompt input of JetBrains AIR.
 *
 * AIR has no public API for its prompt, so we rely on its internal names. It marks the prompt document with
 * `AIR_PROMPT_INPUT_DOCUMENT` before the editor is created, and names the document's light file `AirPrompt-<uuid>`.
 *
 * AIR binds its prompt actions (`Air.Chat.Prompt.*`) to keys such as Enter (send), Shift+Enter (new line) and
 * Ctrl+Enter (send now). In the prompt, it turns the platform's line-break actions (`EditorEnter`,
 * `EditorStartNewLine`, `EditorStartNewLineBefore`, `EditorSplitLine`) into no-ops, so only its own new-line action
 * breaks a line.
 */
class AirChatInputProvider : ChatInputProvider {
  override fun isChatInput(editor: Editor): Boolean {
    @Suppress("DEPRECATION") val key = Key.findKeyByName(PROMPT_DOCUMENT_KEY_NAME)
    if (key != null && editor.document.getUserData(key) == true) return true
    val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return false
    return file.name.startsWith(PROMPT_FILE_NAME_PREFIX)
  }

  override fun isChatKey(editor: Editor, keyStroke: KeyStroke): Boolean {
    val keymap = KeymapManager.getInstance()?.activeKeymap ?: return false
    return keymap.getActionIds(keyStroke).any { it.startsWith(PROMPT_ACTION_ID_PREFIX) }
  }

  override fun getLineBreakAction(editor: Editor): AnAction? =
    ActionManager.getInstance().getAction(NEW_LINE_ACTION_ID)

  private companion object {
    const val PROMPT_DOCUMENT_KEY_NAME = "AIR_PROMPT_INPUT_DOCUMENT"
    const val PROMPT_FILE_NAME_PREFIX = "AirPrompt-"
    const val PROMPT_ACTION_ID_PREFIX = "Air.Chat.Prompt."
    const val NEW_LINE_ACTION_ID = "Air.Chat.Prompt.NewLine"
  }
}
