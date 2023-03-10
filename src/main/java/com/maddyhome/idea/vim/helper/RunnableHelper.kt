/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
internal object RunnableHelper {
  private val logger = logger<RunnableHelper>()

  @JvmStatic
  fun runReadCommand(project: Project?, cmd: Runnable, name: String?, groupId: Any?) {
    logger.debug { "Run read command: $name" }
    CommandProcessor.getInstance()
      .executeCommand(project, { ApplicationManager.getApplication().runReadAction(cmd) }, name, groupId)
  }

  @JvmStatic
  fun runWriteCommand(project: Project?, cmd: Runnable, name: String?, groupId: Any?) {
    logger.debug { "Run write command: $name" }
    CommandProcessor.getInstance()
      .executeCommand(project, { ApplicationManager.getApplication().runWriteAction(cmd) }, name, groupId)
  }
}
