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

package com.maddyhome.idea.vim

import com.intellij.openapi.diagnostic.Logger
import com.maddyhome.idea.vim.helper.IjActionExecutor
import com.maddyhome.idea.vim.helper.VimActionExecutor
import com.maddyhome.idea.vim.newapi.IjNativeActionManager
import com.maddyhome.idea.vim.newapi.IjVimLogger
import com.maddyhome.idea.vim.newapi.NativeActionManager
import com.maddyhome.idea.vim.newapi.VimLogger

interface VimInjector {
  fun <T : Any> getLogger(clazz: Class<T>): VimLogger
  val actionExecutor: VimActionExecutor
  val nativeActionManager: NativeActionManager
}

class IjVimInjector : VimInjector {
  override fun <T : Any> getLogger(clazz: Class<T>): VimLogger = IjVimLogger(Logger.getInstance(clazz::class.java))
  override val actionExecutor: VimActionExecutor = IjActionExecutor()
  override val nativeActionManager: NativeActionManager = IjNativeActionManager()
}

// We should inject logger here somehow
var injector: VimInjector = IjVimInjector()

inline fun <reified T : Any> vimLogger(): VimLogger = injector.getLogger(T::class.java)
