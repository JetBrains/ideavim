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

import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.Logger
import com.maddyhome.idea.vim.api.ExecutionContextManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimEnabler
import com.maddyhome.idea.vim.api.VimMessages
import com.maddyhome.idea.vim.api.VimProcessGroup
import com.maddyhome.idea.vim.api.VimRegisterGroup
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.VimMachine
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.group.VimChangeGroup
import com.maddyhome.idea.vim.group.VimDigraphGroup
import com.maddyhome.idea.vim.group.VimKeyGroup
import com.maddyhome.idea.vim.group.visual.VimVisualMotionGroup
import com.maddyhome.idea.vim.helper.IjActionExecutor
import com.maddyhome.idea.vim.helper.VimActionExecutor
import com.maddyhome.idea.vim.helper.vimCommandState
import com.maddyhome.idea.vim.vimscript.services.OptionService

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

  fun commandStateFor(editor: VimEditor): CommandState
}

class IjVimInjector : VimInjector {
  override fun <T : Any> getLogger(clazz: Class<T>): VimLogger = IjVimLogger(Logger.getInstance(clazz::class.java))

  override val actionExecutor: VimActionExecutor
    get() = service<IjActionExecutor>()
  override val nativeActionManager: NativeActionManager
    get() = service<IjNativeActionManager>()
  override val messages: VimMessages
    get() = service<IjVimMessages>()
  override val registerGroup: VimRegisterGroup
    get() = service()
  override val registerGroupIfCreated: VimRegisterGroup?
    get() = serviceIfCreated()
  override val changeGroup: VimChangeGroup
    get() = service()
  override val processGroup: VimProcessGroup
    get() = service()
  override val keyGroup: VimKeyGroup
    get() = service()
  override val application: VimApplication
    get() = service<IjVimApplication>()
  override val executionContextManager: ExecutionContextManager
    get() = service<IjExecutionContextManager>()
  override val vimMachine: VimMachine
    get() = service<VimMachineImpl>()
  override val enabler: VimEnabler
    get() = service<IjVimEnabler>()
  override val digraphGroup: VimDigraphGroup
    get() = service()
  override val visualMotionGroup: VimVisualMotionGroup
    get() = service()

  override val optionService: OptionService
    get() = service()

  override fun commandStateFor(editor: VimEditor): CommandState {
    var res = editor.ij.vimCommandState
    if (res == null) {
      res = CommandState(editor)
      editor.ij.vimCommandState = res
    }
    return res
  }
}

// We should inject logger here somehow
var injector: VimInjector = IjVimInjector()

inline fun <reified T : Any> vimLogger(): VimLogger = injector.getLogger(T::class.java)
