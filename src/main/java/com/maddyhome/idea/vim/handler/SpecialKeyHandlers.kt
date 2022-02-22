/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * @author Alex Plate
 *
 * Handler for SHIFTED special keys except arrows, that are defined in `:h keymodel`
 * There are: <End>, <Home>, <PageUp> and <PageDown>
 *
 * This handler is used to properly handle there keys according to current `keymodel` and `selectmode` options
 *
 * Handler is called once for all carets
 */
abstract class ShiftedSpecialKeyHandler : VimActionHandler.SingleExecution() {
  final override fun execute(editor: Editor, context: DataContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val startSel = OptionConstants.keymodel_startsel in (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.keymodelName) as VimString).value
    if (startSel && !editor.inVisualMode && !editor.inSelectMode) {
      if (OptionConstants.selectmode_key in (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.selectmodeName) as VimString).value) {
        VimPlugin.getVisualMotion().enterSelectMode(editor, CommandState.SubMode.VISUAL_CHARACTER)
      } else {
        VimPlugin.getVisualMotion()
          .toggleVisual(IjVimEditor(editor), 1, 0, CommandState.SubMode.VISUAL_CHARACTER)
      }
    }
    motion(editor, context, cmd)
    return true
  }

  /**
   * This method is called when `keymodel` doesn't contain `startsel`,
   * or contains one of `continue*` values but in different mode.
   */
  abstract fun motion(editor: Editor, context: DataContext, cmd: Command)
}

/**
 * Handler for SHIFTED arrow keys
 *
 * This handler is used to properly handle there keys according to current `keymodel` and `selectmode` options
 *
 * Handler is called once for all carets
 */
abstract class ShiftedArrowKeyHandler : VimActionHandler.SingleExecution() {
  final override fun execute(editor: Editor, context: DataContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val keymodelOption = (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.keymodelName) as VimString).value
    val startSel = OptionConstants.keymodel_startsel in keymodelOption
    val inVisualMode = editor.inVisualMode
    val inSelectMode = editor.inSelectMode

    val continueSelectSelection = OptionConstants.keymodel_continueselect in keymodelOption && inSelectMode
    val continueVisualSelection = OptionConstants.keymodel_continuevisual in keymodelOption && inVisualMode
    if (startSel || continueSelectSelection || continueVisualSelection) {
      if (!inVisualMode && !inSelectMode) {
        if (OptionConstants.selectmode_key in (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.selectmodeName) as VimString).value) {
          VimPlugin.getVisualMotion().enterSelectMode(editor, CommandState.SubMode.VISUAL_CHARACTER)
        } else {
          VimPlugin.getVisualMotion()
            .toggleVisual(IjVimEditor(editor), 1, 0, CommandState.SubMode.VISUAL_CHARACTER)
        }
      }
      motionWithKeyModel(editor, context, cmd)
    } else {
      motionWithoutKeyModel(editor, context, cmd)
    }
    return true
  }

  /**
   * This method is called when `keymodel` contains `startsel`, or one of `continue*` values in corresponding mode
   */
  abstract fun motionWithKeyModel(editor: Editor, context: DataContext, cmd: Command)

  /**
   * This method is called when `keymodel` doesn't contain `startsel`,
   * or contains one of `continue*` values but in different mode.
   */
  abstract fun motionWithoutKeyModel(editor: Editor, context: DataContext, cmd: Command)
}

/**
 * Handler for NON-SHIFTED special keys, that are defined in `:h keymodel`
 * There are: cursor keys, <End>, <Home>, <PageUp> and <PageDown>
 *
 * This handler is used to properly handle there keys according to current `keymodel` and `selectmode` options
 *
 * Handler is called for each caret
 */
abstract class NonShiftedSpecialKeyHandler : MotionActionHandler.ForEachCaret() {
  final override fun getOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val keymodel = (
      VimPlugin.getOptionService()
        .getOptionValue(OptionScope.GLOBAL, OptionConstants.keymodelName) as VimString
      ).value.split(",")
    if (editor.inSelectMode && (OptionConstants.keymodel_stopsel in keymodel || OptionConstants.keymodel_stopselect in keymodel)) {
      editor.exitSelectMode(false)
    }
    if (editor.ij.inVisualMode && (OptionConstants.keymodel_stopsel in keymodel || OptionConstants.keymodel_stopvisual in keymodel)) {
      editor.ij.exitVisualMode()
    }

    return offset(editor.ij, caret.ij, context.ij, operatorArguments.count1, operatorArguments.count0, argument).toMotionOrError()
  }

  /**
   * Calculate new offset for current [caret]
   */
  abstract fun offset(
    editor: Editor,
    caret: Caret,
    context: DataContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): Int
}
