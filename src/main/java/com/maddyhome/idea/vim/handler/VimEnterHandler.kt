/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.key
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.runFromVimKey
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * This handler doesn't work in tests for ex commands
 */
internal abstract class OctopusHandler(private val nextHandler: EditorActionHandler) : EditorActionHandler() {

  abstract fun executeHandler(editor: Editor, caret: Caret?, dataContext: DataContext?)
  open fun isHandlerEnabled(editor: Editor, dataContext: DataContext?): Boolean {
    return true
  }

  final override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    if (isThisHandlerEnabled(editor, caret, dataContext)) {
      executeHandler(editor, caret, dataContext)
    } else {
      nextHandler.execute(editor, caret, dataContext)
    }
  }

  @Suppress("RedundantIf")
  private fun isThisHandlerEnabled(editor: Editor, caret: Caret?, dataContext: DataContext?): Boolean {
    if (!VimPlugin.isEnabled()) return false
    if (!isHandlerEnabled(editor, dataContext)) return false
    if (dataContext?.getData(runFromVimKey) == true) return false
    if (!enableOctopus) return false
    return true
  }

  final override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return isThisHandlerEnabled(editor, caret, dataContext) || nextHandler.isEnabled(editor, caret, dataContext)
  }
}

/**
 * Known conflicts & solutions:
 * - Smart step into - set handler after
 * - Python notebooks - set handler before - test needed!
 * - Ace jump - set handler after
 * - Lookup - doesn't intersect with enter anymore
 * - App code - set handler after
 * - Template - doesn't intersect with enter anymore
 */
internal class VimEnterHandler(nextHandler: EditorActionHandler) : VimKeyHandler(nextHandler) {
  override val key: String = "<CR>"
}

/**
 * Known conflicts & solutions:
 *
 * - Smart step into - set handler after
 * - Python notebooks - set handler before - test needed
 * - Ace jump - set handler after
 * - Lookup - It disappears after putting our esc before templateEscape. But I'm not sure why it works like that
 * - App code - Need to review
 * - Template - Need to review
 *
 * Also, we need to pass esc to IDE if we're in normal mode and there is nothing to cancel
 * (e.g. we still can cancel numbers)
 */
internal class VimEscHandler(nextHandler: EditorActionHandler) : VimKeyHandler(nextHandler) {
  override val key: String = "<Esc>"

  override fun isHandlerEnabled(editor: Editor, dataContext: DataContext?): Boolean {
    return editor.mode != CommandState.Mode.COMMAND ||
      editor.vimStateMachine?.commandBuilder?.count != 0
  }
}

internal abstract class VimKeyHandler(nextHandler: EditorActionHandler) : OctopusHandler(nextHandler) {

  abstract val key: String

  override fun executeHandler(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    val enterKey = key(key)
    val context = injector.executionContextManager.onEditor(editor.vim, dataContext?.vim)
    KeyHandler.getInstance().handleKey(editor.vim, enterKey, context)
  }

  override fun isHandlerEnabled(editor: Editor, dataContext: DataContext?): Boolean {
    val enterKey = key(key)
    return isOctopusEnabled(enterKey, editor)
  }
}

internal fun isOctopusEnabled(s: KeyStroke, editor: Editor): Boolean {
  if (!enableOctopus) return false
  when {
    s.keyCode == KeyEvent.VK_ENTER -> return editor.mode in listOf(
      CommandState.Mode.COMMAND,
      CommandState.Mode.INSERT,
      CommandState.Mode.VISUAL,
    )
    s.keyCode == KeyEvent.VK_ESCAPE -> return editor.mode in listOf(
      CommandState.Mode.COMMAND,
      CommandState.Mode.INSERT,
      CommandState.Mode.VISUAL,
    )
  }
  return false
}

/**
 * Experiment: At the moment, IdeaVim intersects all shortcuts and sends the to [KeyHandler]
 * However, this doesn't seem to be a good solution as other handlers are overridden by vim.
 * If this option is enabled, vim will connect to IDE via EditorActionHandler extension point
 *   what seems to be a way better solution as this is a correct way to override editor actions like enter, right, etc.
 */
internal val enableOctopus: Boolean
  get() = injector.globalIjOptions().octopushandler
