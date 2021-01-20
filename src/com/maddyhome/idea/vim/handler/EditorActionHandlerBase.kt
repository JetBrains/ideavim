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
import org.jetbrains.annotations.NonNls
import java.util.*
import javax.swing.KeyStroke

/**
 * All the commands in IdeaVim should implement one of the following handlers and be registered in VimActions.xml
 * Check the KtDocs of handlers for the details.
 *
 * Structure of handlers:
 *
 * - [EditorActionHandlerBase]: Base handler for all handlers. Please don't use it directly.
 *  - [VimActionHandler]: .............. Common vim commands.. E.g.: u, <C-W>s, <C-D>.
 *  - [TextObjectActionHandler]: ....... Text objects. ....... E.g.: iw, a(, i>
 *  - [MotionActionHandler]: ........... Motion commands. .... E.g.: k, w, <Up>
 *  - [ChangeEditorActionHandler]: ..... Change commands. .... E.g.: s, r, gU
 *  - [VisualOperatorActionHandler]: ... Visual commands.
 *
 *  SpecialKeyHandlers are not presented here because these handlers are created to a limited set of commands and they
 *    are already implemented.
 */
abstract class EditorActionHandlerBase(private val myRunForEachCaret: Boolean) {
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

  abstract fun baseExecute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean

  fun execute(editor: Editor, context: DataContext) {
    val hostEditor: Editor = CommonDataKeys.HOST_EDITOR.getData(context) ?: editor
    val action = { caret: Caret -> doExecute(editor, caret, context) }
    if (myRunForEachCaret) {
      hostEditor.caretModel.runForEachCaret(action)
    } else {
      action(editor.caretModel.currentCaret)
    }
  }

  private fun doExecute(editor: Editor, caret: Caret, context: DataContext) {
    if (!VimPlugin.isEnabled()) return

    val topLevelEditor = editor.getTopLevelEditor()
    logger.debug("Execute command with handler: " + this.javaClass.name)

    val cmd = topLevelEditor.commandState.executingCommand ?: run {
      VimPlugin.indicateError()
      return
    }

    if (!baseExecute(topLevelEditor, caret, CaretSpecificDataContext(context, caret), cmd)) VimPlugin.indicateError()
  }

  open fun process(cmd: Command) {
    // No-op
  }

  companion object {
    private val logger = Logger.getInstance(EditorActionHandlerBase::class.java.name)

    fun parseKeysSet(keyStrings: List<String>) = keyStrings.map { StringHelper.parseKeys(it) }.toSet()

    @JvmStatic
    fun parseKeysSet(@NonNls vararg keyStrings: String): Set<List<KeyStroke>> = List(keyStrings.size) {
      StringHelper.parseKeys(keyStrings[it])
    }.toSet()

    @NonNls private const val VimActionPrefix = "Vim"

    @NonNls
    fun getActionId(classFullName: String): String {
      return classFullName
        .takeLastWhile { it != '.' }
        .let { if (it.startsWith(VimActionPrefix, true)) it else "$VimActionPrefix$it" }
    }
  }
}
