/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.ui.treeStructure.Tree
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.group.KeyGroup
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.RequiredShortcut
import java.awt.KeyboardFocusManager
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * This plugin extends NERDTree support to components other than the Project Tool Window.
 *
 * TODO:
 * It should be considered a "sub-plugin" of NERDTree and cannot be enabled independently,
 * i.e., should not function after the NERDTree plugin is turned off.
 */
internal class NerdTreeEverywhere : VimExtension {
  companion object {
    const val PLUGIN_NAME = "NERDTreeEverywhere" // This is a temporary name
  }

  override fun getName() = PLUGIN_NAME

  override fun init() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner") {
      val newFocusOwner = it.newValue
      val oldFocusOwner = it.oldValue
      val dispatcher = service<Dispatcher>()
      if (newFocusOwner is Tree) {
        val shortcuts = mappings.keyStrokes.map { RequiredShortcut(it, MappingOwner.Plugin.get(PLUGIN_NAME)) }
        // It's okay to have `register` called multiple times, as its internal implementation prevents duplicate registrations
        dispatcher.registerCustomShortcutSet(KeyGroup.toShortcutSet(shortcuts), newFocusOwner)
      }
      // Note that we do not have to unregister the shortcut in fact
      if (oldFocusOwner is Tree) {
        dispatcher.unregisterCustomShortcutSet(oldFocusOwner)
      }
    }
  }

  @Service
  class Dispatcher : AbstractDispatcher(mappings) {
    init {
      templatePresentation.isEnabledInModalContext = true

      mappings.registerNavigationMappings()
      mappings.register("NERDTreeMapActivateNode", "o", Mappings.Action { _, tree ->
        // TODO a more reliable way of invocation (such as double-clicking?)
        val listener = tree.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
        listener.actionPerformed(ActionEvent(tree, ActionEvent.ACTION_PERFORMED, null))
      })
    }
  }
}

private val mappings = Mappings(NerdTreeEverywhere.PLUGIN_NAME)
