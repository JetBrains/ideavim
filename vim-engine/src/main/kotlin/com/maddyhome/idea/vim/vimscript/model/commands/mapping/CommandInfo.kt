/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.maddyhome.idea.vim.command.MappingMode
import org.jetbrains.annotations.NonNls

internal class CommandInfo(
  @NonNls val prefix: String,
  @NonNls suffix: String,
  val mappingModes: Set<MappingMode>,
  val isRecursive: Boolean,
  val bang: Boolean = false,
) {
  val command = if (suffix.isBlank()) prefix else "$prefix[$suffix]"
}
