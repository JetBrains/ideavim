/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

/**
 * Actions that can be used in `:action` and `<Action>(...)`.
 *
 * Those are executed on the frontend, so in split mode the backend's own ActionManager isn't enough: the frontend
 * sends its actions over [ActionIdRemoteApi] and they are kept here. In monolith and on the frontend, nothing is
 * received and the local ActionManager already has every action.
 */
@Service(Service.Level.APP)
class FrontendActionIds {
  /** Actions received from the frontend, by ID */
  @Volatile
  var actions: Map<String, ActionIdInfo> = emptyMap()

  fun allActionIds(): Set<String> = ActionManager.getInstance().getActionIdList("").toSet() + actions.keys

  /** Prefers the local action, the frontend only knows the actions missing here */
  fun findInfo(actionId: String): ActionIdInfo? {
    val action = ActionManager.getInstance().getActionOrStub(actionId) ?: return actions[actionId]
    return ActionIdInfo.of(actionId, action)
  }

  companion object {
    @JvmStatic
    fun getInstance(): FrontendActionIds = service()
  }
}
