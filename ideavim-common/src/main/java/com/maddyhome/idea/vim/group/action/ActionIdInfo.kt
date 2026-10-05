/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import kotlinx.serialization.Serializable

/**
 * An action ID with the action's text and description, shown in the ideavimrc completion lookup.
 * Sent via [ActionIdRemoteApi] from the frontend to the backend.
 */
@Serializable
data class ActionIdInfo(
  val id: String,
  val text: String?,
  val description: String?,
) {
  companion object {
    /** Reads the text and description of [action], which can be an action stub, so the action is not instantiated */
    fun of(id: String, action: AnAction): ActionIdInfo {
      val presentation = action.templatePresentation
      return ActionIdInfo(id, presentation.text?.ifBlank { null }, presentation.description?.ifBlank { null })
    }

    /** All actions of the local ActionManager */
    fun collect(): List<ActionIdInfo> {
      val actionManager = ActionManager.getInstance()
      return actionManager.getActionIdList("").mapNotNull { id -> actionManager.getActionOrStub(id)?.let { of(id, it) } }
    }
  }
}
