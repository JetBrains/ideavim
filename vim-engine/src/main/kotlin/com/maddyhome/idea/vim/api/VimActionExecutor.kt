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
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import org.jetbrains.annotations.NonNls

interface VimActionExecutor {

  val ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE: String
  val ACTION_COLLAPSE_ALL_REGIONS: String
  val ACTION_COLLAPSE_REGION: String
  val ACTION_COLLAPSE_REGION_RECURSIVELY: String
  val ACTION_EXPAND_ALL_REGIONS: String
  val ACTION_EXPAND_REGION: String
  val ACTION_EXPAND_REGION_RECURSIVELY: String

  /**
   * Execute an action
   *
   * @param action  The action to execute
   * @param context The context to run it in
   */
  fun executeAction(action: NativeAction, context: ExecutionContext): Boolean

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  fun executeAction(name: @NonNls String, context: ExecutionContext): Boolean

  fun executeCommand(
    editor: VimEditor?,
    runnable: Runnable,
    name: String?,
    groupId: Any?,
  )

  fun executeEsc(context: ExecutionContext): Boolean

  fun executeVimAction(
    editor: VimEditor,
    cmd: EditorActionHandlerBase,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  )

  fun findVimAction(id: String): EditorActionHandlerBase?
  fun findVimActionOrDie(id: String): EditorActionHandlerBase

  fun getAction(actionId: String): NativeAction?
  fun getActionIdList(idPrefix: String): List<String>
}
