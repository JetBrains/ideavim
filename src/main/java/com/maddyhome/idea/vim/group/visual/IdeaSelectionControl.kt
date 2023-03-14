/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.editorMode
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.hasVisualSelection
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.helper.popAllModes
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.vimscript.model.options.helpers.IdeaRefactorModeHelper

internal object IdeaSelectionControl {
  /**
   * This method should be in sync with [predictMode]
   *
   * Control unexpected (non vim) selection change and adjust a mode to it. The new mode is not enabled immediately,
   *   but with some delay (using [VimVisualTimer])
   *
   * See [VimVisualTimer] to more info.
   */
  fun controlNonVimSelectionChange(
    editor: Editor,
    selectionSource: VimListenerManager.SelectionSource = VimListenerManager.SelectionSource.OTHER,
  ) {
    VimVisualTimer.singleTask(editor.editorMode) { initialMode ->

      if (editor.isIdeaVimDisabledHere) return@singleTask

      logger.debug("Adjust non-vim selection. Source: $selectionSource, initialMode: $initialMode")

      // Perform logic in one of the next cases:
      //  - There was no selection and now it is
      //  - There was a selection and now it doesn't exist
      //  - There was a selection and now it exists as well (transforming char selection to line selection, for example)
      if (initialMode?.hasVisualSelection == false && !editor.selectionModel.hasSelection(true)) {
        logger.trace { "Exiting without selection adjusting" }
        return@singleTask
      }

      if (editor.selectionModel.hasSelection(true)) {
        if (dontChangeMode(editor)) {
          IdeaRefactorModeHelper.correctSelection(editor)
          logger.trace { "Selection corrected for refactoring" }
          return@singleTask
        }

        logger.debug("Some carets have selection. State before adjustment: ${editor.vim.vimStateMachine.toSimpleString()}")

        editor.popAllModes()

        activateMode(editor, chooseSelectionMode(editor, selectionSource, true))
      } else {
        logger.debug("None of carets have selection. State before adjustment: ${editor.vim.vimStateMachine.toSimpleString()}")
        if (editor.inVisualMode) editor.vim.exitVisualMode()
        if (editor.inSelectMode) editor.exitSelectMode(false)

        if (editor.inNormalMode) {
          activateMode(editor, chooseNonSelectionMode(editor))
        }
      }

      KeyHandler.getInstance().reset(editor.vim)
      logger.debug("${editor.editorMode} is enabled")
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
  fun predictMode(editor: Editor, selectionSource: VimListenerManager.SelectionSource): VimStateMachine.Mode {
    if (editor.selectionModel.hasSelection(true)) {
      if (dontChangeMode(editor)) return editor.editorMode
      return chooseSelectionMode(editor, selectionSource, false)
    } else {
      return chooseNonSelectionMode(editor)
    }
  }

  private fun activateMode(editor: Editor, mode: VimStateMachine.Mode) {
    when (mode) {
      VimStateMachine.Mode.VISUAL -> VimPlugin.getVisualMotion()
        .enterVisualMode(editor.vim, VimPlugin.getVisualMotion().autodetectVisualSubmode(editor.vim))
      VimStateMachine.Mode.SELECT -> VimPlugin.getVisualMotion()
        .enterSelectMode(editor.vim, VimPlugin.getVisualMotion().autodetectVisualSubmode(editor.vim))
      VimStateMachine.Mode.INSERT -> VimPlugin.getChange().insertBeforeCursor(
        editor.vim,
        injector.executionContextManager.onEditor(editor.vim),
      )
      VimStateMachine.Mode.COMMAND -> Unit
      else -> error("Unexpected mode: $mode")
    }
  }

  private fun dontChangeMode(editor: Editor): Boolean =
    editor.isTemplateActive() && (IdeaRefactorModeHelper.keepMode() || editor.editorMode.hasVisualSelection)

  private fun chooseNonSelectionMode(editor: Editor): VimStateMachine.Mode {
    val templateActive = editor.isTemplateActive()
    if (templateActive && editor.inNormalMode || editor.inInsertMode) {
      return VimStateMachine.Mode.INSERT
    }
    return VimStateMachine.Mode.COMMAND
  }

  private fun chooseSelectionMode(
    editor: Editor,
    selectionSource: VimListenerManager.SelectionSource,
    logReason: Boolean,
  ): VimStateMachine.Mode {
    val selectmode = injector.options(editor.vim).getStringListValues(Options.selectmode)
    return when {
      editor.isOneLineMode -> {
        if (logReason) logger.debug("Enter select mode. Reason: one line mode")
        VimStateMachine.Mode.SELECT
      }
      selectionSource == VimListenerManager.SelectionSource.MOUSE && OptionConstants.selectmode_mouse in selectmode -> {
        if (logReason) logger.debug("Enter select mode. Selection source is mouse and selectMode option has mouse")
        VimStateMachine.Mode.SELECT
      }
      editor.isTemplateActive() && IdeaRefactorModeHelper.selectMode() -> {
        if (logReason) logger.debug("Enter select mode. Template is active and selectMode has template")
        VimStateMachine.Mode.SELECT
      }
      selectionSource == VimListenerManager.SelectionSource.OTHER && OptionConstants.selectmode_ideaselection in selectmode -> {
        if (logReason) logger.debug("Enter select mode. Selection source is OTHER and selectMode has refactoring")
        VimStateMachine.Mode.SELECT
      }
      else -> {
        if (logReason) logger.debug("Enter visual mode")
        VimStateMachine.Mode.VISUAL
      }
    }
  }

  private val logger = Logger.getInstance(IdeaSelectionControl::class.java)
}
