/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.maddyhome.idea.vim.handler.ActionBeanClass
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.IjVimActionsInitiator
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

public object RegisterActions {
  @Deprecated("Please use @CommandOrMotion annotation instead")
  internal val VIM_ACTIONS_EP: ExtensionPointName<ActionBeanClass> = ExtensionPointName.create("IdeaVIM.vimAction")

  /**
   * Register all the key/action mappings for the plugin.
   */
  @JvmStatic
  public fun registerActions() {
    registerVimCommandActions()
    registerEmptyShortcuts()
    registerEpListener()
  }

  @Deprecated("Moving to annotations approach instead of xml")
  private fun registerEpListener() {
    // IdeaVim doesn't support contribution to VIM_ACTIONS_EP extension point, so technically we can skip this update,
    //   but let's support dynamic plugins in a more classic way and reload actions on every EP change.
    VIM_ACTIONS_EP.addChangeListener({
      unregisterActions()
      registerActions()
    }, VimPlugin.getInstance())
  }

  public fun findAction(id: String): EditorActionHandlerBase? {
    return VIM_ACTIONS_EP.getExtensionList(ApplicationManager.getApplication()).stream()
      .filter { vimActionBean: ActionBeanClass -> vimActionBean.actionId == id }
      .findFirst().map { obj: ActionBeanClass -> obj.instance }
      .orElse(null)
  }

  public fun findActionOrDie(id: String): EditorActionHandlerBase {
    return findAction(id) ?: throw RuntimeException("Action $id is not registered")
  }

  @JvmStatic
  public fun unregisterActions() {
    val keyGroup = VimPlugin.getKeyIfCreated()
    keyGroup?.unregisterCommandActions()
  }

  private fun registerVimCommandActions() {
    val parser = VimPlugin.getKey()
    VIM_ACTIONS_EP.getExtensionList(ApplicationManager.getApplication()).stream().map { bean: ActionBeanClass? ->
      IjVimActionsInitiator(
        bean!!
      )
    }
      .forEach { actionHolder: IjVimActionsInitiator? ->
        parser.registerCommandAction(
          actionHolder!!
        )
      }
  }

  private fun registerEmptyShortcuts() {
    val parser = VimPlugin.getKey()

    // The {char1} <BS> {char2} shortcut is handled directly by KeyHandler#handleKey, so doesn't have an action. But we
    // still need to register the shortcut, to make sure the editor doesn't swallow it.
    parser
      .registerShortcutWithoutAction(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), MappingOwner.IdeaVim.System)
  }
}
