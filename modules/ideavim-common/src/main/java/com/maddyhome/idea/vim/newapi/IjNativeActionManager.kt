/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.NativeAction
import com.maddyhome.idea.vim.api.NativeActionManager

@Service
internal class IjNativeActionManager : NativeActionManager {
  override val enterAction: NativeAction? by lazy { byName(IdeActions.ACTION_EDITOR_ENTER) }
  override val createLineAboveCaret: NativeAction? by lazy { byName("EditorStartNewLineBefore") }
  override val joinLines: NativeAction? by lazy { byName(IdeActions.ACTION_EDITOR_JOIN_LINES) }
  override val indentLines: NativeAction? by lazy { byName(IdeActions.ACTION_EDITOR_AUTO_INDENT_LINES) }
  override val saveAll: NativeAction? by lazy { byName("SaveAll") }
  override val saveCurrent: NativeAction? by lazy { byName("SaveDocument") }
  override val deleteAction: NativeAction? by lazy { byName(IdeActions.ACTION_EDITOR_DELETE) }

  private fun byName(name: String): IjNativeAction? {
    val action: AnAction? = ActionManager.getInstance().getAction(name)
    return action?.vim
  }
}

val AnAction.vim: IjNativeAction
  get() = IjNativeAction(this)

class IjNativeAction(override val action: AnAction) : NativeAction {
  override fun toString(): String {
    return "IjNativeAction(action=$action)"
  }
}
