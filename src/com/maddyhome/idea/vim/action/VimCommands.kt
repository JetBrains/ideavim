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

import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.handler.VimActionHandler

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
sealed class VimCommandActionBase {

  val handler by lazy {
    makeActionHandler()
  }

  protected abstract fun makeActionHandler(): EditorActionHandlerBase
}

abstract class VimCommandAction : VimCommandActionBase() {
  abstract override fun makeActionHandler(): VimActionHandler
}

abstract class TextObjectAction : VimCommandActionBase() {
  abstract override fun makeActionHandler(): TextObjectActionHandler
}

abstract class MotionEditorAction : VimCommandActionBase() {
  abstract override fun makeActionHandler(): MotionActionHandler
}
