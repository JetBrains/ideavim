/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.action

import com.google.common.collect.ImmutableSet
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.EmptyAction
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
import com.intellij.ui.KeyStrokeAdapter
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.helper.isPrimaryEditor
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.listener.IdeaSpecifics.AppCodeTemplates.appCodeTemplateCaptured
import com.maddyhome.idea.vim.listener.IdeaSpecifics.aceJumpActive
import com.maddyhome.idea.vim.option.OptionsManager
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Handles Vim keys that are treated as action shortcuts by the IDE.
 *
 *
 * These keys are not passed to [com.maddyhome.idea.vim.VimTypedActionHandler] and should be handled by actions.
 */
class VimShortcutKeyAction : AnAction(), DumbAware/*, LightEditCompatible*/ {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = getEditor(e)
    val keyStroke = getKeyStroke(e)
    if (editor != null && keyStroke != null) {
      val owner = VimPlugin.getKey().savedShortcutConflicts[keyStroke]
      if (owner == ShortcutOwner.UNDEFINED) {
        VimPlugin.getNotifications(editor.project).notifyAboutShortcutConflict(keyStroke)
      }
      // Should we use HelperKt.getTopLevelEditor(editor) here, as we did in former EditorKeyHandler?
      try {
        KeyHandler.getInstance().handleKey(editor, keyStroke, EditorDataContext(editor, e.dataContext))
      } catch (ignored: ProcessCanceledException) {
        // Control-flow exceptions (like ProcessCanceledException) should never be logged
        // See {@link com.intellij.openapi.diagnostic.Logger.checkException}
      } catch (throwable: Throwable) {
        ourLogger.error(throwable)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  private fun isEnabled(e: AnActionEvent): Boolean {
    if (!VimPlugin.isEnabled()) return false
    val editor = getEditor(e)
    val keyStroke = getKeyStroke(e)
    if (editor != null && keyStroke != null) {
      if (editor.isIdeaVimDisabledHere) return false
      // Workaround for smart step into
      @Suppress("DEPRECATION", "LocalVariableName", "VariableNaming")
      val SMART_STEP_INPLACE_DATA = Key.findKeyByName("SMART_STEP_INPLACE_DATA")
      if (SMART_STEP_INPLACE_DATA != null && editor.getUserData(SMART_STEP_INPLACE_DATA) != null) return false

      if (aceJumpActive()) return false

      val keyCode = keyStroke.keyCode

      if (LookupManager.getActiveLookup(editor) != null && !LookupKeys.isEnabledForLookup(keyStroke)) return false

      if (keyCode == KeyEvent.VK_ESCAPE) return isEnabledForEscape(editor)

      if (keyCode == KeyEvent.VK_TAB && editor.isTemplateActive()) return false

      if ((keyCode == KeyEvent.VK_TAB || keyCode == KeyEvent.VK_ENTER) && editor.appCodeTemplateCaptured()) return false

      if (editor.inInsertMode) { // XXX: <Tab> won't be recorded in macros
        if (keyCode == KeyEvent.VK_TAB) {
          VimPlugin.getChange().tabAction = true
          return false
        }
        // Debug watch, Python console, etc.
        if (keyStroke in NON_FILE_EDITOR_KEYS && !EditorHelper.isFileEditor(editor)) return false
      }

      if (keyStroke in VIM_ONLY_EDITOR_KEYS) return true

      val savedShortcutConflicts = VimPlugin.getKey().savedShortcutConflicts
      return when (savedShortcutConflicts[keyStroke]) {
        ShortcutOwner.VIM -> true
        ShortcutOwner.IDE -> !isShortcutConflict(keyStroke)
        else -> {
          if (isShortcutConflict(keyStroke)) {
            savedShortcutConflicts[keyStroke] = ShortcutOwner.UNDEFINED
          }
          true
        }
      }
    }
    return false
  }

  private fun isEnabledForEscape(editor: Editor): Boolean {
    return editor.isPrimaryEditor()
      || EditorHelper.isFileEditor(editor) && !editor.inNormalMode
      || OptionsManager.ideavimsupport.contains("dialog") && !editor.inNormalMode
  }

  private fun isShortcutConflict(keyStroke: KeyStroke): Boolean {
    return VimPlugin.getKey().getKeymapConflicts(keyStroke).isNotEmpty()
  }

  /**
   * getDefaultKeyStroke is needed for NEO layout keyboard VIM-987
   * but we should cache the value because on the second call (isEnabled -> actionPerformed)
   * the event is already consumed
   */
  private var keyStrokeCache: Pair<KeyEvent?, KeyStroke?> = null to null

  private fun getKeyStroke(e: AnActionEvent): KeyStroke? {
    val inputEvent = e.inputEvent
    if (inputEvent is KeyEvent) {
      val defaultKeyStroke = KeyStrokeAdapter.getDefaultKeyStroke(inputEvent)
      val strokeCache = keyStrokeCache
      if (defaultKeyStroke != null) {
        keyStrokeCache = inputEvent to defaultKeyStroke
        return defaultKeyStroke
      } else if (strokeCache.first === inputEvent) {
        keyStrokeCache = null to null
        return strokeCache.second
      }
      return KeyStroke.getKeyStrokeForEvent(inputEvent)
    }
    return null
  }

  private fun getEditor(e: AnActionEvent): Editor? = e.getData(PlatformDataKeys.EDITOR)

  /**
   * Every time the key pressed with an active lookup, there is a decision:
   *   should this key be processed by IdeaVim, or by IDE. For example, dot and enter should be processed by IDE, but
   *   <C-W> by IdeaVim.
   *
   * The list of keys that should be processed by IDE is stored in [OptionsManager.lookupKeys]. So, we should search
   *   if the pressed key is presented in this list. The caches are used to speedup the process.
   */
  private object LookupKeys {
    private var parsedLookupKeys: Set<KeyStroke> = parseLookupKeys()

    init {
      OptionsManager.lookupKeys.addOptionChangeListener { _, _ ->
        parsedLookupKeys = parseLookupKeys()
      }
    }

    fun isEnabledForLookup(keyStroke: KeyStroke): Boolean = keyStroke !in parsedLookupKeys

    private fun parseLookupKeys() = OptionsManager.lookupKeys.values()
      .map { StringHelper.parseKeys(it) }.filter { it.isNotEmpty() }.map { it.first() }.toSet()
  }

  companion object {
    @JvmField
    val VIM_ONLY_EDITOR_KEYS: Set<KeyStroke> =
      ImmutableSet.builder<KeyStroke>().addAll(getKeyStrokes(KeyEvent.VK_ENTER, 0))
        .addAll(getKeyStrokes(KeyEvent.VK_ESCAPE, 0))
        .addAll(getKeyStrokes(KeyEvent.VK_TAB, 0))
        .addAll(getKeyStrokes(KeyEvent.VK_BACK_SPACE, 0, InputEvent.CTRL_DOWN_MASK))
        .addAll(getKeyStrokes(KeyEvent.VK_INSERT, 0))
        .addAll(getKeyStrokes(KeyEvent.VK_DELETE, 0, InputEvent.CTRL_DOWN_MASK))
        .addAll(getKeyStrokes(KeyEvent.VK_UP, 0, InputEvent.CTRL_DOWN_MASK, InputEvent.SHIFT_DOWN_MASK))
        .addAll(getKeyStrokes(KeyEvent.VK_DOWN, 0, InputEvent.CTRL_DOWN_MASK, InputEvent.SHIFT_DOWN_MASK))
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_LEFT,
            0,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
          )
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_RIGHT,
            0,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
          )
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_HOME,
            0,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
          )
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_END,
            0,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
          )
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_PAGE_UP,
            0,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
          )
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_PAGE_DOWN,
            0,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
          )
        ).build()

    private const val ACTION_ID = "VimShortcutKeyAction"

    private val NON_FILE_EDITOR_KEYS: Set<KeyStroke> = ImmutableSet.builder<KeyStroke>()
      .addAll(getKeyStrokes(KeyEvent.VK_ENTER, 0))
      .addAll(getKeyStrokes(KeyEvent.VK_ESCAPE, 0))
      .addAll(getKeyStrokes(KeyEvent.VK_TAB, 0))
      .addAll(getKeyStrokes(KeyEvent.VK_UP, 0))
      .addAll(getKeyStrokes(KeyEvent.VK_DOWN, 0))
      .build()

    private val ourLogger = Logger.getInstance(VimShortcutKeyAction::class.java.name)

    @JvmStatic
    val instance: AnAction by lazy {
      EmptyAction.wrap(ActionManager.getInstance().getAction(ACTION_ID))
    }

    private fun getKeyStrokes(keyCode: Int, vararg modifiers: Int) =
      modifiers.map { KeyStroke.getKeyStroke(keyCode, it) }
  }
}
