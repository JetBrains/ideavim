/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.helper.ActionEventKeyStrokeExtractor
import com.maddyhome.idea.vim.impl.state.VimStateMachineImpl
import com.maddyhome.idea.vim.key.MappingInfo
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.ToActionMappingInfo
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.annotations.VisibleForTesting
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.text.JTextComponent

/**
 * Makes user mappings to IDE actions work when no editor is open.
 */
@Service
class OutsideEditorKeyDispatcher : DumbAwareAction() {
  private val keyStrokeExtractor = ActionEventKeyStrokeExtractor()
  private var registeredComponent: JComponent? = null

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isActive() && isOutsideEditor(e) && claimsKey(e)
  }

  private fun isActive(): Boolean = VimPlugin.isEnabled() && injector.globalIjOptions().ideaoutsideeditor

  private fun isOutsideEditor(e: AnActionEvent): Boolean =
    e.getData(PlatformDataKeys.EDITOR) == null && e.getData(PlatformDataKeys.SPEED_SEARCH_TEXT) == null

  /**
   * Only keys that continue a user mapping are claimed. Anything else is left to the IDE, so unmapped Vim commands
   * like `dd` never reach the hidden fallback window.
   */
  private fun claimsKey(e: AnActionEvent): Boolean {
    val keyStroke = keyStrokeExtractor.getKeyStroke(e)?.normalized() ?: return false
    val typed = pendingKeys() + keyStroke
    return userMappingKeys().any { it.startsWith(typed) }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val keyStroke = keyStrokeExtractor.getKeyStroke(e)?.normalized() ?: return
    val keyHandler = KeyHandler.getInstance()
    try {
      forceNormalModeSilently()
      keyHandler.withoutRecording {
        keyHandler.handleUserKey(injector.fallbackWindow, keyStroke, e.dataContext.vim)
      }
    } catch (ex: ProcessCanceledException) {
      throw ex
    } catch (ex: Exception) {
      LOG.error(ex)
    } finally {
      // An unfinished sequence is kept for MappingProcessor to complete on 'timeout'. Without an editor, Normal is the
      // only mode that makes sense once the mapping is done.
      if (!hasPendingSequence()) resetToNormal()
    }
  }

  private fun pendingKeys(): List<KeyStroke> = KeyHandler.getInstance().keyHandlerState.mappingState.keys.toList()

  private fun hasPendingSequence(): Boolean = KeyHandler.getInstance().keyHandlerState.mappingState.keys.any()

  /** Also clears the selected register and the current error message */
  private fun resetToNormal() {
    KeyHandler.getInstance().fullReset(injector.fallbackWindow)
  }

  private fun forceNormalModeSilently() {
    (injector.vimState as VimStateMachineImpl).mode = Mode.NORMAL()
  }

  /** The shortcut set is rebuilt on every registration to pick up `:map`/`:unmap` */
  private fun register(component: JComponent) {
    if (!injector.globalIjOptions().ideaoutsideeditor) return
    val keys = userMappingKeys().flatten().toSet()
    if (keys.isEmpty()) return
    registerCustomShortcutSet(CustomShortcutSet(*keys.map { KeyboardShortcut(it, null) }.toTypedArray()), component)
    registeredComponent = component
  }

  private fun unregister() {
    val component = registeredComponent ?: return
    unregisterCustomShortcutSet(component)
    registeredComponent = null
    // Half of a sequence typed outside the editor must not change what the next keys do inside it
    if (hasPendingSequence()) resetToNormal()
  }

  /**
   * User mappings that run nothing but IDE actions. A mapping to Vim keys, e.g. `nmap j gj`, would run invisibly on the
   * fallback window and steal the key from the focused component (e.g. `j` starting speed search in a tree).
   */
  private fun userMappingKeys(): Sequence<List<KeyStroke>> =
    injector.keyGroup.getKeyMapping(MappingMode.NORMAL).getAll(emptyList())
      .filter { it.mappingInfo.owner.isUserDefined && it.mappingInfo.isActionsOnly }.map { it.getPath() }

  private fun onFocusOwnerChanged(newFocusOwner: Component?) {
    unregister()
    if (newFocusOwner is JComponent && shouldHandle(newFocusOwner)) register(newFocusOwner)
  }

  @VisibleForTesting
  internal val focusListener = PropertyChangeListener { evt -> onFocusOwnerChanged(evt.newValue as? Component) }

  private val optionListener = GlobalOptionChangeListener {
    onFocusOwnerChanged(KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner)
  }

  fun installListeners() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener)
    injector.optionGroup.addGlobalOptionChangeListener(IjOptions.ideaoutsideeditor, optionListener)
  }

  fun removeListeners() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener)
    injector.optionGroup.removeGlobalOptionChangeListener(IjOptions.ideaoutsideeditor, optionListener)
    unregister()
  }

  companion object {
    private val LOG = logger<OutsideEditorKeyDispatcher>()
    private const val ESCAPE_CHAR = '\u001B'

    @JvmStatic
    fun getInstance(): OutsideEditorKeyDispatcher = service()

    private fun shouldHandle(component: Component): Boolean {
      val dataContext = DataManager.getInstance().getDataContext(component)
      val project = CommonDataKeys.PROJECT.getData(dataContext)
      return shouldHandle(component, project, PlatformDataKeys.IS_MODAL_CONTEXT.getData(dataContext) == true)
    }

    /**
     * Whether to register the shortcuts on the focused [component]: a non-text component of a project with no open
     * editor. Text components (e.g. the Search Everywhere field) keep their typed characters.
     *
     * Modal dialogs and popups are skipped, because a registered shortcut hides the local actions of the parent
     * components for the same key, e.g. Escape closing Search Everywhere, even while [update] disables it.
     */
    fun shouldHandle(component: Component, project: Project?, isModalContext: Boolean = false): Boolean {
      if (component is EditorComponentImpl || component is JTextComponent) return false
      if (isModalContext || component.isInPopup) return false
      if (project == null || project.isDisposed) return false
      return FileEditorManager.getInstance(project).selectedTextEditor == null
    }

    private val MappingOwner.isUserDefined: Boolean
      get() = this === MappingOwner.IdeaVim.InitScript || this === MappingOwner.IdeaVim.Other

    /** True when the right-hand side is one or more `<Action>(...)` sequences and nothing else */
    private val MappingInfo.isActionsOnly: Boolean
      get() = when (this) {
        is ToActionMappingInfo -> true
        is ToKeysMappingInfo -> toKeys.isNotEmpty() && toKeys.isActionSequences()
        else -> false
      }

    private fun List<KeyStroke>.isActionSequences(): Boolean {
      val actionKey = injector.parser.actionKeyStroke
      var i = 0
      while (i < size) {
        if (this[i].keyCode != actionKey.keyCode || getOrNull(i + 1)?.keyChar != '(') return false
        val close = (i + 2 until size).firstOrNull { this[it].keyChar == ')' } ?: return false
        val isEmptyActionName = close == i + 2
        if (isEmptyActionName) return false
        i = close + 1
      }
      return true
    }

    private fun List<KeyStroke>.startsWith(prefix: List<KeyStroke>): Boolean =
      size >= prefix.size && subList(0, prefix.size) == prefix

    // Upper case letters arrive with the shift modifier, while mappings store plain typed keys
    private fun KeyStroke.normalized(): KeyStroke =
      if (keyChar != KeyEvent.CHAR_UNDEFINED && keyChar != ESCAPE_CHAR) KeyStroke.getKeyStroke(keyChar) else this

    // PopupUtil.getPopupContainerFor recognises only JWindow popups and misses the ones shown in a dialog window
    private val Component.isInPopup: Boolean
      get() = generateSequence(this) { it.parent }.any { (it as? JComponent)?.getClientProperty(JBPopup.KEY) != null }
  }
}
