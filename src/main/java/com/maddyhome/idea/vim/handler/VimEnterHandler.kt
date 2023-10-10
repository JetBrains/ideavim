/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.removeUserData
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.key
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.newapi.actionStartedFromVim
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

internal val commandContinuation = Key.create<EditorActionHandler>("commandContinuation")

/**
 * Handler that corrects the shape of the caret in python notebooks.
 *
 * By default, py notebooks show a thin caret after entering the cell.
 *   However, we're in normal mode, so this handler fixes it.
 */
internal class CaretShapeEnterEditorHandler(private val nextHandler: EditorActionHandler) : EditorActionHandler() {
  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    invokeLater {
      editor.updateCaretsVisualAttributes()
    }
    nextHandler.execute(editor, caret, dataContext)
  }

  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return nextHandler.isEnabled(editor, caret, dataContext)
  }
}

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
      try {
        (dataContext as? UserDataHolder)?.putUserData(commandContinuation, nextHandler)
        executeHandler(editor, caret, dataContext)
      } finally {
        (dataContext as? UserDataHolder)?.removeUserData(commandContinuation)
      }
    } else {
      nextHandler.execute(editor, caret, dataContext)
    }
  }

  @Suppress("RedundantIf")
  private fun isThisHandlerEnabled(editor: Editor, caret: Caret?, dataContext: DataContext?): Boolean {
    if (!VimPlugin.isEnabled()) return false
    if (!isHandlerEnabled(editor, dataContext)) return false
    if (dataContext?.actionStartedFromVim == true) return false
    return true
  }

  final override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return isThisHandlerEnabled(editor, caret, dataContext) || nextHandler.isEnabled(editor, caret, dataContext)
  }
}

/**
 * Known conflicts & solutions:
 * - Smart step into - set handler after
 * - Python notebooks - set handler after
 * - Ace jump - set handler after
 * - Lookup - doesn't intersect with enter anymore
 * - App code - set handler after
 * - Template - doesn't intersect with enter anymore
 */
internal class VimEnterHandler(nextHandler: EditorActionHandler) : VimKeyHandler(nextHandler) {
  override val key: String = "<CR>"

  override fun isHandlerEnabled(editor: Editor, dataContext: DataContext?): Boolean {
    if (!super.isHandlerEnabled(editor, dataContext)) return false
    // This is important for one-line editors, to turn off enter.
    // Some one-line editors rely on the fact that there are no enter actions registered. For example, hash search in git
    // See VIM-2974 for example where it was broken
    return !editor.isOneLineMode
  }
}

/**
 * Known conflicts & solutions:
 *
 * - Smart step into - set handler after
 * - Python notebooks - set handler before - yes, we have <CR> as "after" and <esc> as before. I'm not completely sure
 *   why this combination is correct, but other versions don't work.
 * - Ace jump - set handler after
 * - Lookup - It disappears after putting our esc before templateEscape. But I'm not sure why it works like that
 * - App code - Need to review
 * - Template - Need to review
 */
internal class VimEscHandler(nextHandler: EditorActionHandler) : VimKeyHandler(nextHandler) {
  override val key: String = "<Esc>"
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
  when {
    s.keyCode == KeyEvent.VK_ENTER -> return editor.mode in listOf(
      CommandState.Mode.COMMAND,
      CommandState.Mode.INSERT,
      CommandState.Mode.VISUAL,
      CommandState.Mode.REPLACE,
    )
    s.keyCode == KeyEvent.VK_ESCAPE -> return editor.mode in listOf(
      CommandState.Mode.COMMAND,
      CommandState.Mode.INSERT,
      CommandState.Mode.VISUAL,
      CommandState.Mode.REPLACE,
    )
  }
  return false
}
