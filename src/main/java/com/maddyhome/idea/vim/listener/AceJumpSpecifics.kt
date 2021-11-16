/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import org.acejump.session.SessionManager

/**
 * Key handling for IdeaVim should be updated to editorHandler usage. In this case this class can be safely removed.
 */

@Suppress("DEPRECATION")
interface AceJumpService {
  fun isActive(editor: Editor): Boolean

  companion object {
    fun getInstance(): AceJumpService? = ServiceManager.getService(AceJumpService::class.java)
  }
}

class AceJumpServiceImpl : AceJumpService {
  override fun isActive(editor: Editor): Boolean {
    return try {
      SessionManager[editor] != null
    } catch (e: Throwable) {
      // In case of any exception
      false
    }
  }
}