/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.actionSystem.EditorActionHandlerBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * Logs the chain of handlers for esc and enter
 *
 * As we made a migration to the new way of handling esc keys (VIM-2974), we may face several issues around that
 * One of the possible issues is that some plugin may also register a shortcut for this key and do not pass
 * the control to the next handler. In this way, the esc won't work, but there will be no exceptions.
 *
 * This is a logger that logs the chain of handlers.
 *
 * Strictly speaking, such access to the extension point is not allowed by the platform. But we can't do this thing
 *   otherwise, so let's use it as long as we can.
 */
internal class EditorHandlersChainLogger : StartupActivity {
  @Suppress("UnresolvedPluginConfigReference")
  private val editorHandlers = ExtensionPointName<EditorActionHandlerBean>("com.intellij.editorActionHandler")

  override fun runActivity(project: Project) {
    val escHandlers = editorHandlers.extensionList
      .filter { it.action == "EditorEscape" }
      .joinToString("\n") { it.implementationClass }
    val enterHandlers = editorHandlers.extensionList
      .filter { it.action == "EditorEnter" }
      .joinToString("\n") { it.implementationClass }

    LOG.info("Esc handlers chain:\n$escHandlers")
    LOG.info("Enter handlers chain:\n$enterHandlers")
  }

  companion object {
    val LOG = logger<EditorHandlersChainLogger>()
  }
}