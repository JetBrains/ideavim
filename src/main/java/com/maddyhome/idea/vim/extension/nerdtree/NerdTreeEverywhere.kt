/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.ui.treeStructure.Tree
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.newapi.vim
import java.awt.KeyboardFocusManager
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import javax.swing.KeyStroke

/**
 * This plugin extends NERDTree support to components other than the Project Tool Window.
 *
 * TODO:
 * It should be considered a "sub-plugin" of NERDTree and cannot be enabled independently,
 * i.e., should not function after the NERDTree plugin is turned off.
 */
class NerdTreeEverywhere : VimExtension {
  companion object {
    const val PLUGIN_NAME = "NERDTreeEverywhere" // This is a temporary name
  }

  override fun getName() = PLUGIN_NAME

  val focusListener = PropertyChangeListener { evt ->
    val newFocusOwner = evt.newValue
    val oldFocusOwner = evt.oldValue
    val dispatcher = service<Dispatcher>()
    if (newFocusOwner is Tree) {
      // It's okay to have `register` called multiple times, as its internal implementation prevents duplicate registrations
      dispatcher.register(newFocusOwner)
    }
    // Unregistration of the shortcut is required to make the plugin disposable
    if (oldFocusOwner is Tree) {
      dispatcher.unregisterCustomShortcutSet(oldFocusOwner)
    }
  }

  override fun init() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener)
  }

  @Service
  class Dispatcher : AbstractDispatcher(PLUGIN_NAME, navigationMappings.toMutableMap().apply {
    // NerdTreeEverywhere must handle all file-opening mappings that NerdTree uses.
    // Multi-key sequences like 'gs'/'gi' add their individual keys ('s', 'i') to the
    // CustomShortcutSet, which would capture those keys and swallow them as invalid
    // if we don't also register standalone 's'/'i'/'t'/'T' handlers here.
    register("NERDTreeMapActivateNode", "o", NerdTreeAction { event, tree ->
      openFileOrSimulateEnter(event, tree)
    })
    register("NERDTreeMapPreview", "go", NerdTreeAction { event, _ ->
      openFileOrSimulateEnter(event, focusEditor = false)
    })
    register("NERDTreeMapOpenInTab", "t", NerdTreeAction { event, tree ->
      openFileOrSimulateEnter(event, tree)
    })
    register("NERDTreeMapOpenInTabSilent", "T", NerdTreeAction { event, _ ->
      openFileOrSimulateEnter(event, focusEditor = false)
    })
    register("NERDTreeMapOpenVSplit", "s", NerdTreeAction { event, _ ->
      val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@NerdTreeAction
      if (file.isDirectory) return@NerdTreeAction
      injector.window.splitWindowVertical(event.dataContext.vim, file.path)
    })
    register("NERDTreeMapOpenSplit", "i", NerdTreeAction { event, _ ->
      val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@NerdTreeAction
      if (file.isDirectory) return@NerdTreeAction
      injector.window.splitWindowHorizontal(event.dataContext.vim, file.path)
    })
    register("NERDTreeMapPreviewVSplit", "gs", NerdTreeAction { event, tree ->
      val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@NerdTreeAction
      if (file.isDirectory) return@NerdTreeAction
      injector.window.splitWindowVertical(event.dataContext.vim, file.path)
      tree.requestFocus()
    })
    register("NERDTreeMapPreviewSplit", "gi", NerdTreeAction { event, tree ->
      val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@NerdTreeAction
      if (file.isDirectory) return@NerdTreeAction
      injector.window.splitWindowHorizontal(event.dataContext.vim, file.path)
      tree.requestFocus()
    })
  }) {
    init {
      templatePresentation.isEnabledInModalContext = true
    }
  }

  override fun dispose() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener)
    super.dispose()
  }
}

/**
 * Opens a file via [injector.file] (which routes through RPC in split mode),
 * or simulates Enter for directories / non-file tree nodes.
 */
private fun openFileOrSimulateEnter(event: AnActionEvent, tree: Tree, focusEditor: Boolean = true) {
  val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
  if (file != null && !file.isDirectory) {
    injector.file.openFile(file.path, event.dataContext.vim, focusEditor)
  } else {
    val listener = tree.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    listener?.actionPerformed(ActionEvent(tree, ActionEvent.ACTION_PERFORMED, null))
  }
}

/**
 * Opens a file without focus (for preview actions). Does nothing for directories.
 */
private fun openFileOrSimulateEnter(event: AnActionEvent, focusEditor: Boolean) {
  val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
  if (file != null && !file.isDirectory) {
    injector.file.openFile(file.path, event.dataContext.vim, focusEditor)
  }
}
