/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.maddyhome.idea.vim.VimTypedActionHandler
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * It's needed to wait till JupyterCommandModeTypingBlocker is going to be registered using an extension point
 * After that, we would be able to register our typingHandler before (or after) the one from jupyter.
 */
internal class HandlerInjector {
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
    fun notebookCommandMode(editor: Editor?): Boolean {
      return if (editor != null) {
        val inEditor = EditorHelper.getVirtualFile(editor)?.extension == "ipynb"
        return if (TypedAction.getInstance().rawHandler::class.java.simpleName.equals("JupyterCommandModeTypingBlocker")) {
          inEditor
        } else {
          // only true in command mode.
          // Set by `org.jetbrains.plugins.notebooks.ui.editor.actions.command.mode.NotebookEditorModeListenerAdapter`
          // appears to be null in non Notebook editors
          val allow_plain_letter_shortcuts =
            editor.contentComponent.getClientProperty(ActionUtil.ALLOW_PlAIN_LETTER_SHORTCUTS)
          inEditor && (allow_plain_letter_shortcuts != null && allow_plain_letter_shortcuts as Boolean)
        }
      } else {
        TypedAction.getInstance().rawHandler::class.java.simpleName.equals("JupyterCommandModeTypingBlocker")
      }
    }
  }
}
