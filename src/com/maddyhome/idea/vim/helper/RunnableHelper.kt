/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
package com.maddyhome.idea.vim.helper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

/**
 * This provides some helper methods to run code as a command and an application write action
 */
object RunnableHelper {
  private val logger = logger<RunnableHelper>()

  @JvmStatic
  fun runReadCommand(project: Project?, cmd: Runnable, name: String?, groupId: Any?) {
    logger.debug { "Run read command: $name" }
    CommandProcessor.getInstance().executeCommand(project, { ApplicationManager.getApplication().runReadAction(cmd) }, name, groupId)
  }

  @JvmStatic
  fun runWriteCommand(project: Project?, cmd: Runnable, name: String?, groupId: Any?) {
    logger.debug { "Run write command: $name" }
    CommandProcessor.getInstance().executeCommand(project, { ApplicationManager.getApplication().runWriteAction(cmd) }, name, groupId)
  }
}
