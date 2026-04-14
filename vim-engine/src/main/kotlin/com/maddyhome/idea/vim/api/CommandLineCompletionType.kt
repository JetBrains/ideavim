/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

enum class CommandLineCompletionType {
  NONE,
  FILE,
}

object CommandCompletionTypes {
  private val commandToCompletionType = mapOf(
    "edit" to CommandLineCompletionType.FILE,
    "browse" to CommandLineCompletionType.FILE,
    "find" to CommandLineCompletionType.FILE,
    "source" to CommandLineCompletionType.FILE,
    "write" to CommandLineCompletionType.FILE,
    "read" to CommandLineCompletionType.FILE,
    "split" to CommandLineCompletionType.FILE,
    "vsplit" to CommandLineCompletionType.FILE,
  )

  fun getCompletionType(fullCommandName: String): CommandLineCompletionType {
    return commandToCompletionType[fullCommandName] ?: CommandLineCompletionType.NONE
  }
}
