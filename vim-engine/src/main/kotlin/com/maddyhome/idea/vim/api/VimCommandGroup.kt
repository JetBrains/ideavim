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

public interface VimCommandGroup {
  public fun isAlias(command: String): Boolean
  public fun hasAlias(name: String): Boolean
  public fun getAliasCommand(command: String, count: Int): GoalCommand
  public fun setAlias(name: String, commandAlias: CommandAlias)
  public fun removeAlias(name: String)
  public fun listAliases(): Set<Map.Entry<String, CommandAlias>>
  public fun resetAliases()

  public companion object {
    @NonNls
    public val BLACKLISTED_ALIASES: Array<String> = arrayOf("X", "Next", "Print")
  }
}
