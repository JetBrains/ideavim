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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.CaretSpecificDataContext
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.getTopLevelEditor
import com.maddyhome.idea.vim.helper.noneOfEnum
import java.util.*
import javax.swing.KeyStroke

/**
 * Structure of handlers
 * `~` - this symbol means that this handler cannot be used directly (only its children)
 * Almost each handler isn't usable by itself and has two children - "SingleExecution" and "ForEachCaret"
 *      which should be used
 *
 *                                         ~ EditorActionHandlerBase ~
 *                                                     |
 *               ----------------------------------------------------------------------------
 *                 |                                   |                                    |
 *          ~ ForEachCaret ~                   ~ SingleExecution ~                  ~ VimActionHandler ~
 *                 |                                   |                                /         \
 *       TextObjectActionHandler               MotionActionHandler                    /            \
 *                                                                             SingleExecution   ForEachCaret
 *                                                                                  |
 *                      -------------------------------------------------------------
 *                      |                                   |
 *        ~ ChangeEditorActionHandler ~         ~ VisualOperatorActionHandler ~
 *              /           \                         /         \
 *    SingleExecution    ForEachCaret         SingleExecution    ForEachCaret
 *
 *
 *  SpecialKeyHandlers are not presented here because these handlers are created to a limited set of commands and they
 *    are already implemented
 */

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

  final override fun baseExecute(editor: Editor, caret: Caret?, context: DataContext, cmd: Command): Boolean {
    return when (this) {
      is ForEachCaret -> caret == null || execute(editor, caret, context, cmd)
      is SingleExecution -> execute(editor, context, cmd)
    }
  }
}

sealed class EditorActionHandlerBase(private val myRunForEachCaret: Boolean) {
  val id: String = getActionId(this::class.java.name)

  abstract val type: Command.Type

  open val argumentType: Argument.Type? = null

  /**
   * Returns various binary flags for the command.
   *
   * These legacy flags will be refactored in future releases.
   *
   * @see com.maddyhome.idea.vim.command.Command
   */
  open val flags: EnumSet<CommandFlags> = noneOfEnum()


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

  fun execute(editor: Editor, context: DataContext) {
    val hostEditor: Editor = CommonDataKeys.HOST_EDITOR.getData(context) ?: editor
    val action = { caret: Caret -> doExecute(editor, caret, context) }
    if (myRunForEachCaret) {
      hostEditor.caretModel.runForEachCaret(action)
    } else {
      action(editor.caretModel.currentCaret)
    }
  }

  private fun doExecute(_editor: Editor, caret: Caret, context: DataContext) {
    if (!VimPlugin.isEnabled()) return

    val editor = _editor.getTopLevelEditor()
    logger.debug("Execute command with handler: " + this.javaClass.name)

    val cmd = editor.commandState.command ?: run {
      VimPlugin.indicateError()
      return
    }

    if (!baseExecute(editor, caret, CaretSpecificDataContext(context, caret), cmd)) VimPlugin.indicateError()
  }

  open fun process(cmd: Command) {
    // No-op
  }

  companion object {
    private val logger = Logger.getInstance(EditorActionHandlerBase::class.java.name)

    fun parseKeysSet(keyStrings: List<String>) = keyStrings.map { StringHelper.parseKeys(it) }.toSet()

    @JvmStatic
    fun parseKeysSet(vararg keyStrings: String): Set<List<KeyStroke>> = List(keyStrings.size) {
      StringHelper.parseKeys(keyStrings[it])
    }.toSet()

    fun getActionId(classFullName: String): String {
      return classFullName
        .takeLastWhile { it != '.' }
        .let { if (it.startsWith("Vim", true)) it else "Vim$it" }
    }
  }
}
