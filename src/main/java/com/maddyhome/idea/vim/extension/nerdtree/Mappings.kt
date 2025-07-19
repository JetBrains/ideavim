/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.treeStructure.Tree
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.key.KeyStrokeTrie
import com.maddyhome.idea.vim.key.add
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import javax.swing.KeyStroke

/**
 * Maps key sequences to NERDTree actions using KeyStrokeTrie for efficient lookup.
 *
 * @constructor Creates an empty trie for key mappings
 * @param name The name of the KeyStrokeTrie instance (for debug purposes)
 */
internal class Mappings(name: String) {
  private val trie = KeyStrokeTrie<Action>(name)
  val getAction = trie::getData

  private val _keyStrokes = mutableSetOf<KeyStroke>()
  val keyStrokes: Set<KeyStroke> get() = _keyStrokes

  fun register(variable: String, defaultMapping: String, action: Action) {
    val variableValue = VimPlugin.getVariableService().getGlobalVariableValue(variable)
    val mapping = if (variableValue is VimString) {
      variableValue.value
    } else {
      defaultMapping
    }
    register(mapping, action)
  }

  fun register(mapping: String, action: Action) {
    trie.add(mapping, action)
    _keyStrokes.addAll(injector.parser.parseKeys(mapping))
  }

  /**
   * Defines the actual behavior of actions in NERDTree
   */
  class Action(val action: (AnActionEvent, Tree) -> Unit) {
    companion object {
      private fun executeAction(event: AnActionEvent, id: String) =
        NerdTree.Util.callAction(null, id, event.dataContext.vim)

      /**
       * Creates an [Action] that executes an IntelliJ action identified by its ID.
       *
       * @param id A string representing the ID of the action to execute.
       * @return An [Action] that runs the specified action when triggered.
       */
      fun ij(id: String): Action = Action { event, _ -> executeAction(event, id) }
    }
  }
}
