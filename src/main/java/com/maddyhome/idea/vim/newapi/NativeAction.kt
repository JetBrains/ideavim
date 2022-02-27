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

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.actionSystem.AnAction
import com.maddyhome.idea.vim.api.ExecutionContext

interface NativeAction {
  val action: Any
}

fun NativeAction?.execute(context: ExecutionContext) {
  if (this == null) return
  injector.actionExecutor.executeAction(this, context)
}

val AnAction.vim: IjNativeAction
  get() = IjNativeAction(this)

class IjNativeAction(override val action: AnAction) : NativeAction

interface NativeActionManager {
  val enterAction: NativeAction?
  val createLineAboveCaret: NativeAction?
  val joinLines: NativeAction?
  val indentLines: NativeAction?
  val saveAll: NativeAction?
  val saveCurrent: NativeAction?
}
