/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.actionSystem.EditorActionHandlerBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.maddyhome.idea.vim.api.key
import com.maddyhome.idea.vim.helper.keyStroke
import com.maddyhome.idea.vim.newapi.initInjector

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
internal class EditorHandlersChainLogger : ProjectActivity {
  @Suppress("UnresolvedPluginConfigReference")
  private val editorHandlers = ExtensionPointName<EditorActionHandlerBean>("com.intellij.editorActionHandler")

  override suspend fun execute(project: Project) {
    initInjector()

    if (!enableOctopus) return

    val escHandlers = editorHandlers.extensionList
      .filter { it.action == "EditorEscape" }
      .joinToString("\n") { it.implementationClass }
    val enterHandlers = editorHandlers.extensionList
      .filter { it.action == "EditorEnter" }
      .joinToString("\n") { it.implementationClass }

    LOG.info("Esc handlers chain:\n$escHandlers")
    LOG.info("Enter handlers chain:\n$enterHandlers")

    val keymapManager = KeymapManagerEx.getInstanceEx()
    val keymap = keymapManager.activeKeymap
    val keymapShortcutsForEsc = keymap.getShortcuts(IdeActions.ACTION_EDITOR_ESCAPE).joinToString()
    val keymapShortcutsForEnter = keymap.getShortcuts(IdeActions.ACTION_EDITOR_ENTER).joinToString()

    LOG.info("Active keymap (${keymap.name}) shortcuts for esc: $keymapShortcutsForEsc, Shortcuts for enter: $keymapShortcutsForEnter")

    val actionsForEsc = keymap.getActionIds(key("<esc>").keyStroke).joinToString("\n")
    val actionsForEnter = keymap.getActionIds(key("<enter>").keyStroke).joinToString("\n")

    LOG.info(
      "Also keymap (${keymap.name}) has " +
        "the following actions assigned to esc:\n$actionsForEsc " +
        "\nand following actions assigned to enter:\n$actionsForEnter"
    )
  }

  companion object {
    val LOG = logger<EditorHandlersChainLogger>()
  }
}