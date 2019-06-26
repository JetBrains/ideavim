/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState


/**
 * Handler for common usage
 */
sealed class VimActionHandler(myRunForEachCaret: Boolean) : EditorActionHandlerBase(myRunForEachCaret) {
  abstract class ForEachCaret : VimActionHandler(true) {
    abstract fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean
  }

  abstract class SingleExecution : VimActionHandler(false) {
    abstract fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean
  }

  override fun baseExecute(editor: Editor, caret: Caret?, context: DataContext, cmd: Command): Boolean {
    return when (this) {
      is ForEachCaret -> caret == null || execute(editor, caret, context, cmd)
      is SingleExecution -> execute(editor, context, cmd)
    }
  }
}

abstract class EditorActionHandlerBase(myRunForEachCaret: Boolean) : EditorActionHandler(myRunForEachCaret) {

  abstract class ForEachCaret : EditorActionHandlerBase(true) {
    abstract fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean

    final override fun baseExecute(editor: Editor, caret: Caret?, context: DataContext, cmd: Command): Boolean {
      if (caret == null) return false
      return execute(editor, caret, context, cmd)
    }
  }

  abstract class SingleExecution : EditorActionHandlerBase(false) {
    abstract fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean

    final override fun baseExecute(editor: Editor, caret: Caret?, context: DataContext, cmd: Command): Boolean {
      return execute(editor, context, cmd)
    }
  }

  abstract fun baseExecute(editor: Editor, caret: Caret?, context: DataContext, cmd: Command): Boolean

  public final override fun doExecute(_editor: Editor, caret: Caret?, context: DataContext) {
    if (!VimPlugin.isEnabled()) return

    val editor = InjectedLanguageUtil.getTopLevelEditor(_editor)
    logger.debug("doExecute")

    val state = CommandState.getInstance(editor)
    val cmd = state.command ?: run {
      VimPlugin.indicateError()
      return
    }

    if (!baseExecute(editor, caret, context, cmd)) VimPlugin.indicateError()
  }

  open fun process(cmd: Command) {
    // No-op
  }

  private companion object {
    private val logger = Logger.getInstance(EditorActionHandlerBase::class.java.name)
  }
}
