package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.hasVisualSelection
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.popAllModes
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.option.IdeaRefactorMode
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.SelectModeOptionData

object IdeaSelectionControl {
  /**
   * This method should be in sync with [predictMode]
   *
   * Control unexpected (non vim) selection change and adjust a mode to it. The new mode is not enabled immediately,
   *   but with some delay (using [VimVisualTimer])
   *
   * See [VimVisualTimer] to more info.
   */
  fun controlNonVimSelectionChange(editor: Editor, selectionSource: VimListenerManager.SelectionSource = VimListenerManager.SelectionSource.OTHER) {
    VimVisualTimer.singleTask(editor.mode) { initialMode ->

      logger.info("Adjust non-vim selection. Source: $selectionSource")

      // Perform logic in one of the next cases:
      //  - There was no selection and now it is
      //  - There was a selection and now it doesn't exist
      //  - There was a selection and now it exists as well (transforming char selection to line selection, for example)
      if (initialMode?.hasVisualSelection == false && !editor.selectionModel.hasSelection(true)) return@singleTask

      if (editor.selectionModel.hasSelection(true)) {
        if (dontChangeMode(editor)) {
          IdeaRefactorMode.correctSelection(editor)
          return@singleTask
        }

        logger.info("Some carets have selection. State before adjustment: ${editor.commandState.toSimpleString()}")

        editor.popAllModes()

        activateMode(editor, chooseSelectionMode(editor, selectionSource, true))
      } else {
        logger.info("None of carets have selection. State before adjustment: ${editor.commandState.toSimpleString()}")
        if (editor.inVisualMode) editor.exitVisualMode()
        if (editor.inSelectMode) editor.exitSelectMode(false)

        if (editor.inNormalMode) {
          activateMode(editor, chooseNonSelectionMode(editor))
        }
      }

      KeyHandler.getInstance().reset(editor)
      updateCaretState(editor)
      logger.info("${editor.mode} is enabled")
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
  fun predictMode(editor: Editor, selectionSource: VimListenerManager.SelectionSource): CommandState.Mode {
    if (editor.selectionModel.hasSelection(true)) {
      if (dontChangeMode(editor)) return editor.mode
      return chooseSelectionMode(editor, selectionSource, false)
    } else {
      return chooseNonSelectionMode(editor)
    }
  }

  private fun activateMode(editor: Editor, mode: CommandState.Mode) {
    when (mode) {
      CommandState.Mode.VISUAL -> VimPlugin.getVisualMotion().enterVisualMode(editor, VimPlugin.getVisualMotion().autodetectVisualSubmode(editor))
      CommandState.Mode.SELECT -> VimPlugin.getVisualMotion().enterSelectMode(editor, VimPlugin.getVisualMotion().autodetectVisualSubmode(editor))
      CommandState.Mode.INSERT -> VimPlugin.getChange().insertBeforeCursor(editor, EditorDataContext(editor))
      CommandState.Mode.COMMAND -> Unit
      else -> throw RuntimeException("Unexpected mode: $mode")
    }
  }

  private fun dontChangeMode(editor: Editor): Boolean = editor.isTemplateActive() && (IdeaRefactorMode.keepMode() || editor.mode.hasVisualSelection)

  private fun chooseNonSelectionMode(editor: Editor): CommandState.Mode {
    val templateActive = editor.isTemplateActive()
    if (templateActive && editor.inNormalMode || editor.inInsertMode) {
      return CommandState.Mode.INSERT
    }
    return CommandState.Mode.COMMAND
  }

  private fun chooseSelectionMode(editor: Editor, selectionSource: VimListenerManager.SelectionSource, logReason: Boolean): CommandState.Mode {
    return when {
      editor.isOneLineMode -> {
        if (logReason) logger.info("Enter select mode. Reason: one line mode")
        CommandState.Mode.SELECT
      }
      selectionSource == VimListenerManager.SelectionSource.MOUSE && SelectModeOptionData.mouse in OptionsManager.selectmode -> {
        if (logReason) logger.info("Enter select mode. Selection source is mouse and selectMode option has mouse")
        CommandState.Mode.SELECT
      }
      editor.isTemplateActive() && IdeaRefactorMode.selectMode() -> {
        if (logReason) logger.info("Enter select mode. Template is active and selectMode has template")
        CommandState.Mode.SELECT
      }
      selectionSource == VimListenerManager.SelectionSource.OTHER && SelectModeOptionData.ideaselectionEnabled() -> {
        if (logReason) logger.info("Enter select mode. Selection source is OTHER and selectMode has refactoring")
        CommandState.Mode.SELECT
      }
      else -> {
        if (logReason) logger.info("Enter visual mode")
        CommandState.Mode.VISUAL
      }
    }
  }

  private val logger = Logger.getInstance(IdeaSelectionControl::class.java)
}
