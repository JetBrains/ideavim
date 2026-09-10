/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.ShortcutDispatcher
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.ui.ex.ExTextField
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

/**
 * Extends `<C-[>` to every component of the IDE, not just the editor.
 */
class EscEverywhere : VimExtension {
  companion object {
    const val PLUGIN_NAME = "EscEverywhere"
  }

  override fun getName() = PLUGIN_NAME

  private var registeredComponent: JComponent? = null

  val focusListener = PropertyChangeListener { evt ->
    val newFocusOwner = evt.newValue
    val oldFocusOwner = evt.oldValue
    val dispatcher = service<Dispatcher>()
    // Unregistration of the shortcut is required to make the plugin disposable.
    if (oldFocusOwner is JComponent) {
      dispatcher.unregisterCustomShortcutSet(oldFocusOwner)
      registeredComponent = null
    }
    // IdeaVim's own components are skipped: there `<C-[>` is an ordinary Vim key that the key handler already
    // treats as `<Esc>`, and that the user may have mapped to something else entirely.
    if (newFocusOwner is JComponent && !newFocusOwner.isVimComponent) {
      // `register` is idempotent - its internal implementation prevents duplicate registrations
      dispatcher.register(newFocusOwner)
      registeredComponent = newFocusOwner
    }
  }

  override fun init() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener)
  }

  @Service
  class Dispatcher : ShortcutDispatcher<(Component) -> Unit>(PLUGIN_NAME, createMappings(), EscListener) {
    init {
      // By default, IntelliJ disables actions while a modal dialog is open, but `<C-[>` has to cancel a dialog too.
      templatePresentation.isEnabledInModalContext = true
    }

    override fun update(e: AnActionEvent) {
      // A second guard for the components handled by IdeaVim itself: the shortcut set is registered on the focus
      // owner, but the action system also consults it for anything focused inside that component.
      val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
      e.presentation.isEnabled = component != null && !component.isVimComponent
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
  }

  override fun dispose() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener)
    registeredComponent?.let { service<Dispatcher>().unregisterCustomShortcutSet(it) }
    registeredComponent = null
    super.dispose()
  }
}

private object EscListener : ShortcutDispatcher.Listener<(Component) -> Unit> {
  override fun onMatch(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>, data: (Component) -> Unit) {
    e.getData(PlatformDataKeys.CONTEXT_COMPONENT)?.let(data)
    keyStrokes.clear()
  }

  override fun onInvalid(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>) {
    // There is a single one-key mapping, so nothing can be half-typed. Stay silent instead of beeping.
    keyStrokes.clear()
  }
}

private fun createMappings(): Map<List<KeyStroke>, (Component) -> Unit> =
  mapOf(injector.parser.parseKeys("<C-[>") to ::pressEsc)

private fun pressEsc(contextComponent: Component) {
  // The popup manager, the keymap and the Swing bindings all work off the real focus owner, so the event has to name
  // it; the action's context component is only a fallback.
  val target = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner ?: contextComponent
  SwingUtilities.invokeLater {
    val queue = IdeEventQueue.getInstance()
    val timestamp = System.currentTimeMillis()
    for (id in intArrayOf(KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED)) {
      queue.dispatchEvent(KeyEvent(target, id, timestamp, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED))
    }
  }
}

private val Component.isVimComponent: Boolean
  get() = this is EditorComponentImpl || this is ExTextField
