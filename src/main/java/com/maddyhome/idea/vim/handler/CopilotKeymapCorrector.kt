/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls
import java.util.concurrent.ConcurrentHashMap


// We use alarm with delay to avoid many actions in case many events are fired at the same time
internal val correctorRequester = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

private val LOG = logger<CopilotKeymapCorrector>()

internal class CopilotKeymapCorrector : ProjectActivity {
  override suspend fun execute(project: Project) {
    project.service<CopilotKeymapCorrectorService>().start()
    correctorRequester.emit(Unit)
  }
}

/**
 * At the moment of release 2023.3 there is a problem that starting a coroutine like this
 *   right in the project activity will block this project activity in tests.
 * To avoid that, there is an intermediate service that will allow to avoid this issue.
 *
 * However, in general we should start this coroutine right in the [CopilotKeymapCorrector]
 */
@OptIn(FlowPreview::class)
@Service(Service.Level.PROJECT)
internal class CopilotKeymapCorrectorService(private val cs: CoroutineScope) {
  fun start() {
    cs.launch {
      correctorRequester
        .debounce(5_000)
        .collectLatest { correctCopilotKeymap() }
    }
  }
}


internal class IdeaVimCorrectorKeymapChangedListener : KeymapManagerListener {
  override fun activeKeymapChanged(keymap: Keymap?) {
    check(correctorRequester.tryEmit(Unit))
  }

  override fun shortcutsChanged(keymap: Keymap, actionIds: @NonNls Collection<String>, fromSettings: Boolean) {
    check(correctorRequester.tryEmit(Unit))
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

  if (!enableOctopus) return
  if (injector.enabler.isEnabled()) {
    val keymap = KeymapManagerEx.getInstanceEx().activeKeymap
    val res = keymap.getShortcuts("copilot.disposeInlays")
    if (res.isEmpty()) return


    val escapeShortcut = res.find { it.toString() == "[pressed ESCAPE]" } ?: return
    keymap.removeShortcut("copilot.disposeInlays", escapeShortcut)
    copilotHideActionMap[keymap.name] = Unit
    LOG.info("Remove copilot escape shortcut from keymap ${keymap.name}")
  } else {
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
