/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.MouseShortcut
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.keymap.KeymapManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.register.Register
import com.maddyhome.idea.vim.register.RegisterConstants
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.event.MouseEvent

/**
 * Middle-click paste in Insert mode puts the `*` register at the caret, instead of at the mouse position.
 *
 * The platform moves the caret to the mouse when the button is pressed, but pastes only when it is released. So the
 * mouse listener consumes the press when [shouldKeepCaretOnPress] says so, and this handler puts at the caret.
 */
internal class VimPasteFromX11Handler(private val originalHandler: EditorActionHandler) : EditorActionHandler() {
  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean =
    originalHandler.isEnabled(editor, caret, dataContext)

  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    if (pastesAtCaret(editor)) {
      putPrimaryRegisterAtCaret(editor)
    } else {
      originalHandler.execute(editor, caret, dataContext)
    }
  }

  private fun putPrimaryRegisterAtCaret(editor: Editor) {
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val register = injector.registerGroup.getRegister(vimEditor, context, RegisterConstants.PRIMARY_REGISTER) ?: return
    injector.put.putText(vimEditor, context, putBeforeCaret(register))
  }

  private fun putBeforeCaret(register: Register) = PutData(
    PutData.TextData(register),
    null,
    1,
    insertTextBeforeCaret = true,
    rawIndent = false,
    caretAfterInsertedText = true,
  )

  companion object {
    private const val PASTE_FROM_X11_ACTION = "EditorPasteFromX11"

    fun shouldKeepCaretOnPress(event: EditorMouseEvent): Boolean =
      event.area == EditorMouseEventArea.EDITING_AREA &&
        isPasteFromX11Supported() &&
        pastesAtCaret(event.editor) &&
        isBoundToPasteFromX11(event.mouseEvent)

    private fun isPasteFromX11Supported(): Boolean = injector.systemInfoService.isXWindow

    private fun isBoundToPasteFromX11(event: MouseEvent): Boolean {
      val shortcut = MouseShortcut(MouseShortcut.getButton(event), event.modifiersEx, event.clickCount)
      val actionIds = KeymapManager.getInstance()?.activeKeymap?.getActionIds(shortcut) ?: return false
      return PASTE_FROM_X11_ACTION in actionIds
    }

    private fun pastesAtCaret(editor: Editor): Boolean =
      VimPlugin.isEnabled() && !editor.isIdeaVimDisabledHere && editor.vim.mode is Mode.INSERT
  }
}
