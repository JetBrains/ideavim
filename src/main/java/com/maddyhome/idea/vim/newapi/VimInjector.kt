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

import com.maddyhome.idea.vim.api.ExecutionContextManager
import com.maddyhome.idea.vim.api.VimApplication
import com.maddyhome.idea.vim.api.VimDigraphGroup
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimEnabler
import com.maddyhome.idea.vim.api.VimMessages
import com.maddyhome.idea.vim.api.VimProcessGroup
import com.maddyhome.idea.vim.api.VimRegisterGroup
import com.maddyhome.idea.vim.api.VimStringParser
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.VimMachine
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.group.VimChangeGroup
import com.maddyhome.idea.vim.group.VimKeyGroup
import com.maddyhome.idea.vim.group.visual.VimVisualMotionGroup
import com.maddyhome.idea.vim.helper.VimActionExecutor
import com.maddyhome.idea.vim.options.OptionService

interface VimInjector {
  fun <T : Any> getLogger(clazz: Class<T>): VimLogger
  val actionExecutor: VimActionExecutor
  val nativeActionManager: NativeActionManager
  val messages: VimMessages
  val registerGroup: VimRegisterGroup
  val registerGroupIfCreated: VimRegisterGroup?
  val changeGroup: VimChangeGroup
  val processGroup: VimProcessGroup
  val keyGroup: VimKeyGroup
  val application: VimApplication
  val executionContextManager: ExecutionContextManager
  val digraphGroup: VimDigraphGroup
  val visualMotionGroup: VimVisualMotionGroup

  val vimMachine: VimMachine

  val enabler: VimEnabler

  // TODO We should somehow state that [OptionServiceImpl] can be used from any implementation
  val optionService: OptionService

  val parser: VimStringParser

  fun commandStateFor(editor: VimEditor): CommandState
}

// We should inject logger here somehow
lateinit var injector: VimInjector

inline fun <reified T : Any> vimLogger(): VimLogger = injector.getLogger(T::class.java)
