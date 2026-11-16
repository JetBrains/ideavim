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

interface VimActionExecutor {

  val ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE: String
  val ACTION_COLLAPSE_ALL_REGIONS: String
  val ACTION_COLLAPSE_REGION: String
  val ACTION_COLLAPSE_REGION_RECURSIVELY: String
  val ACTION_EXPAND_ALL_REGIONS: String
  val ACTION_EXPAND_REGION: String
  val ACTION_EXPAND_REGION_RECURSIVELY: String
  val ACTION_EXPAND_COLLAPSE_TOGGLE: String
  val ACTION_UNDO: String
  val ACTION_REDO: String

  /**
   * Execute an action
   *
   * @param action  The action to execute
   * @param context The context to run it in
   */
  fun executeAction(editor: VimEditor?, action: NativeAction, context: ExecutionContext): Boolean

  /**
   * Execute an action
   *
   * @param action  The action to execute
   */
  fun executeAction(editor: VimEditor?, action: NativeAction): Boolean

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  fun executeAction(editor: VimEditor, name: @NonNls String, context: ExecutionContext): Boolean

  /**
   * Execute an action by name, telling whoever listens to IDE actions where it was invoked from
   *
   * The place travels with the action itself, so a listener sees it even when the action runs later, asynchronously.
   * Use it to mark an invocation that a listener must treat differently from other invocations of the same action, e.g.
   * [com.maddyhome.idea.vim.tag.TagStackPlace.TAG_STACK_PLACE].
   *
   * Implementations that don't know about places run the action as [executeAction] without a place does.
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   * @param place   Where the action reports being invoked from, or `null` for the usual place
   */
  fun executeAction(editor: VimEditor, name: @NonNls String, context: ExecutionContext, place: String?): Boolean =
    executeAction(editor, name, context)

  fun executeCommand(
    editor: VimEditor?,
    runnable: Runnable,
    name: String?,
    groupId: Any?,
  )

  fun executeEsc(editor: VimEditor, context: ExecutionContext): Boolean

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
