/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.editor.Editor
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl.controlNonVimSelectionChange
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl.predictMode
import com.maddyhome.idea.vim.helper.RWLockLabel
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.hasVisualSelection
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.helper.vimDisabled
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inCommandLineMode
import com.maddyhome.idea.vim.state.mode.inNormalMode
import com.maddyhome.idea.vim.state.mode.inSelectMode
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.vimscript.model.options.helpers.IdeaRefactorModeHelper
import com.maddyhome.idea.vim.vimscript.model.options.helpers.isIdeaRefactorModeKeep
import com.maddyhome.idea.vim.vimscript.model.options.helpers.isIdeaRefactorModeSelect

internal object IdeaSelectionControl {
  /**
   * This method should be in sync with [predictMode]
   *
   * Control unexpected (non vim) selection change and adjust a mode to it. The new mode is not enabled immediately,
   *   but with some delay (using [VimVisualTimer]). The delay is used because some platform functionality
   *   makes features by using selection. E.g. PyCharm unindent firstly select the indenting then applies delete action.
   *   Such "quick" selection breaks IdeaVim behaviour.
   *
   * See [VimVisualTimer] to more info.
   *
   * XXX: This method can be split into "change calculation" and "change apply". In this way, we would be able
   *   to calculate if we need to make a change or not and reduce the number of these calls.
   *   If this refactoring ever is applied, please add `assertNull(VimVisualTimer.timer)` to `tearDown` of VimTestCase.
   */
  fun controlNonVimSelectionChange(
    editor: Editor,
    selectionSource: VimListenerManager.SelectionSource = VimListenerManager.SelectionSource.OTHER,
  ) {
    VimVisualTimer.singleTask(editor.vim.mode) { initialMode ->
      if (vimDisabled(editor)) return@singleTask

      logger.debug("Adjust non-vim selection. Source: $selectionSource, initialMode: $initialMode")

      // Perform logic in one of the next cases:
      //  - There was no selection and now it is
      //  - There was a selection and now it doesn't exist
      //  - There was a selection and now it exists as well (transforming char selection to line selection, for example)
      val hasSelection = ApplicationManager.getApplication().runReadAction<Boolean> {
        editor.selectionModel.hasSelection(true)
      }
      if (initialMode?.hasVisualSelection == false && !hasSelection) {
        logger.trace { "Exiting without selection adjusting" }
        return@singleTask
      }

      if (hasSelection) {
        if (editor.vim.inCommandLineMode && editor.vim.mode.returnTo.hasVisualSelection) {
          logger.trace { "Modifying selection while in Command-line mode, most likely incsearch" }
          return@singleTask
        }

        if (dontChangeMode(editor)) {
          IdeaRefactorModeHelper.correctSelection(editor)
          logger.trace { "Selection corrected for refactoring" }
          return@singleTask
        }

        logger.debug("Some carets have selection. State before adjustment: ${editor.vim.mode}")

        editor.vim.mode = Mode.NORMAL()

        val mode = injector.application.runReadAction { chooseSelectionMode(editor, selectionSource, true) }
        activateMode(editor, mode)
      } else {
        logger.debug("None of carets have selection. State before adjustment: ${editor.vim.mode}")
        if (editor.vim.inVisualMode) editor.vim.exitVisualMode()
        if (editor.vim.inSelectMode) editor.vim.exitSelectMode(false)

        if (editor.vim.inNormalMode) {
          activateMode(editor, chooseNonSelectionMode(editor))
        }
      }

      KeyHandler.getInstance().reset(editor.vim)
      logger.debug("${editor.vim.mode} is enabled")
    }
  }

  /**
   * This method should be in sync with [controlNonVimSelectionChange]
   *
   * Predict the mode after changing visual selection. The prediction will be correct if there is only one sequential
   *   visual change (e.g. somebody executed "extract selection" action. The prediction can be wrong in case of
   *   multiple sequential visual changes (e.g. "technical" visual selection during typing in japanese)
   *
   * This method is created to improve user experience. It allows avoiding delay in some operations
   *   (because [controlNonVimSelectionChange] is not executed immediately)
   */
  @RWLockLabel.Readonly
  @RequiresReadLock
  fun predictMode(editor: Editor, selectionSource: VimListenerManager.SelectionSource): Mode {
    if (editor.selectionModel.hasSelection(true)) {
      if (dontChangeMode(editor)) return editor.vim.mode
      return chooseSelectionMode(editor, selectionSource, false)
    } else {
      return chooseNonSelectionMode(editor)
    }
  }

  private fun activateMode(editor: Editor, mode: Mode) {
    when (mode) {
      is Mode.VISUAL -> VimPlugin.getVisualMotion().enterVisualMode(editor.vim, mode.selectionType)
      is Mode.SELECT -> VimPlugin.getVisualMotion().enterSelectMode(editor.vim, mode.selectionType)
      is Mode.INSERT -> VimPlugin.getChange()
        .insertBeforeCaret(editor.vim, injector.executionContextManager.getEditorExecutionContext(editor.vim))

      is Mode.NORMAL -> Unit
      else -> error("Unexpected mode: $mode")
    }
  }

  private fun dontChangeMode(editor: Editor): Boolean {
    return editor.isTemplateActive() && (editor.vim.isIdeaRefactorModeKeep || editor.vim.mode.hasVisualSelection)
  }

  private fun chooseNonSelectionMode(editor: Editor): Mode {
    val templateActive = editor.isTemplateActive()
    if (templateActive && editor.vim.mode.inNormalMode || editor.inInsertMode) {
      return Mode.INSERT
    }
    return Mode.NORMAL()
  }

  @RWLockLabel.Readonly
  @RequiresReadLock
  private fun chooseSelectionMode(
    editor: Editor,
    selectionSource: VimListenerManager.SelectionSource,
    logReason: Boolean,
  ): Mode {
    val selectmode = injector.options(editor.vim).selectmode
    return when {
      editor.isOneLineMode -> {
        if (logReason) logger.debug("Enter select mode. Reason: one line mode")
        Mode.SELECT(VimPlugin.getVisualMotion().detectSelectionType(editor.vim))
      }

      selectionSource == VimListenerManager.SelectionSource.MOUSE && OptionConstants.selectmode_mouse in selectmode -> {
        if (logReason) logger.debug("Enter select mode. Selection source is mouse and selectMode option has mouse")
        Mode.SELECT(VimPlugin.getVisualMotion().detectSelectionType(editor.vim))
      }

      editor.isTemplateActive() && editor.vim.isIdeaRefactorModeSelect -> {
        if (logReason) logger.debug("Enter select mode. Template is active and selectMode has template")
        Mode.SELECT(VimPlugin.getVisualMotion().detectSelectionType(editor.vim))
      }

      selectionSource == VimListenerManager.SelectionSource.OTHER && OptionConstants.selectmode_ideaselection in selectmode -> {
        if (logReason) logger.debug("Enter select mode. Selection source is OTHER and selectMode has refactoring")
        Mode.SELECT(VimPlugin.getVisualMotion().detectSelectionType(editor.vim))
      }

      else -> {
        if (logReason) logger.debug("Enter visual mode")
        Mode.VISUAL(VimPlugin.getVisualMotion().detectSelectionType(editor.vim))
      }
    }
  }

  private val logger = Logger.getInstance(IdeaSelectionControl::class.java)
}
