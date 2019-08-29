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

package com.maddyhome.idea.vim.action

import com.google.common.collect.ImmutableSet
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.noneOfEnum
import java.util.*
import javax.swing.KeyStroke

/**
 * Structure of commands
 *
 *                                          VimCommandActionBase
 *                                        (Can't be used directly)
 *                                                  |
 *         -------------------------------------------------------------------------------------
 *         |                                        |                                          |
 *     MotionEditorAction                    TextObjectAction                          VimCommandAction
 * (accepts MotionActionHandler)        (accepts TextObjectActionHandler)        (accepts VimActionHandler)
 *
 * See also EditorActionHandlerBase.kt for handlers structure
 */

/**
 * Action that represents a Vim command.
 *
 * Actions should be registered in resources/META-INF/plugin.xml and in package-info.java
 * inside [com.maddyhome.idea.vim.action].
 *
 * @author vlan
 */
sealed class VimCommandActionBase : EditorAction(null) {

  var id = ""
  var text = ""

  init {
    @Suppress("LeakingThis")
    setupHandler(makeActionHandler())
  }

  protected abstract fun makeActionHandler(): EditorActionHandlerBase

  abstract val mappingModes: Set<MappingMode>

  abstract val keyStrokesSet: Set<List<KeyStroke>>

  abstract val type: Command.Type

  open val argumentType: Argument.Type = Argument.Type.NONE

  /**
   * Returns various binary flags for the command.
   *
   * These legacy flags will be refactored in future releases.
   *
   * @see com.maddyhome.idea.vim.command.Command
   */
  open val flags: EnumSet<CommandFlags> = noneOfEnum()

  protected companion object {
    @JvmStatic
    fun parseKeysSet(vararg keyStrings: String): Set<List<KeyStroke>> {
      val builder = ImmutableSet.builder<List<KeyStroke>>()
      for (keyString in keyStrings) {
        builder.add(StringHelper.parseKeys(keyString))
      }
      return builder.build()
    }
  }
}

abstract class VimCommandAction : VimCommandActionBase() {
  abstract override fun makeActionHandler(): VimActionHandler
}

abstract class TextObjectAction : VimCommandActionBase() {
  abstract override fun makeActionHandler(): TextObjectActionHandler

  fun getRange(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): TextRange? {
    val actionHandler = handler as? TextObjectActionHandler
      ?: throw RuntimeException("TextObjectAction works only with TextObjectActionHandler")

    return actionHandler.getRange(editor, caret, context, count, rawCount, argument)
  }

  final override val type: Command.Type = Command.Type.MOTION
}

abstract class MotionEditorAction : VimCommandActionBase() {
  abstract override fun makeActionHandler(): MotionActionHandler

  fun getOffset(editor: Editor,
                caret: Caret,
                context: DataContext,
                count: Int,
                rawCount: Int,
                argument: Argument?): Int {
    val actionHandler = handler as? MotionActionHandler
      ?: throw RuntimeException("MotionAction works only with MotionHandler")

    return when (actionHandler) {
      is MotionActionHandler.SingleExecution -> actionHandler.getOffset(editor, context, count, rawCount, argument)
      is MotionActionHandler.ForEachCaret -> actionHandler.getOffset(editor, caret, context, count, rawCount, argument)
    }
  }

  final override val type: Command.Type = Command.Type.MOTION
}

abstract class NativeAction : VimCommandAction() {

  abstract val actionName: String

  final override fun makeActionHandler(): VimActionHandler = object : VimActionHandler.SingleExecution() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
      KeyHandler.executeAction(actionName, context)
      return true
    }
  }
}
