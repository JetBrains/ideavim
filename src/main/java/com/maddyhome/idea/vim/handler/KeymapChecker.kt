/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.key
import javax.swing.KeyStroke

/**
 * This checker verifies that the keymap has a correct configuration that is required for IdeaVim plugin
 */
internal class KeymapChecker : StartupActivity {
  override fun runActivity(project: Project) {
    verifyKeymap(project)
  }
}

internal class IdeaVimKeymapChangedListener : KeymapManagerListener {
  override fun activeKeymapChanged(keymap: Keymap?) {
    verifyKeymap(null)
  }

  override fun shortcutChanged(keymap: Keymap, actionId: String) {
    verifyKeymap(null)
  }

  override fun shortcutChanged(keymap: Keymap, actionId: String, fromSettings: Boolean) {
    verifyKeymap(null)
  }
}

/**
 * After migration to the editor action handlers, we have to make sure that the keymap has a correct configuration.
 * For example, that esc key is assigned to esc editor action
 *
 * Usually this is not a problem because this is a standard mapping, but the problem may appear in a misconfiguration
 *   like it was in VIM-3204
 */
private fun verifyKeymap(project: Project?) {
  val keymapManager = KeymapManagerEx.getInstanceEx()
  val keymap = keymapManager.activeKeymap
  val keymapShortcutsForEsc = keymap.getShortcuts(IdeActions.ACTION_EDITOR_ESCAPE)
  val keymapShortcutsForEnter = keymap.getShortcuts(IdeActions.ACTION_EDITOR_ENTER)

  val issues = ArrayList<KeyMapIssue>()
  if ("[pressed ESCAPE]" !in keymapShortcutsForEsc.map { it.toString() }) {
    issues += KeyMapIssue(
      "esc",
      "editor escape",
      IdeActions.ACTION_EDITOR_ESCAPE,
      key("<esc>")
    )
  }

  if ("[pressed ENTER]" !in keymapShortcutsForEnter.map { it.toString() }) {
    issues += KeyMapIssue(
      "enter",
      "editor enter",
      IdeActions.ACTION_EDITOR_ENTER,
      key("<enter>")
    )
  }

  if (issues.isNotEmpty()) {
    VimPlugin.getNotifications(project).notifyKeymapIssues(issues)
  }
}

internal class KeyMapIssue(
  val key: String,
  val action: String,
  val actionId: String,
  val keyStroke: KeyStroke,
)