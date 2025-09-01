/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopup
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.key.KeyStrokeTrie
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

internal class HintDispatcher(
  targets: List<HintTarget>,
  private val component: JComponent,
  private val popup: JBPopup,
) : DumbAwareAction() {
  private val trie = KeyStrokeTrie<HintTarget>("hints")

  init {
    val keys: MutableList<KeyStroke> = mutableListOf()
    for (target in targets) {
      injector.parser.parseKeys(target.hint.lowercase()).let {
        keys.addAll(it)
        trie.add(it, target)
      }
    }
    val shortcuts = keys.map { KeyboardShortcut(it, null) }
    registerCustomShortcutSet(CustomShortcutSet(*shortcuts.toTypedArray()), component, popup)
  }

  private val keyStrokes: MutableList<KeyStroke> = mutableListOf()

  override fun actionPerformed(event: AnActionEvent) {
    val keyStroke = getKeyStroke(event) ?: return
    keyStrokes.add(keyStroke)

    trie.getData(keyStrokes)?.let {
      onChosen(it)
      return
    }
    if (!trie.isPrefix(keyStrokes)) {
      onChosen(null)
    }
  }

  fun onChosen(target: HintTarget?) {
    unregisterCustomShortcutSet(component)
    if (target != null) {
      target.component.accessibleContext?.accessibleAction?.doAccessibleAction(0)
      popup.closeOk(null)
    } else {
      injector.messages.indicateError()
      popup.cancel()
    }
  }
}

private fun getKeyStroke(event: AnActionEvent): KeyStroke? =
  (event.inputEvent as? KeyEvent)?.let { inputEvent -> KeyStroke.getKeyStrokeForEvent(inputEvent) }
