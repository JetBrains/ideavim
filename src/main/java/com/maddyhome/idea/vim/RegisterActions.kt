/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim

import com.maddyhome.idea.vim.action.EngineCommandProvider
import com.maddyhome.idea.vim.action.IntellijCommandProvider
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.key.MappingOwner
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

object RegisterActions {
  /**
   * Register all the key/action mappings for the plugin.
   */
  @JvmStatic
  fun registerActions() {
    registerVimCommandActions()
    registerShortcutsWithoutActions()
  }

  fun findAction(id: String): EditorActionHandlerBase? {
    val commandBean = IntellijCommandProvider.getCommands().firstOrNull { it.actionId == id }
      ?: EngineCommandProvider.getCommands().firstOrNull { it.actionId == id } ?: return null
    return commandBean.instance
  }

  fun findActionOrDie(id: String): EditorActionHandlerBase {
    return findAction(id) ?: throw RuntimeException("Action $id is not registered")
  }

  @JvmStatic
  fun unregisterActions() {
    val keyGroup = VimPlugin.getKeyIfCreated()
    keyGroup?.unregisterCommandActions()
  }

  private fun registerVimCommandActions() {
    val parser = VimPlugin.getKey()
    IntellijCommandProvider.getCommands().forEach { parser.registerCommandAction(it) }
    EngineCommandProvider.getCommands().forEach { parser.registerCommandAction(it) }
  }

  private fun registerShortcutsWithoutActions() {
    val parser = VimPlugin.getKey()

    // The {char1} <BS> {char2} shortcut is handled directly by KeyHandler#handleKey, so doesn't have an action. But we
    // still need to register the shortcut, to make sure the editor doesn't swallow it.
    parser.registerShortcutWithoutAction(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), MappingOwner.IdeaVim.System)
  }
}
