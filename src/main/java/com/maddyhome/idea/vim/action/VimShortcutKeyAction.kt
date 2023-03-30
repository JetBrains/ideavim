/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action

import com.google.common.collect.ImmutableSet
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionWrapper
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
import com.intellij.ui.KeyStrokeAdapter
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.handler.enableOctopus
import com.maddyhome.idea.vim.handler.isOctopusEnabled
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.HandlerInjector
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.helper.isPrimaryEditor
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.listener.AceJumpService
import com.maddyhome.idea.vim.listener.AppCodeTemplates.appCodeTemplateCaptured
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Handles Vim keys that are treated as action shortcuts by the IDE.
 *
 *
 * These keys are not passed to [com.maddyhome.idea.vim.VimTypedActionHandler] and should be handled by actions.
 */
internal class VimShortcutKeyAction : AnAction(), DumbAware/*, LightEditCompatible*/ {
  private val traceTime = injector.globalOptions().ideatracetime

  override fun actionPerformed(e: AnActionEvent) {
    LOG.trace("Executing shortcut key action")
    val editor = getEditor(e)
    val keyStroke = getKeyStroke(e)
    if (editor != null && keyStroke != null) {
      val owner = VimPlugin.getKey().savedShortcutConflicts[keyStroke]
      if ((owner as? ShortcutOwnerInfo.AllModes)?.owner == ShortcutOwner.UNDEFINED) {
        VimPlugin.getNotifications(editor.project).notifyAboutShortcutConflict(keyStroke)
      }
      // Should we use HelperKt.getTopLevelEditor(editor) here, as we did in former EditorKeyHandler?
      try {
        val start = if (traceTime) System.currentTimeMillis() else null
        KeyHandler.getInstance().handleKey(
          editor.vim,
          keyStroke,
          injector.executionContextManager.onEditor(editor.vim, e.dataContext.vim),
        )
        if (start != null) {
          val duration = System.currentTimeMillis() - start
          LOG.info("VimShortcut update '$keyStroke': $duration ms")
        }
      } catch (ignored: ProcessCanceledException) {
        // Control-flow exceptions (like ProcessCanceledException) should never be logged
        // See {@link com.intellij.openapi.diagnostic.Logger.checkException}
      } catch (throwable: Throwable) {
        LOG.error(throwable)
      }
    }
  }

  // There is a chance that we can use BGT, but we call for isCell inside the update.
  // Not sure if can can use BGT with this call. Let's use EDT for now.
  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    val start = if (traceTime) System.currentTimeMillis() else null
    val actionEnableStatus = isEnabled(e)
    e.presentation.isEnabled = actionEnableStatus.isEnabled
    actionEnableStatus.printLog()
    if (start != null) {
      val keyStroke = getKeyStroke(e)
      val duration = System.currentTimeMillis() - start
      LOG.info("VimShortcut update '$keyStroke': $duration ms")
    }
  }

  private fun isEnabled(e: AnActionEvent): ActionEnableStatus {
    if (!VimPlugin.isEnabled()) return ActionEnableStatus.no("IdeaVim is disabled", LogLevel.DEBUG)
    val editor = getEditor(e)
    val keyStroke = getKeyStroke(e)
    if (editor != null && keyStroke != null) {
      if (enableOctopus) {
        if (isOctopusEnabled(keyStroke, editor)) {
          return ActionEnableStatus.no("Octopus handler is enabled", LogLevel.DEBUG)
        }
      }
      if (editor.isIdeaVimDisabledHere) {
        return ActionEnableStatus.no("IdeaVim is disabled in this place", LogLevel.INFO)
      }
      // Workaround for smart step into
      @Suppress("DEPRECATION", "LocalVariableName", "VariableNaming")
      val SMART_STEP_INPLACE_DATA = Key.findKeyByName("SMART_STEP_INPLACE_DATA")
      if (SMART_STEP_INPLACE_DATA != null && editor.getUserData(SMART_STEP_INPLACE_DATA) != null) {
        LOG.trace("Do not execute shortcut because of smart step")
        return ActionEnableStatus.no("Smart step into is active", LogLevel.INFO)
      }

      val keyCode = keyStroke.keyCode

      if (HandlerInjector.notebookCommandMode(editor)) {
        LOG.debug("Python Notebook command mode")
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_KP_RIGHT || keyCode == KeyEvent.VK_ENTER) {
          invokeLater { editor.updateCaretsVisualAttributes() }
        }
        return ActionEnableStatus.no("Python notebook is in command mode", LogLevel.INFO)
      }

      if (AceJumpService.getInstance()?.isActive(editor) == true) {
        return ActionEnableStatus.no("AceJump is active", LogLevel.INFO)
      }

      if (LookupManager.getActiveLookup(editor) != null && !LookupKeys.isEnabledForLookup(keyStroke)) {
        return ActionEnableStatus.no("Lookup keys are active", LogLevel.INFO)
      }

      if (keyCode == KeyEvent.VK_ESCAPE) {
        return if (isEnabledForEscape(editor)) {
          ActionEnableStatus.yes("Is enabled for Esc", LogLevel.INFO)
        } else {
          ActionEnableStatus.no("Is disabled for Esc", LogLevel.INFO)
        }
      }

      if (keyCode == KeyEvent.VK_TAB && editor.isTemplateActive()) {
        return ActionEnableStatus.no("The key is tab and the template is active", LogLevel.INFO)
      }

      if ((keyCode == KeyEvent.VK_TAB || keyCode == KeyEvent.VK_ENTER) && editor.appCodeTemplateCaptured()) {
        return ActionEnableStatus.no("App code template is active", LogLevel.INFO)
      }

      if (editor.inInsertMode) {
        if (keyCode == KeyEvent.VK_TAB) {
          // TODO: This stops VimEditorTab seeing <Tab> in insert mode and correctly scrolling the view
          // There are multiple actions registered for VK_TAB. The important items, in order, are this, the Live
          // Templates action and TabAction. Returning false in insert mode means that the Live Template action gets to
          // execute, and this allows Emmet to work (VIM-674). But it also means that the VimEditorTab handle is never
          // called, so we can't scroll the caret into view correctly.
          // If we do return true, VimEditorTab handles the Vim side of things and then invokes
          // IdeActions.ACTION_EDITOR_TAB, which inserts the tab. It also bypasses the Live Template action, and Emmet
          // no longer works.
          // This flag is used when recording text entry/keystrokes for repeated insertion. Because we return false and
          // don't execute the VimEditorTab handler, we don't record tab as an action. Instead, we see an incoming text
          // change of multiple whitespace characters, which is normally ignored because it's auto-indent content from
          // hitting <Enter>. When this flag is set, we record the whitespace as the output of the <Tab>
          VimPlugin.getChange().tabAction = true
          return ActionEnableStatus.no("Tab action in insert mode", LogLevel.INFO)
        }
        // Debug watch, Python console, etc.
        if (keyStroke in NON_FILE_EDITOR_KEYS && !EditorHelper.isFileEditor(editor)) {
          return ActionEnableStatus.no("Non file editor keys", LogLevel.INFO)
        }
      }

      if (keyStroke in VIM_ONLY_EDITOR_KEYS) {
        return ActionEnableStatus.yes("Vim only editor keys", LogLevel.INFO)
      }

      val savedShortcutConflicts = VimPlugin.getKey().savedShortcutConflicts
      val info = savedShortcutConflicts[keyStroke]
      return when (info?.forEditor(editor.vim)) {
        ShortcutOwner.VIM -> {
          return ActionEnableStatus.yes("Owner is vim", LogLevel.DEBUG)
        }
        ShortcutOwner.IDE -> {
          if (!isShortcutConflict(keyStroke)) {
            ActionEnableStatus.yes("Owner is IDE, but no actionve shortcut conflict", LogLevel.DEBUG)
          } else {
            ActionEnableStatus.no("Owner is IDE", LogLevel.DEBUG)
          }
        }
        else -> {
          if (isShortcutConflict(keyStroke)) {
            savedShortcutConflicts[keyStroke] = ShortcutOwnerInfo.allUndefined
          }
          ActionEnableStatus.yes("Enable vim for shortcut without owner", LogLevel.DEBUG)
        }
      }
    }
    return ActionEnableStatus.no("End of the selection", LogLevel.DEBUG)
  }

  private fun isEnabledForEscape(editor: Editor): Boolean {
    val ideaVimSupportDialog = injector.options(editor.vim).hasValue(IjOptions.ideavimsupport, IjOptionConstants.ideavimsupport_dialog)
    return editor.isPrimaryEditor() ||
      EditorHelper.isFileEditor(editor) && !editor.inNormalMode ||
      ideaVimSupportDialog && !editor.inNormalMode
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
   * The list of keys that should be processed by IDE is stored in the "lookupKeys" option. So, we should search
   *   if the pressed key is presented in this list. The caches are used to speedup the process.
   */
  private object LookupKeys {
    private var parsedLookupKeys: Set<KeyStroke> = parseLookupKeys()

    init {
      VimPlugin.getOptionGroup().addListener(IjOptions.lookupkeys, { parsedLookupKeys = parseLookupKeys() })
    }

    fun isEnabledForLookup(keyStroke: KeyStroke): Boolean = keyStroke !in parsedLookupKeys

    private fun parseLookupKeys() = injector.globalIjOptions().lookupkeys
      .map { injector.parser.parseKeys(it) }
      .filter { it.isNotEmpty() }
      .map { it.first() }
      .toSet()
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
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK,
          ),
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_RIGHT,
            0,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK,
          ),
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_HOME,
            0,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK,
          ),
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_END,
            0,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK,
          ),
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_PAGE_UP,
            0,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK,
          ),
        )
        .addAll(
          getKeyStrokes(
            KeyEvent.VK_PAGE_DOWN,
            0,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK,
          ),
        ).build()

    private const val ACTION_ID = "VimShortcutKeyAction"

    private val NON_FILE_EDITOR_KEYS: Set<KeyStroke> = ImmutableSet.builder<KeyStroke>()
      .addAll(getKeyStrokes(KeyEvent.VK_ENTER, 0))
      .addAll(getKeyStrokes(KeyEvent.VK_ESCAPE, 0))
      .addAll(getKeyStrokes(KeyEvent.VK_TAB, 0))
      .addAll(getKeyStrokes(KeyEvent.VK_UP, 0))
      .addAll(getKeyStrokes(KeyEvent.VK_DOWN, 0))
      .build()

    private val LOG = logger<VimShortcutKeyAction>()

    @JvmStatic
    val instance: AnAction by lazy {
      AnActionWrapper(ActionManager.getInstance().getAction(ACTION_ID))
    }

    private fun getKeyStrokes(keyCode: Int, vararg modifiers: Int) =
      modifiers.map { KeyStroke.getKeyStroke(keyCode, it) }
  }
}

private class ActionEnableStatus(
  val isEnabled: Boolean,
  val message: String,
  val logLevel: LogLevel,
) {
  fun printLog() {
    when (logLevel) {
      LogLevel.INFO -> LOG.info("IdeaVim keys are enabled = $isEnabled: $message")
      LogLevel.DEBUG -> LOG.debug("IdeaVim keys are enabled = $isEnabled: $message")
    }
  }

  companion object {
    private val LOG = logger<ActionEnableStatus>()

    fun no(message: String, logLevel: LogLevel) = ActionEnableStatus(false, message, logLevel)
    fun yes(message: String, logLevel: LogLevel) = ActionEnableStatus(true, message, logLevel)
  }
}

private enum class LogLevel {
  DEBUG, INFO,
}