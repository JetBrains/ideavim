/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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
import javax.swing.KeyStroke

// We use alarm with delay to avoid many notifications in case many events are fired at the same time
internal val keyCheckRequests = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

/**
 * This checker verifies that the keymap has a correct configuration that is required for IdeaVim plugin
 */
internal class KeymapChecker : ProjectActivity {
  override suspend fun execute(project: Project) {
    project.service<KeymapCheckerService>().start()
    keyCheckRequests.emit(Unit)
  }
}

/**
 * At the moment of release 2023.3 there is a problem that starting a coroutine like this
 *   right in the project activity will block this project activity in tests.
 * To avoid that, there is an intermediate service that will allow to avoid this issue.
 *
 * However, in general we should start this coroutine right in the [KeymapChecker]
 */
@OptIn(FlowPreview::class)
@Service(Service.Level.PROJECT)
internal class KeymapCheckerService(private val cs: CoroutineScope) {
  fun start() {
    cs.launch {
      keyCheckRequests
        .debounce(5_000)
        .collectLatest { verifyKeymap() }
    }
  }
}

internal class IdeaVimKeymapChangedListener : KeymapManagerListener {
  override fun activeKeymapChanged(keymap: Keymap?) {
    check(keyCheckRequests.tryEmit(Unit))
  }

  override fun shortcutChanged(keymap: Keymap, actionId: String) {
    check(keyCheckRequests.tryEmit(Unit))
  }

  override fun shortcutChanged(keymap: Keymap, actionId: String, fromSettings: Boolean) {
    check(keyCheckRequests.tryEmit(Unit))
  }
}

/**
 * After migration to the editor action handlers, we have to make sure that the keymap has a correct configuration.
 * For example, that esc key is assigned to esc editor action
 *
 * Usually this is not a problem because this is a standard mapping, but the problem may appear in a misconfiguration
 *   like it was in VIM-3204
 */
private fun verifyKeymap() {
  // This is needed to initialize the injector in case this verification is called to fast
  VimPlugin.getInstance()

  if (!enableOctopus) return
  if (!injector.enabler.isEnabled()) return

  val keymap = KeymapManagerEx.getInstanceEx().activeKeymap
  val keymapShortcutsForEsc = keymap.getShortcuts(IdeActions.ACTION_EDITOR_ESCAPE)
  val keymapShortcutsForEnter = keymap.getShortcuts(IdeActions.ACTION_EDITOR_ENTER)

  val issues = ArrayList<KeyMapIssue>()
  val correctShortcutMissing = keymapShortcutsForEsc
    .filterIsInstance<KeyboardShortcut>()
    .none { it.firstKeyStroke.toString() == "pressed ESCAPE" && it.secondKeyStroke == null }

  // We also check if there are any shortcuts starting from esc and with a second key. This should also be removed.
  // For example, VIM-3162 has a case when two escapes were assigned to editor escape action
  val shortcutsStartingFromEsc = keymapShortcutsForEsc
    .filterIsInstance<KeyboardShortcut>()
    .filter { it.firstKeyStroke.toString() == "pressed ESCAPE" && it.secondKeyStroke != null }
  if (correctShortcutMissing) {
    issues += KeyMapIssue.AddShortcut(
      "esc",
      "editor escape",
      IdeActions.ACTION_EDITOR_ESCAPE,
      key("<esc>")
    )
  }
  shortcutsStartingFromEsc.forEach {
    issues += KeyMapIssue.RemoveShortcut("editor escape", IdeActions.ACTION_EDITOR_ESCAPE, it)
  }


  val correctEnterShortcutMissing = keymapShortcutsForEnter
    .filterIsInstance<KeyboardShortcut>()
    .none { it.firstKeyStroke.toString() == "pressed ENTER" && it.secondKeyStroke == null }
  val shortcutsStartingFromEnter = keymapShortcutsForEnter
    .filterIsInstance<KeyboardShortcut>()
    .filter { it.firstKeyStroke.toString() == "pressed ENTER" && it.secondKeyStroke != null }
  if (correctEnterShortcutMissing) {
    issues += KeyMapIssue.AddShortcut(
      "enter",
      "editor enter",
      IdeActions.ACTION_EDITOR_ENTER,
      key("<enter>")
    )
  }
  shortcutsStartingFromEnter.forEach {
    issues += KeyMapIssue.RemoveShortcut("editor enter", IdeActions.ACTION_EDITOR_ENTER, it)
  }

  if (issues.isNotEmpty()) {
    VimPlugin.getNotifications(null).notifyKeymapIssues(issues)
  }
}

internal sealed interface KeyMapIssue {
  data class AddShortcut(
    val key: String,
    val action: String,
    val actionId: String,
    val keyStroke: KeyStroke,
  ) : KeyMapIssue

  data class RemoveShortcut(
    val action: String,
    val actionId: String,
    val shortcut: Shortcut,
  ) : KeyMapIssue
}