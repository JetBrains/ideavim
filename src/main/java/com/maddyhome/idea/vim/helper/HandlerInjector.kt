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

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.maddyhome.idea.vim.VimTypedActionHandler
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class HandlerInjector {
  companion object {
    @JvmStatic
    fun inject(): TypedActionHandler? {
      try {
        val javaClass = TypedAction.getInstance().rawHandler::class.java
        val pythonHandler = javaClass.kotlin.objectInstance
        val field =
          javaClass.declaredFields.singleOrNull { it.name == "DEFAULT_RAW_HANDLER" || it.name == "editModeRawHandler" }
            ?: return null
        field.isAccessible = true

        val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

        val originalHandler = field.get(pythonHandler) as? TypedActionHandler ?: return null
        val newVimHandler = VimTypedActionHandler(originalHandler)
        field.set(pythonHandler, newVimHandler)
        return originalHandler
      } catch (ignored: Exception) {
        // Ignore
      }
      return null
    }

    @JvmStatic
    fun notebookCommandMode(): Boolean {
      return TypedAction.getInstance().rawHandler::class.java.simpleName.equals("JupyterCommandModeTypingBlocker")
    }
  }
}