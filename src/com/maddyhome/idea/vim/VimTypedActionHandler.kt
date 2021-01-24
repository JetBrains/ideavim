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
package com.maddyhome.idea.vim

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
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
    if (editor.isIdeaVimDisabledHere) {
      (handler.originalHandler as? TypedActionHandlerEx)?.beforeExecute(editor, charTyped, context, plan)
      return
    }

    val modifiers = if (charTyped == ' ' && VimKeyListener.isSpaceShift) KeyEvent.SHIFT_DOWN_MASK else 0
    val keyStroke = KeyStroke.getKeyStroke(charTyped, modifiers)
    handler.beforeHandleKey(editor, keyStroke, context, plan)
  }

  override fun execute(editor: Editor, charTyped: Char, context: DataContext) {
    if (editor.isIdeaVimDisabledHere) {
      handler.originalHandler.execute(editor, charTyped, context)
      return
    }

    try {
      val modifiers = if (charTyped == ' ' && VimKeyListener.isSpaceShift) KeyEvent.SHIFT_DOWN_MASK else 0
      val keyStroke = KeyStroke.getKeyStroke(charTyped, modifiers)
      handler.handleKey(editor, keyStroke, EditorDataContext.init(editor, context))
    } catch (e: Throwable) {
      logger.error(e)
    }
  }

  companion object {
    private val logger = logger<VimTypedActionHandler>()
  }
}

/**
 * A nasty workaround to handle `<S-Space>` events. Probably all the key events should go trough this listener.
 */
object VimKeyListener : KeyAdapter() {

  var isSpaceShift = false

  override fun keyPressed(e: KeyEvent) {
    isSpaceShift = e.modifiersEx and KeyEvent.SHIFT_DOWN_MASK != 0 && e.keyChar == ' '
  }
}
