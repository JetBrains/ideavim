/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.pushCurrentPosition
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.helper.isRiderBackendCompositeAction
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.KeyStroke

/**
 * IDE navigation to a definition, which is a tag jump however it is invoked, see "h tag-stack"
 *
 * Covers Ctrl+Click and its siblings, the keyboard shortcuts, `<Action>(...)` mappings and `:action`, as well as
 * `<C-]>`, `gd` and `gD`, which run Go to Declaration too.
 */
private val TAG_ACTIONS: Set<String> = setOf(
  IdeActions.ACTION_GOTO_DECLARATION,
  "GotoDeclarationOnly",
  IdeActions.ACTION_GOTO_TYPE_DECLARATION,
  IdeActions.ACTION_GOTO_IMPLEMENTATION,
)

/**
 * True for an IDE navigation to a definition, see [TAG_ACTIONS]
 *
 * For a mouse click or a hotkey, Rider runs its backend actions through one composite action without an ID, and the
 * backend picks which of them to run. Only the keymap tells that the shortcut is bound to a tag jump then.
 */
internal fun isTagJump(action: AnAction, actionId: String?, event: AnActionEvent): Boolean {
  if (actionId != null) return actionId in TAG_ACTIONS
  return action.isRiderBackendCompositeAction() && shortcutActionIds(event.inputEvent).any { it in TAG_ACTIONS }
}

private fun shortcutActionIds(inputEvent: InputEvent?): List<String> {
  val shortcut = when (inputEvent) {
    is MouseEvent -> KeymapUtil.createMouseShortcut(inputEvent)
    is KeyEvent -> KeyboardShortcut(KeyStroke.getKeyStrokeForEvent(inputEvent), null)
    else -> return emptyList()
  }
  return KeymapManager.getInstance().activeKeymap.getActionIdList(shortcut)
}

/**
 * Pushes the caret position onto the tag stack before the IDE navigates away from it
 *
 * Without a keyword there is no tag to jump to, so there is nothing to walk back over either. The IDE is still free
 * to be cleverer about what the caret is on than Vim's notion of a keyword.
 */
internal fun pushTagLocation(editor: Editor) {
  if (editor.isDisposed || editor.isIdeaVimDisabledHere) return
  val vimEditor = editor.vim
  val wordRange =
    injector.searchHelper.findWordAtOrFollowingCursor(vimEditor, vimEditor.currentCaret(), isBigWord = false)
      ?: return
  injector.tagService.pushCurrentPosition(vimEditor, vimEditor.getText(wordRange))
}
