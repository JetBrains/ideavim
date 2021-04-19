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

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.vimSelectionStart

/**
 * @author Alex Plate
 *
 * Base class for motion handlers.
 * @see [MotionActionHandler.SingleExecution] and [MotionActionHandler.ForEachCaret]
 */
sealed class MotionActionHandler : EditorActionHandlerBase(false) {

  /**
   * Base class for motion handlers.
   * This handler executes an action for each caret. That means that if you have 5 carets, [getOffset] will be
   *   called 5 times.
   * @see [MotionActionHandler.SingleExecution] for only one execution
   */
  abstract class ForEachCaret : MotionActionHandler() {

    /**
     * This method should return new offset for [caret]
     * It executes once for each [caret]. That means that if you have 5 carets, [getOffset] will be
     *   called 5 times.
     * The method executes only once it there is block selection.
     */
    abstract fun getOffset(
      editor: Editor,
      caret: Caret,
      context: DataContext,
      count: Int,
      rawCount: Int,
      argument: Argument?
    ): Motion

    /**
     * This method is called before [getOffset] once for each [caret].
     * The method executes only once it there is block selection.
     */
    open fun preOffsetComputation(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean = true

    /**
     * This method is called after [getOffset], but before caret motion.
     *
     * The method executes for each caret, but only once it there is block selection.
     */
    open fun preMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {}

    /**
     * This method is called after [getOffset] and after caret motion.
     *
     * The method executes for each caret, but only once it there is block selection.
     */
    open fun postMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {}
  }

  /**
   * Base class for motion handlers.
   * This handler executes an action only once for all carets. That means that if you have 5 carets,
   *   [getOffset] will be called 1 time.
   * @see [MotionActionHandler.ForEachCaret] for per-caret execution
   */
  abstract class SingleExecution : MotionActionHandler() {
    /**
     * This method should return new offset for primary caret
     * It executes once for all carets. That means that if you have 5 carets, [getOffset] will be
     *   called 1 time.
     */
    abstract fun getOffset(editor: Editor, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Motion

    /**
     * This method is called before [getOffset].
     * The method executes only once.
     */
    open fun preOffsetComputation(editor: Editor, context: DataContext, cmd: Command): Boolean = true

    /**
     * This method is called after [getOffset], but before caret motion.
     *
     * The method executes only once.
     */
    open fun preMove(editor: Editor, context: DataContext, cmd: Command) = Unit

    /**
     * This method is called after [getOffset] and after caret motion.
     *
     * The method executes only once it there is block selection.
     */
    open fun postMove(editor: Editor, context: DataContext, cmd: Command) = Unit
  }

  abstract val motionType: MotionType

  final override val type: Command.Type = Command.Type.MOTION

  fun getHandlerOffset(
    editor: Editor,
    caret: Caret,
    context: DataContext,
    count: Int,
    rawCount: Int,
    argument: Argument?
  ): Motion {
    return when (this) {
      is SingleExecution -> getOffset(editor, context, count, rawCount, argument)
      is ForEachCaret -> getOffset(editor, caret, context, count, rawCount, argument)
    }
  }

  final override fun baseExecute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
    val blockSubmodeActive = editor.inBlockSubMode

    when (this) {
      is SingleExecution -> run {
        if (!preOffsetComputation(editor, context, cmd)) return@run

        val offset = getOffset(editor, context, cmd.count, cmd.rawCount, cmd.argument)

        when (offset) {
          is Motion.AbsoluteOffset -> {
            var resultOffset = offset.offset
            if (resultOffset < 0) {
              logger<MotionActionHandler>().error("Offset is less than 0. $resultOffset. ${this.javaClass.name}")
            }
            if (CommandFlags.FLAG_SAVE_JUMP in cmd.flags) {
              VimPlugin.getMark().saveJumpLocation(editor)
            }
            if (!editor.commandState.mode.isEndAllowed) {
              resultOffset = EditorHelper.normalizeOffset(editor, resultOffset, false)
            }
            preMove(editor, context, cmd)
            MotionGroup.moveCaret(editor, editor.caretModel.primaryCaret, resultOffset)
            postMove(editor, context, cmd)
          }
          is Motion.Error -> VimPlugin.indicateError()
          is Motion.NoMotion -> Unit
        }
      }
      is ForEachCaret -> run {
        when {
          blockSubmodeActive || editor.caretModel.caretCount == 1 -> {
            val primaryCaret = editor.caretModel.primaryCaret
            doExecuteForEach(editor, primaryCaret, context, cmd)
          }
          else -> {
            try {
              editor.caretModel.addCaretListener(CaretMergingWatcher)
              editor.caretModel.runForEachCaret { caret -> doExecuteForEach(editor, caret, context, cmd) }
            } finally {
              editor.caretModel.removeCaretListener(CaretMergingWatcher)
            }
          }
        }
      }
    }

    return true
  }

  private fun doExecuteForEach(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {
    this as ForEachCaret
    if (!preOffsetComputation(editor, caret, context, cmd)) return

    val offset = getOffset(editor, caret, context, cmd.count, cmd.rawCount, cmd.argument)

    when (offset) {
      is Motion.AbsoluteOffset -> {
        var resultMotion = offset.offset
        if (resultMotion < 0) {
          logger<MotionActionHandler>().error("Offset is less than 0. $resultMotion. ${this.javaClass.name}")
        }
        if (CommandFlags.FLAG_SAVE_JUMP in cmd.flags) {
          VimPlugin.getMark().saveJumpLocation(editor)
        }
        if (!editor.commandState.mode.isEndAllowed) {
          resultMotion = EditorHelper.normalizeOffset(editor, resultMotion, false)
        }
        preMove(editor, caret, context, cmd)
        MotionGroup.moveCaret(editor, caret, resultMotion)
        val postMoveCaret = if (editor.inBlockSubMode) editor.caretModel.primaryCaret else caret
        postMove(editor, postMoveCaret, context, cmd)
      }
      is Motion.Error -> VimPlugin.indicateError()
      is Motion.NoMotion -> Unit
    }
  }

  private object CaretMergingWatcher : CaretListener {
    override fun caretRemoved(event: CaretEvent) {
      val editor = event.editor
      val caretToDelete = event.caret ?: return
      if (editor.inVisualMode) {
        for (caret in editor.caretModel.allCarets) {
          val curCaretStart = caret.selectionStart
          val curCaretEnd = caret.selectionEnd
          val caretStartBetweenCur = caretToDelete.selectionStart in curCaretStart until curCaretEnd
          val caretEndBetweenCur = caretToDelete.selectionEnd in curCaretStart + 1..curCaretEnd
          if (caretStartBetweenCur || caretEndBetweenCur) {
            // Okay, caret is being removed because of merging
            val vimSelectionStart = caretToDelete.vimSelectionStart
            caret.vimSelectionStart = vimSelectionStart
          }
        }
      }
    }
  }
}

sealed class Motion {
  object Error : Motion()
  object NoMotion : Motion()
  class AbsoluteOffset(val offset: Int) : Motion()
}

fun Int.toMotion(): Motion.AbsoluteOffset {
  if (this < 0) error("Unexpected motion: $this")
  return Motion.AbsoluteOffset(this)
}

fun Int.toMotionOrError(): Motion = if (this < 0) Motion.Error else Motion.AbsoluteOffset(this)
fun Int.toMotionOrNoMotion(): Motion = if (this < 0) Motion.NoMotion else Motion.AbsoluteOffset(this)
