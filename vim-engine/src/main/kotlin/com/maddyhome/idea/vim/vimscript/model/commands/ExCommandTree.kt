/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.helper.indexOfOrNull
import com.maddyhome.idea.vim.helper.lastIndexOfOrNull

// TODO do we really need a tree structure here?
public class ExCommandTree {
  private val abbrevToCommand = mutableMapOf<String, String>()
  private val commandToInstance = mutableMapOf<String, LazyExCommandInstance>()

  public fun addCommand(commandsPattern: String, lazyInstance: LazyExCommandInstance) {
    val subCommands = parseCommandPattern(commandsPattern)
    for ((requiredPart, optionalPart) in subCommands) {
      val fullCommand = requiredPart + optionalPart
      commandToInstance[fullCommand] = lazyInstance

      for (i in (0..optionalPart.length)) {
        abbrevToCommand[requiredPart + optionalPart.substring(0, i)] = fullCommand
      }
    }
  }

  public fun getCommand(command: String): LazyExCommandInstance? {
    return abbrevToCommand[command]?.let { commandToInstance[it] }
  }

  private fun parseCommandPattern(commandsPattern: String): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    val commands = commandsPattern.split(",")
    for (command in commands) {
      val leftBraceIndex = command.indexOfOrNull('[')
      val rightBraceIndex = command.indexOfOrNull(']')
      if (
        (leftBraceIndex == null && rightBraceIndex != null) ||
        (leftBraceIndex != null && rightBraceIndex == null) ||
        (leftBraceIndex != null && rightBraceIndex != null && leftBraceIndex > rightBraceIndex) ||
        (leftBraceIndex != null && leftBraceIndex != command.lastIndexOfOrNull('[')) ||
        (rightBraceIndex != null && rightBraceIndex != command.lastIndexOfOrNull(']'))
      ) {
        throw RuntimeException("Invalid ex-command pattern $commandsPattern")
      }
      val primaryPart = command.substring(0, leftBraceIndex ?: command.length)
      val optionalPart = if (leftBraceIndex != null && rightBraceIndex != null) command.substring(leftBraceIndex + 1, rightBraceIndex) else ""
      result.add(Pair(primaryPart, optionalPart))
    }
    return result
  }
}