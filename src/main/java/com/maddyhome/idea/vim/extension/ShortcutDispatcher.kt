/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.project.DumbAwareAction
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.ActionEventKeyStrokeExtractor
import com.maddyhome.idea.vim.key.KeyStrokeTrie
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

open class ShortcutDispatcher<T>(
  name: String,
  data: Map<List<KeyStroke>, T>,
  private val listener: Listener<T>,
) : DumbAwareAction() {
  interface Listener<T> {
    fun onMatch(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>, data: T) {}
    fun onInvalid(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>) {}
    fun onKey(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>, entries: Sequence<KeyStrokeTrie.TrieNode<T>>) {}
  }

  constructor(
    name: String,
    data: Map<String, T>,
    onMatch: (T) -> Unit,
    onInvalid: () -> Unit,
    onKey: (Sequence<KeyStrokeTrie.TrieNode<T>>) -> Unit,
  ) : this(name, data.mapKeys { injector.parser.parseKeys(it.key) }.toMap(), object : Listener<T> {
    override fun onMatch(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>, data: T) = onMatch(data)
    override fun onInvalid(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>) = onInvalid()
    override fun onKey(
      e: AnActionEvent,
      keyStrokes: MutableList<KeyStroke>,
      entries: Sequence<KeyStrokeTrie.TrieNode<T>>,
    ) = onKey(entries)
  })

  protected val trie = KeyStrokeTrie<T>(name)
  private val shortcutSet: ShortcutSet

  init {
    val keys: MutableList<KeyStroke> = mutableListOf()
    for ((k, v) in data) {
      keys.addAll(k)
      trie.add(k, v)
    }
    val shortcuts = keys.map { KeyboardShortcut(it, null) }
    shortcutSet = CustomShortcutSet(*shortcuts.toTypedArray())
  }

  protected val keyStrokes: MutableList<KeyStroke> = mutableListOf()

  final override fun actionPerformed(e: AnActionEvent) {
    var keyStroke = getKeyStroke(e) ?: return
    // Omit the modifier (shift) from keyStroke
    keyStroke.keyChar.let {
      if (it != KeyEvent.CHAR_UNDEFINED) {
        keyStroke = KeyStroke.getKeyStroke(it)
      }
    }
    keyStrokes.add(keyStroke)
    listener.onKey(e, keyStrokes, trie.getEntries(keyStrokes))

    trie.getData(keyStrokes)?.let {
      listener.onMatch(e, keyStrokes, it)
      return
    }
    if (!trie.isPrefix(keyStrokes)) {
      listener.onInvalid(e, keyStrokes)
    }
  }

  fun register(component: JComponent?) = registerCustomShortcutSet(shortcutSet, component)
  fun register(component: JComponent?, parentDisposable: Disposable?) =
    registerCustomShortcutSet(shortcutSet, component, parentDisposable)

  private val keyStrokeExtractor = ActionEventKeyStrokeExtractor()

  private fun getKeyStroke(e: AnActionEvent): KeyStroke? = keyStrokeExtractor.getKeyStroke(e)
}
