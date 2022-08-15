/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.handler.editor

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.api.VimKeyGroupBase.Companion.downKey
import com.maddyhome.idea.vim.api.VimKeyGroupBase.Companion.enterKey
import com.maddyhome.idea.vim.api.VimKeyGroupBase.Companion.escKey
import com.maddyhome.idea.vim.api.VimKeyGroupBase.Companion.leftKey
import com.maddyhome.idea.vim.api.VimKeyGroupBase.Companion.rightKey
import com.maddyhome.idea.vim.api.VimKeyGroupBase.Companion.upKey
import javax.swing.KeyStroke

class VimLeftEditorHandler(nextHandler: EditorActionHandler) : VimEditorHandler(leftKey, nextHandler)
class VimRightEditorHandler(nextHandler: EditorActionHandler) : VimEditorHandler(rightKey, nextHandler)
class VimDownEditorHandler(nextHandler: EditorActionHandler) : VimEditorHandler(downKey, nextHandler)
class VimUpEditorHandler(nextHandler: EditorActionHandler) : VimEditorHandler(upKey, nextHandler)
class VimEnterEditorHandler(nextHandler: EditorActionHandler) : VimEditorHandler(enterKey, nextHandler)
class VimEscEditorHandler(nextHandler: EditorActionHandler) : VimEditorHandler(escKey, nextHandler)

abstract class VimEditorHandler(
  private val key: KeyStroke,
  private val nextHandler: EditorActionHandler,
) : EditorActionHandler(false) {

  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    val enabled = VimShortcutKeyAction.isEnabled(editor, key)
    if (enabled) {
      VimShortcutKeyAction.executeAction(key, editor, dataContext)
    } else {
      nextHandler.execute(editor, caret, dataContext)
    }
  }

  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return VimShortcutKeyAction.isEnabled(editor, key) || nextHandler.isEnabled(editor, caret, dataContext)
  }
}