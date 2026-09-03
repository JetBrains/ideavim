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
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.ActionEventKeyStrokeExtractor
import com.maddyhome.idea.vim.impl.state.VimStateMachineImpl
import com.maddyhome.idea.vim.key.KeySource
import com.maddyhome.idea.vim.key.MappingInfo
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.ToActionMappingInfo
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.text.JTextComponent

/**
 * Makes user mappings work when no editor is open.
 */
@Service
class OutsideEditorKeyDispatcher : DumbAwareAction() {
  private val keyStrokeExtractor = ActionEventKeyStrokeExtractor()
  private var registeredComponent: JComponent? = null

  init {
    templatePresentation.isEnabledInModalContext = true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  private fun isEnabled(e: AnActionEvent): Boolean {
    if (VimPlugin.isNotEnabled()) return false
    if (e.getData(PlatformDataKeys.EDITOR) != null) return false
    if (e.getData(PlatformDataKeys.SPEED_SEARCH_TEXT) != null) return false
    if (e.getData(PlatformDataKeys.IS_MODAL_CONTEXT) == true) return false
    val keyStroke = keyStrokeExtractor.getKeyStroke(e)?.normalized() ?: return false
    val pendingKeys = KeyHandler.getInstance().keyHandlerState.mappingState.keys.toList()
    if (keyStroke.isEscape) return pendingKeys.isNotEmpty()

    // Only keys that continue (or complete) a user mapping are taken. Anything else is left to the IDE, so unmapped
    // Vim commands like `dd` never reach the hidden fallback window.
    val keys = pendingKeys + keyStroke
    return userMappingKeys().any { it.size >= keys.size && it.subList(0, keys.size) == keys }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val keyStroke = keyStrokeExtractor.getKeyStroke(e)?.normalized() ?: return
    val keyHandler = KeyHandler.getInstance()
    val fallbackWindow = injector.fallbackWindow
    if (keyStroke.isEscape) {
      resetToNormal()
      return
    }
    try {
      forceNormalModeSilently()
      keyHandler.withoutRecording {
        keyHandler.handleKey(
          fallbackWindow, keyStroke, KeySource.TYPED, e.dataContext.vim, keyHandler.keyHandlerState
        )
      }
    } catch (ex: ProcessCanceledException) {
      throw ex
    } catch (ex: Exception) {
      LOG.error(ex)
    } finally {
      // An unfinished sequence must survive: MappingProcessor completes it on 'timeout'. A finished one leaves the
      // hidden editor in whatever mode the mapping produced, and Normal is the only mode that makes sense without an
      // editor. Note that fullReset also clears the selected register and the current error message.
      if (keyHandler.keyHandlerState.mappingState.keys.none()) resetToNormal()
    }
  }

  private fun resetToNormal() {
    KeyHandler.getInstance().fullReset(injector.fallbackWindow)
  }

  private fun forceNormalModeSilently() {
    (injector.vimState as VimStateMachineImpl).mode = Mode.NORMAL()
  }

  private fun register(component: JComponent) {
    // The shortcut set is rebuilt on every registration, so `:map`/`:unmap` executed since the last focus change are
    // picked up. Escape is always included so that a pending sequence can be cancelled.
    val keys = userMappingKeys().flatten().toMutableSet()
    if (keys.isEmpty()) return
    keys += ESCAPE
    registerCustomShortcutSet(CustomShortcutSet(*keys.map { KeyboardShortcut(it, null) }.toTypedArray()), component)
    registeredComponent = component
  }

  private fun unregister() {
    val component = registeredComponent ?: return
    unregisterCustomShortcutSet(component)
    registeredComponent = null
    // Half of a sequence typed outside the editor must not change what the next keys do inside it
    if (KeyHandler.getInstance().keyHandlerState.mappingState.keys.any()) resetToNormal()
  }

  /**
   * The mappings that make sense without an editor: defined by the user and executing nothing but IDE actions. A
   * mapping to Vim keys, e.g. `nmap j gj`, would run on the hidden fallback window, where the command is invisible and
   * most of them fail, while stealing the key from the focused component (e.g. `j` starting speed search in a tree).
   */
  private fun userMappingKeys(): Sequence<List<KeyStroke>> =
    injector.keyGroup.getKeyMapping(MappingMode.NORMAL).getAll(emptyList())
      .filter { it.mappingInfo.owner.isUserDefined && it.mappingInfo.isActionsOnly }.map { it.getPath() }

  private val focusListener = PropertyChangeListener { evt ->
    unregister()
    val newFocusOwner = evt.newValue as? JComponent ?: return@PropertyChangeListener
    if (shouldHandle(newFocusOwner)) register(newFocusOwner)
  }

  fun installFocusListener() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener)
  }

  fun removeFocusListener() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener)
    unregister()
  }

  companion object {
    private val LOG = logger<OutsideEditorKeyDispatcher>()
    private val ESCAPE: KeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    private const val ESCAPE_CHAR = '\u001B'

    @JvmStatic
    fun getInstance(): OutsideEditorKeyDispatcher = service()

    private fun shouldHandle(component: Component): Boolean {
      val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(component))
      return shouldHandle(component, project)
    }

    /**
     * The dispatcher is active only for the scenario of the issue: focus is on a non-text component of a project that
     * has no open editor. Text components (e.g. the Search Everywhere field) must keep receiving typed characters.
     */
    fun shouldHandle(component: Component, project: Project?): Boolean {
      if (component is EditorComponentImpl || component is JTextComponent) return false
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
        if (close == i + 2) return false // empty action name
        i = close + 1
      }
      return true
    }

    // Typed characters arrive with the shift modifier for upper case letters; mappings are stored as plain typed keys
    private fun KeyStroke.normalized(): KeyStroke =
      if (keyChar != KeyEvent.CHAR_UNDEFINED && keyChar != ESCAPE_CHAR) KeyStroke.getKeyStroke(keyChar) else this

    private val KeyStroke.isEscape: Boolean
      get() = keyCode == KeyEvent.VK_ESCAPE || keyChar == ESCAPE_CHAR
  }
}
