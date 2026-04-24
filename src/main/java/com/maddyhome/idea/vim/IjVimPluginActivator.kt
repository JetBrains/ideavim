/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.keymap.impl.DefaultKeymap
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.SlowOperations
import com.maddyhome.idea.vim.action.FrontendCommandProvider
import com.maddyhome.idea.vim.api.VimPluginActivator
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.VimExtensionRegistrar
import com.maddyhome.idea.vim.helper.MacKeyRepeat
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.newapi.IjVimSearchGroup
import com.maddyhome.idea.vim.ui.StatusBarIconFactory
import com.maddyhome.idea.vim.vimscript.model.commands.IntellijExCommandProvider
import com.maddyhome.idea.vim.vimscript.model.functions.IntellijFunctionProvider
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage
import com.maddyhome.idea.vim.vimscript.services.VimRcService.executeIdeaVimRc

/**
 * Handles the frontend-facing plugin activation/deactivation lifecycle.
 *
 * This class contains the logic that was previously in VimPlugin.turnOnPlugin/turnOffPlugin.
 * It's separated to decouple VimPlugin.java from frontend-only classes like VimListenerManager,
 * RegisterActions, StatusBarIconFactory, etc.
 *
 * This class lives in the frontend module where all its dependencies (VimListenerManager,
 * RegisterActions, StatusBarIconFactory, etc.) are also located.
 */
internal class IjVimPluginActivator : VimPluginActivator {

  private val log = Logger.getInstance(IjVimPluginActivator::class.java)
  private var stateUpdated = false

  /**
   * IdeaVim plugin activation.
   * This is an important operation and some commands ordering should be preserved.
   * Please make sure that the documentation of this function is in sync with the code.
   *
   * 1. Update state - schedules a state update (shows dialogs to the user)
   * 2. Command registration - BEFORE ~/.ideavimrc execution
   *    2.1 Register vim actions in command mode
   *    2.2 Register extensions
   *    2.3 Register functions
   * 3. Options initialisation
   * 4. ~/.ideavimrc execution
   * 5. Components initialization - AFTER ideavimrc execution
   *    (VimListenerManager accesses `number` option and guicaret)
   */
  override fun activate() {
    // 1) Update state
    ApplicationManager.getApplication().invokeLater(this::updateState)

    // 2) Command registration
    // 2.1) Register vim actions in command mode
    RegisterActions.registerCommandProvider(FrontendCommandProvider)
    RegisterActions.registerActions()

    // 2.2) Register ex commands
    VimscriptParser.registerCommandProvider(IntellijExCommandProvider)

    // 2.3) Register extensions
    (injector.extensionRegistrator as VimExtensionRegistrar).registerExtensions()

    // 2.4) Register functions
    (injector.functionService as FunctionStorage).registerFunctionProvider(IntellijFunctionProvider)
    injector.functionService.registerHandlers()

    // 3) Option initialisation
    injector.optionGroup.initialiseOptions()

    // 4) ~/.ideavimrc execution
    // Evaluate in the context of the fallback window, to capture local option state, to copy to the first editor window
    try {
      SlowOperations.knownIssue("VIM-3661").use {
        registerIdeavimrc()
      }
    } catch (e: Exception) {
      log.error("Failed to register ideavimrc", e)
    }

    // 5) Turning on should be performed after all commands registration
    (VimPlugin.getSearch() as IjVimSearchGroup).turnOn()
    VimListenerManager.turnOn()
  }

  override fun deactivate(unsubscribe: Boolean) {
    val searchGroup = VimPlugin.getSearchIfCreated() as IjVimSearchGroup?
    searchGroup?.turnOff()

    if (unsubscribe) {
      VimListenerManager.turnOff()
    }

    // Use getServiceIfCreated to avoid creating the service during the dispose (this is prohibited by the platform)
    val commandLineService = ApplicationManager.getApplication()
      .getServiceIfCreated(com.maddyhome.idea.vim.api.VimCommandLineService::class.java)
    // VIM-4115: close() clears editor mode, KeyHandlerState.commandLineCommandBuilder, and the panel
    // together. fullReset() alone only deactivates the panel; the KeyHandler singleton retains the
    // stale CMD_LINE builder across disable/enable and NPEs on the next Esc.
    commandLineService?.getActiveCommandLine()?.close(refocusOwningEditor = true, resetCaret = false)
    commandLineService?.fullReset()

    // Unregister vim actions in command mode
    RegisterActions.unregisterActions()
  }

  override fun updateStatusBarIcon() {
    StatusBarIconFactory.Util.updateIcon()
  }

  private var ideavimrcRegistered = false

  private fun registerIdeavimrc() {
    if (ideavimrcRegistered) return
    ideavimrcRegistered = true

    if (!ApplicationManager.getApplication().isUnitTestMode) {
      try {
        injector.optionGroup.startInitVimRc()
        executeIdeaVimRc(injector.fallbackWindow)
      } finally {
        injector.optionGroup.endInitVimRc()
      }
    }
  }

  private fun updateState() {
    if (stateUpdated) return
    if (VimPlugin.isEnabled() && !ApplicationManager.getApplication().isUnitTestMode) {
      stateUpdated = true
      if (SystemInfo.isMac) {
        val enabled = MacKeyRepeat.isEnabled
        val isKeyRepeat = VimPlugin.getEditor().isKeyRepeat()
        if ((enabled == null || !enabled) && (isKeyRepeat == null || isKeyRepeat)) {
          // This system property is used in IJ ui robot to hide the startup tips
          val showNotification =
            System.getProperty("ide.show.tips.on.startup.default.value", "true").toBoolean()
          log.info("Do not show mac repeat notification because ide.show.tips.on.startup.default.value=false")
          if (showNotification) {
            if (VimPlugin.getNotifications(null).enableRepeatingMode() == Messages.YES) {
              VimPlugin.getEditor().setKeyRepeat(true)
              MacKeyRepeat.isEnabled = true
            } else {
              VimPlugin.getEditor().setKeyRepeat(false)
            }
          }
        }
      }

      val previousStateVersion = VimPlugin.getInstance().previousStateVersion
      val previousKeyMap = VimPlugin.getInstance().previousKeyMap

      if (previousStateVersion > 0 && previousStateVersion < 3) {
        val manager = KeymapManagerEx.getInstanceEx()
        var keymap: Keymap? = null
        if (previousKeyMap != null) {
          keymap = manager.getKeymap(previousKeyMap)
        }
        if (keymap == null) {
          keymap = manager.getKeymap(DefaultKeymap.getInstance().defaultKeymapName)
        }
        assert(keymap != null) { "Default keymap not found" }
        manager.activeKeymap = keymap!!
      }
      if (previousStateVersion > 0 && previousStateVersion < 4) {
        VimPlugin.getNotifications(null).noVimrcAsDefault()
      }
    }
  }
}
