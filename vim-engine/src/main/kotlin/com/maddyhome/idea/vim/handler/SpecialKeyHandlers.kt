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

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
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
abstract class ShiftedSpecialKeyHandler : VimActionHandler.ConditionalMulticaret() {
  final override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    error("This method should not be executed")
  }

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    motion(editor, context, cmd, caret)
    return true
  }

  override fun runAsMulticaret(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val startSel = OptionConstants.keymodel_startsel in (injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.keymodelName) as VimString).value
    if (startSel && !editor.inVisualMode && !editor.inSelectMode) {
      if (OptionConstants.selectmode_key in (injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.selectmodeName) as VimString).value) {
        injector.visualMotionGroup.enterSelectMode(editor, VimStateMachine.SubMode.VISUAL_CHARACTER)
      } else {
        injector.visualMotionGroup
          .toggleVisual(editor, 1, 0, VimStateMachine.SubMode.VISUAL_CHARACTER)
      }
    }
    return true
  }

  /**
   * This method is called when `keymodel` doesn't contain `startsel`,
   * or contains one of `continue*` values but in different mode.
   */
  abstract fun motion(editor: VimEditor, context: ExecutionContext, cmd: Command, caret: VimCaret)
}

/**
 * Handler for SHIFTED arrow keys
 *
 * This handler is used to properly handle there keys according to current `keymodel` and `selectmode` options
 *
 * Handler is called once for all carets
 */
abstract class ShiftedArrowKeyHandler(private val runBothCommandsAsMulticaret: Boolean) : VimActionHandler.ConditionalMulticaret() {

  override fun runAsMulticaret(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val (inVisualMode, inSelectMode, withKey) = withKeyOrNot(editor)
    if (withKey) {
      if (!inVisualMode && !inSelectMode) {
        if (OptionConstants.selectmode_key in (injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.selectmodeName) as VimString).value) {
          injector.visualMotionGroup.enterSelectMode(editor, VimStateMachine.SubMode.VISUAL_CHARACTER)
        } else {
          injector.visualMotionGroup
            .toggleVisual(editor, 1, 0, VimStateMachine.SubMode.VISUAL_CHARACTER)
        }
      }
      return true
    } else {
      return runBothCommandsAsMulticaret
    }
  }

  private fun withKeyOrNot(editor: VimEditor): Triple<Boolean, Boolean, Boolean> {
    val keymodelOption =
      (injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.keymodelName) as VimString).value
    val startSel = OptionConstants.keymodel_startsel in keymodelOption
    val inVisualMode = editor.inVisualMode
    val inSelectMode = editor.inSelectMode

    val continueSelectSelection = OptionConstants.keymodel_continueselect in keymodelOption && inSelectMode
    val continueVisualSelection = OptionConstants.keymodel_continuevisual in keymodelOption && inVisualMode
    val withKey = startSel || continueSelectSelection || continueVisualSelection
    return Triple(inVisualMode, inSelectMode, withKey)
  }

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (runBothCommandsAsMulticaret) {
      val (_, _, withKey) = withKeyOrNot(editor)
      if (withKey) {
        motionWithKeyModel(editor, caret, context, cmd)
      } else {
        motionWithoutKeyModel(editor, context, cmd)
      }
    } else {
      motionWithKeyModel(editor, caret, context, cmd)
    }
    return true
  }

  final override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    motionWithoutKeyModel(editor, context, cmd)
    return true
  }

  /**
   * This method is called when `keymodel` contains `startsel`, or one of `continue*` values in corresponding mode
   */
  abstract fun motionWithKeyModel(editor: VimEditor, caret: VimCaret, context: ExecutionContext, cmd: Command)

  /**
   * This method is called when `keymodel` doesn't contain `startsel`,
   * or contains one of `continue*` values but in different mode.
   */
  abstract fun motionWithoutKeyModel(editor: VimEditor, context: ExecutionContext, cmd: Command)
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
      injector.optionService.getOptionValue(
        OptionScope.GLOBAL,
        OptionConstants.keymodelName
      ) as VimString
      ).value.split(",")
    if (editor.inSelectMode && (OptionConstants.keymodel_stopsel in keymodel || OptionConstants.keymodel_stopselect in keymodel)) {
      editor.exitSelectModeNative(false)
    }
    if (editor.inVisualMode && (OptionConstants.keymodel_stopsel in keymodel || OptionConstants.keymodel_stopvisual in keymodel)) {
      editor.exitVisualModeNative()
    }

    return offset(editor, caret, context, operatorArguments.count1, operatorArguments.count0, argument).toMotionOrError()
  }

  /**
   * Calculate new offset for current [caret]
   */
  abstract fun offset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): Int
}
