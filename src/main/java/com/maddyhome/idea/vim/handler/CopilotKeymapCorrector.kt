/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.SingleAlarm
import com.jetbrains.rd.util.ConcurrentHashMap
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.key


// We use alarm with delay to avoid many actions in case many events are fired at the same time
// [VERSION UPDATE] 2023.3+ Replace SingleAlarm with coroutine flows https://youtrack.jetbrains.com/articles/IJPL-A-8/Alarm-Alternative
internal val correctorRequester = SingleAlarm({ correctCopilotKeymap() }, 1_000)

private val LOG = logger<CopilotKeymapCorrector>()

internal class CopilotKeymapCorrector : StartupActivity {
  override fun runActivity(project: Project) {
    correctorRequester.request()
  }
}

internal class IdeaVimCorrectorKeymapChangedListener : KeymapManagerListener {
  override fun activeKeymapChanged(keymap: Keymap?) {
    correctorRequester.request()
  }

  override fun shortcutChanged(keymap: Keymap, actionId: String) {
    correctorRequester.request()
  }

  override fun shortcutChanged(keymap: Keymap, actionId: String, fromSettings: Boolean) {
    correctorRequester.request()
  }
}

private val copilotHideActionMap = ConcurrentHashMap<String, Unit>()

/**
 * See VIM-3206
 * The user expected to both copilot suggestion and the insert mode to be exited on a single esc.
 * However, for the moment, the first esc hides copilot suggestion and the second one exits insert mode.
 * To fix this, we remove the esc shortcut from the copilot action if the IdeaVim is active.
 *
 * This workaround is not the best solution, however, I don't see the better way with the current architecture of
 *   actions and EditorHandlers. Firstly, I wanted to suggest to copilot to migrate to EditorActionHandler as well,
 *   but this doesn't seem correct for me because in this case the user will lose an ability to change the shorcut for
 *   it. It seems like copilot has a similar problem as we do - we don't want to make a handler for "Editor enter action",
 *   but a handler for the esc key press. And, moreover, be able to communicate with other plugins about the ordering.
 *   Before this feature is implemented, hiding the copilot suggestion on esc looks like a good workaround.
 */
private fun correctCopilotKeymap() {
  // This is needed to initialize the injector in case this verification is called to fast
  VimPlugin.getInstance()

  if (injector.enabler.isEnabled()) {
    val keymap = KeymapManagerEx.getInstanceEx().activeKeymap
    val res = keymap.getShortcuts("copilot.disposeInlays")
    if (res.isEmpty()) return


    val escapeShortcut = res.find { it.toString() == "[pressed ESCAPE]" } ?: return
    keymap.removeShortcut("copilot.disposeInlays", escapeShortcut)
    copilotHideActionMap[keymap.name] = Unit
    LOG.info("Remove copilot escape shortcut from keymap ${keymap.name}")
  }
  else {
    copilotHideActionMap.forEach { (name, _) ->
      val keymap = KeymapManagerEx.getInstanceEx().getKeymap(name) ?: return@forEach
      val currentShortcuts = keymap.getShortcuts("copilot.disposeInlays")
      if ("[pressed ESCAPE]" !in currentShortcuts.map { it.toString() }) {
        keymap.addShortcut("copilot.disposeInlays", KeyboardShortcut(key("<esc>"), null))
      }
      LOG.info("Restore copilot escape shortcut in keymap ${keymap.name}")
    }
  }
}
