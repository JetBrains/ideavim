package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.hasVisualSelection
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.option.IdeaRefactorMode
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.SelectModeOptionData

object IdeaSelectionControl {
  /**
   * This method should be in sync with [predictMode]
   *
   * Control unexpected (non vim) selection change and adjust mode to it. The new mode is now enabled immidiatelly,
   *   but with some delay (using [VimVisualTimer]
   *
   * See [VimVisualTimer] to more info
   */
  fun controlNonVimSelectionChange(editor: Editor, selectionSource: VimListenerManager.SelectionSource = VimListenerManager.SelectionSource.OTHER) {
    VimVisualTimer.singleTask(editor.mode) { initialMode ->
      VisualMotionGroup.logger.info("Adjust non-vim selection. Source: $selectionSource")
      if (initialMode?.hasVisualSelection == true || editor.caretModel.allCarets.any(Caret::hasSelection)) {
        if (editor.caretModel.allCarets.any(Caret::hasSelection)) {
          val commandState = CommandState.getInstance(editor)
          if (editor.isTemplateActive() && IdeaRefactorMode.keepMode()) {
            IdeaRefactorMode.correctSelection(editor)
            return@singleTask
          }
          VisualMotionGroup.logger.info("Some carets have selection. State before adjustment: ${commandState.toSimpleString()}")
          while (commandState.mode != CommandState.Mode.COMMAND) {
            commandState.popState()
          }
          val autodetectedMode = VimPlugin.getVisualMotion().autodetectVisualSubmode(editor)
          val selectMode = OptionsManager.selectmode
          when {
            editor.isOneLineMode -> {
              VisualMotionGroup.logger.info("Enter select mode. Reason: one line mode")
              VimPlugin.getVisualMotion().enterSelectMode(editor, autodetectedMode)
            }
            selectionSource == VimListenerManager.SelectionSource.MOUSE && SelectModeOptionData.mouse in selectMode -> {
              VisualMotionGroup.logger.info("Enter select mode. Selection source is mouse and selectMode option has mouse")
              VimPlugin.getVisualMotion().enterSelectMode(editor, autodetectedMode)
            }
            editor.isTemplateActive() && IdeaRefactorMode.selectMode() -> {
              VisualMotionGroup.logger.info("Enter select mode. Template is active and selectMode has template")
              VimPlugin.getVisualMotion().enterSelectMode(editor, autodetectedMode)
            }
            selectionSource == VimListenerManager.SelectionSource.OTHER && SelectModeOptionData.refactoring in selectMode -> {
              VisualMotionGroup.logger.info("Enter select mode. Selection source is OTHER and selectMode has refactoring")
              VimPlugin.getVisualMotion().enterSelectMode(editor, autodetectedMode)
            }
            else -> {
              VisualMotionGroup.logger.info("Enter visual mode")
              VimPlugin.getVisualMotion().enterVisualMode(editor, autodetectedMode)
            }
          }
          KeyHandler.getInstance().reset(editor)
        } else {
          val commandState = CommandState.getInstance(editor)
          VisualMotionGroup.logger.info("None of carets have selection. State before adjustment: ${commandState.toSimpleString()}")
          VimPlugin.getVisualMotion().exitVisual(editor)
          VimPlugin.getVisualMotion().exitSelectModeAndResetKeyHandler(editor, true)

          val templateActive = editor.isTemplateActive()
          if (templateActive && editor.inNormalMode) {
            VimPlugin.getChange().insertBeforeCursor(editor, EditorDataContext(editor))
          }
          KeyHandler.getInstance().reset(editor)
        }
      }
      updateCaretState(editor)
      VisualMotionGroup.logger.info("${editor.mode} is enabled")
    }
  }

  /**
   * This method should be in sync with [controlNonVimSelectionChange]
   *
   * Predict mode after changing visual selection. The prediction will be correct if there is only one sequential
   *   visual change (e.g. somebody executed "extract selection" action. The prediction can be wrong in case of
   *   multiple sequential visual changes (e.g. "technical" visual selection during typing in japanese)
   *
   * This method is created to improve user experience. It allows to avoid delay in some operations
   *   (because [controlNonVimSelectionChange] is not executed immediately)
   */
  fun predictMode(editor: Editor, selectionSource: VimListenerManager.SelectionSource): CommandState.Mode {
    if (editor.caretModel.allCarets.any(Caret::hasSelection)) {
      val selectMode = OptionsManager.selectmode
      if (editor.isTemplateActive() && IdeaRefactorMode.keepMode()) {
        return editor.mode
      }
      return when {
        editor.isOneLineMode -> {
          CommandState.Mode.SELECT
        }
        selectionSource == VimListenerManager.SelectionSource.MOUSE && SelectModeOptionData.mouse in selectMode -> {
          CommandState.Mode.SELECT
        }
        editor.isTemplateActive() && IdeaRefactorMode.selectMode() -> {
          CommandState.Mode.SELECT
        }
        selectionSource == VimListenerManager.SelectionSource.OTHER && SelectModeOptionData.refactoring in selectMode -> {
          CommandState.Mode.SELECT
        }
        else -> {
          CommandState.Mode.VISUAL
        }
      }
    } else {
      val templateActive = editor.isTemplateActive()
      if (templateActive && editor.inNormalMode || editor.inInsertMode) {
        return CommandState.Mode.INSERT
      }
      return CommandState.Mode.COMMAND
    }
  }
}
