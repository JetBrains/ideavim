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
import com.maddyhome.idea.vim.newapi.runFromVimKey
import com.maddyhome.idea.vim.newapi.vim

abstract class OctopusHandler(private val nextHandler: EditorActionHandler) : EditorActionHandler() {

  abstract fun executeHandler(editor: Editor, caret: Caret?, dataContext: DataContext?)
  fun isHandlerEnabled(editor: Editor?, dataContext: DataContext?): Boolean {
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

class VimEnterHandler(nextHandler: EditorActionHandler) : OctopusHandler(nextHandler) {
  override fun executeHandler(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    val enterKey = injector.parser.parseKeys("<CR>").first()
    val context = injector.executionContextManager.onEditor(editor.vim, dataContext?.vim)
    KeyHandler.getInstance().handleKey(editor.vim, enterKey, context)
    println("")
  }
}

/**
 * Experiment: At the moment, IdeaVim intersects all shortcuts and sends the to [KeyHandler]
 * However, this doesn't seem to be a good solution as other handlers are overridden by vim.
 * If this option is enabled, vim will connect to IDE via EditorActionHandler extension point
 *   what seems to be a way better solution as this is a correct way to override editor actions like enter, right, etc.
 */
const val enableOctopus = false