/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import org.jetbrains.annotations.NonNls

public interface VimActionExecutor {

  public val ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE: String
  public val ACTION_COLLAPSE_ALL_REGIONS: String
  public val ACTION_COLLAPSE_REGION: String
  public val ACTION_COLLAPSE_REGION_RECURSIVELY: String
  public val ACTION_EXPAND_ALL_REGIONS: String
  public val ACTION_EXPAND_REGION: String
  public val ACTION_EXPAND_REGION_RECURSIVELY: String

  /**
   * Execute an action
   *
   * @param action  The action to execute
   * @param context The context to run it in
   */
  public fun executeAction(editor: VimEditor?, action: NativeAction, context: ExecutionContext): Boolean

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  public fun executeAction(name: @NonNls String, context: ExecutionContext): Boolean

  public fun executeCommand(
    editor: VimEditor?,
    runnable: Runnable,
    name: String?,
    groupId: Any?,
  )

  public fun executeEsc(context: ExecutionContext): Boolean

  public fun executeVimAction(
    editor: VimEditor,
    cmd: EditorActionHandlerBase,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  )

  public fun findVimAction(id: String): EditorActionHandlerBase?
  public fun findVimActionOrDie(id: String): EditorActionHandlerBase

  public fun getAction(actionId: String): NativeAction?
  public fun getActionIdList(idPrefix: String): List<String>
}
