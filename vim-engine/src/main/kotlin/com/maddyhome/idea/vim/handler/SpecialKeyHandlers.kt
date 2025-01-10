/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inSelectMode
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.isInsertionAllowed

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
    val startSel = injector.globalOptions().keymodel.contains(OptionConstants.keymodel_startsel)
    if (startSel && !editor.inVisualMode && !editor.inSelectMode) {
      if (injector.globalOptions().selectmode.contains(OptionConstants.selectmode_key)) {
        injector.visualMotionGroup.enterSelectMode(editor, SelectionType.CHARACTER_WISE)
      } else {
        injector.visualMotionGroup.enterVisualMode(editor, SelectionType.CHARACTER_WISE)
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
        if (injector.globalOptions().selectmode.contains(OptionConstants.selectmode_key)) {
          // Note that this will correctly choose either Select or Insert Select modes
          injector.visualMotionGroup.enterSelectMode(editor, SelectionType.CHARACTER_WISE)
        } else {
          if (editor.isInsertionAllowed) {
            // Enter Insert/Replace Visual mode, passing in the current Insert/Replace mode as pending
            injector.visualMotionGroup.enterVisualMode(editor, SelectionType.CHARACTER_WISE, editor.mode)
          } else {
            injector.visualMotionGroup.enterVisualMode(editor, SelectionType.CHARACTER_WISE)
          }
        }
      }
      return true
    } else {
      return runBothCommandsAsMulticaret
    }
  }

  private fun withKeyOrNot(editor: VimEditor): Triple<Boolean, Boolean, Boolean> {
    val keymodelOption = injector.globalOptions().keymodel
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
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val keymodel = injector.globalOptions().keymodel
    if (editor.inSelectMode && (OptionConstants.keymodel_stopsel in keymodel || OptionConstants.keymodel_stopselect in keymodel)) {
      editor.exitSelectModeNative(false)
    }
    if (editor.inVisualMode && (OptionConstants.keymodel_stopsel in keymodel || OptionConstants.keymodel_stopvisual in keymodel)) {
      editor.exitVisualMode()
    }

    return motion(editor, caret, context, argument, operatorArguments)
  }

  /**
   * Calculate new offset for current [caret]
   */
  abstract fun motion(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion
}
