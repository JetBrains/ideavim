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
import com.maddyhome.idea.vim.common.VimMessages
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.group.VimChangeGroup
import com.maddyhome.idea.vim.group.VimKeyGroup
import com.maddyhome.idea.vim.group.VimProcessGroup
import com.maddyhome.idea.vim.group.VimRegisterGroup
import com.maddyhome.idea.vim.helper.IjActionExecutor
import com.maddyhome.idea.vim.helper.VimActionExecutor
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

  // TODO We should somehow state that [OptionServiceImpl] can be used from any implementation
  val optionService: OptionService
}

class IjVimInjector : VimInjector {
  override fun <T : Any> getLogger(clazz: Class<T>): VimLogger = IjVimLogger(Logger.getInstance(clazz::class.java))

  // TODO Inject via application service
  override val actionExecutor: VimActionExecutor = IjActionExecutor()
  override val nativeActionManager: NativeActionManager = IjNativeActionManager()
  override val messages: VimMessages = IjVimMessages()
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
  override val application: VimApplication = IjVimApplication()

  override val optionService: OptionService
    get() = service()
}

// We should inject logger here somehow
var injector: VimInjector = IjVimInjector()

inline fun <reified T : Any> vimLogger(): VimLogger = injector.getLogger(T::class.java)
