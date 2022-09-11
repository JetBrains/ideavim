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

package com.maddyhome.idea.vim

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.newapi.vim
import javax.swing.KeyStroke

class VimTypedDelegateHandler : TypedHandlerDelegate() {

  lateinit var stateUpdateResult: KeyHandler.StateUpdateResult

  override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
    if (!VimPlugin.isEnabled()) return Result.CONTINUE
    if (editor.isIdeaVimDisabledHere) return Result.CONTINUE
    if (!useNewHandler(editor.vim, charTyped)) return Result.CONTINUE

    return if (editor.inInsertMode) Result.CONTINUE else Result.STOP
  }

  override fun newTypingStarted(c: Char, editor: Editor, context: DataContext) {
    if (!VimPlugin.isEnabled()) return
    if (editor.isIdeaVimDisabledHere) return
    if (!useNewHandler(editor.vim, c)) return

    val keyStroke = KeyStroke.getKeyStroke(c)
    val content = EditorDataContext.init(editor, context)
    content.newTypingDelegate = true
    stateUpdateResult = KeyHandler.getInstance()
      .handleKeyInitial(editor.vim, keyStroke, content.vim, execute = false)
  }

  override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
    if (!VimPlugin.isEnabled()) return Result.CONTINUE
    if (editor.isIdeaVimDisabledHere) return Result.CONTINUE
    if (!useNewHandler(editor.vim, c)) return Result.CONTINUE

    val keyStroke = KeyStroke.getKeyStroke(c)
    val context = EditorDataContext.init(editor)
    context.newTypingDelegate = true
    if (stateUpdateResult.continueExecution) {
      KeyHandler.getInstance().finishedCommandPreparation(editor.vim, context.vim, keyStroke, stateUpdateResult.shouldRecord)
    }

    return Result.STOP
  }

/*
  override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
    if (editor.isIdeaVimDisabledHere) return Result.CONTINUE
    if (c !in charsByDelegate) return Result.CONTINUE

    val keyStroke = KeyStroke.getKeyStroke(c)
    KeyHandler.getInstance().handleKey(editor.vim, keyStroke, EditorDataContext.init(editor).vim)

    return Result.STOP
  }
*/

  override fun isImmediatePaintingEnabled(editor: Editor, c: Char, context: DataContext): Boolean {
    if (!VimPlugin.isEnabled()) return true
    if (editor.isIdeaVimDisabledHere) return true
    if (!useNewHandler(editor.vim, c)) return true

    return editor.inInsertMode
  }
}

internal val charsByDelegate = setOf('j', 'd')

internal fun useNewHandler(editor: VimEditor, c: Char): Boolean {
  return c in charsByDelegate && (editor.inNormalMode || editor.mode == VimStateMachine.Mode.OP_PENDING)
}
