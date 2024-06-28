/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.CommandAlias
import com.maddyhome.idea.vim.common.GoalCommand
import org.jetbrains.annotations.NonNls

interface VimCommandGroup {
  fun isAlias(command: String): Boolean
  fun hasAlias(name: String): Boolean
  fun getAliasCommand(command: String, count: Int): GoalCommand
  fun setAlias(name: String, commandAlias: CommandAlias)
  fun removeAlias(name: String)
  fun listAliases(): Set<Map.Entry<String, CommandAlias>>
  fun resetAliases()

  companion object {
    @NonNls
    val BLACKLISTED_ALIASES: Array<String> = arrayOf("X", "Next", "Print")
  }
}
