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

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.components.Service

@Service
class IjNativeActionManager : NativeActionManager {
  override val enterAction: NativeAction? by lazy { byName(IdeActions.ACTION_EDITOR_ENTER) }
  override val createLineAboveCaret: NativeAction? by lazy { byName("EditorStartNewLineBefore") }
  override val joinLines: NativeAction? by lazy { byName(IdeActions.ACTION_EDITOR_JOIN_LINES) }
  override val indentLines: NativeAction? by lazy { byName(IdeActions.ACTION_EDITOR_AUTO_INDENT_LINES) }
  override val saveAll: NativeAction? by lazy { byName("SaveAll") }
  override val saveCurrent: NativeAction? by lazy { byName("SaveDocument") }

  private fun byName(name: String): IjNativeAction? {
    val action: AnAction? = ActionManager.getInstance().getAction(name)
    return action?.vim
  }
}
