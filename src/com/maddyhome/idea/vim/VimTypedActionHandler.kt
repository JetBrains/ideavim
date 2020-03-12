/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx
import com.maddyhome.idea.vim.helper.EditorDataContext
import javax.swing.KeyStroke

/**
 * Accepts all regular keystrokes and passes them on to the Vim key handler.
 *
 * IDE shortcut keys used by Vim commands are handled by [com.maddyhome.idea.vim.action.VimShortcutKeyAction].
 */
class VimTypedActionHandler(origHandler: TypedActionHandler?) : TypedActionHandlerEx {
  private val handler = KeyHandler.getInstance()

  init {
    handler.originalHandler = origHandler
  }

  override fun beforeExecute(editor: Editor, charTyped: Char, context: DataContext, plan: ActionPlan) {
    handler.beforeHandleKey(editor, KeyStroke.getKeyStroke(charTyped), context, plan)
  }

  override fun execute(editor: Editor, charTyped: Char, context: DataContext) {
    try {
      handler.handleKey(editor, KeyStroke.getKeyStroke(charTyped), EditorDataContext(editor))
    } catch (e: Throwable) {
      logger.error(e)
    }
  }

  companion object {
    private val logger = logger<VimTypedActionHandler>()
  }
}