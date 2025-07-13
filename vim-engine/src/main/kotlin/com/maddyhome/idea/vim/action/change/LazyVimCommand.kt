/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.vimscript.model.LazyInstance
import com.maddyhome.idea.vim.key.VimKeyStroke

open class LazyVimCommand(
  val keys: Set<List<VimKeyStroke>>,
  val modes: Set<MappingMode>,
  className: String,
  classLoader: ClassLoader,
) : LazyInstance<EditorActionHandlerBase>(className, classLoader) {
  val actionId: String = EditorActionHandlerBase.getActionId(className)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LazyVimCommand

    if (keys != other.keys) return false
    if (modes != other.modes) return false
    if (actionId != other.actionId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = keys.hashCode()
    result = 31 * result + modes.hashCode()
    result = 31 * result + actionId.hashCode()
    return result
  }

  override fun toString(): String {
    return actionId
  }
}