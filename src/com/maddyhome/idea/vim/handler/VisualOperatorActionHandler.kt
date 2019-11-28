/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.change.VimRepeater
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.visual.VimBlockSelection
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.group.visual.VimSimpleSelection
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.group.visual.VisualOperation
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.inRepeatMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.vimChangeActionSwitchMode
import com.maddyhome.idea.vim.helper.vimForEachCaret
import com.maddyhome.idea.vim.helper.vimKeepingVisualOperatorAction
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimLastSelectionType
import com.maddyhome.idea.vim.helper.vimLastVisualOperatorRange
import com.maddyhome.idea.vim.helper.vimSelectionStart

/**
 * @author Alex Plate
 *
 * Base class for visual operation handlers.
 * @see [VisualOperatorActionHandler.SingleExecution] and [VisualOperatorActionHandler.ForEachCaret]
 */
sealed class VisualOperatorActionHandler : VimActionHandler.SingleExecution() {
  /**
   * Base class for visual operation handlers.
   * This handler executes an action for each caret. That means that if you have 5 carets,
   *   [executeAction] will be called 5 times.
   * @see [VisualOperatorActionHandler.SingleExecution] for only one execution
   */
  abstract class ForEachCaret : VisualOperatorActionHandler() {

    /**
     * Execute an action for current [caret].
     * The selection offsets and type should be takes from [range] because this [caret] doesn't have this selection
     *   anymore in time of action execution (and editor is in normal mode, not visual).
     *
     * This method is executed once for each caret except case with block selection. If there is block selection,
     *   the method will be executed only once with [Caret#primaryCaret].
     */
    abstract fun executeAction(editor: Editor, caret: Caret, context: DataContext, cmd: Command, range: VimSelection): Boolean

    /**
     * This method executes before [executeAction] and only once for all carets.
     * [caretsAndSelections] contains a map of all current carets and corresponding selections.
     *   If there is block selection, only one caret is in [caretsAndSelections].
     */
    open fun beforeExecution(editor: Editor, context: DataContext, cmd: Command, caretsAndSelections: Map<Caret, VimSelection>) = true

    /**
     * This method executes after [executeAction] and only once for all carets.
     * [res] has true if ALL executions of [executeAction] returned true.
     */
    open fun afterExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}
  }

  /**
   * Base class for visual operation handlers.
   * This handler executes an action only once for all carets. That means that if you have 5 carets,
   *   [executeAction] will be called 1 time.
   * @see [VisualOperatorActionHandler.ForEachCaret] for per-caret execution
   */
  abstract class SingleExecution : VisualOperatorActionHandler() {
    /**
     * Execute an action
     * [caretsAndSelections] contains a map of all current carets and corresponding selections.
     *   If there is block selection, only one caret is in [caretsAndSelections].
     *
     * This method is executed once for all carets.
     */
    abstract fun executeForAllCarets(editor: Editor, context: DataContext, cmd: Command, caretsAndSelections: Map<Caret, VimSelection>): Boolean
  }

  final override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    logger.info("Execute visual command $cmd")

    editor.vimChangeActionSwitchMode = null

    val selections = editor.collectSelections() ?: return false
    logger.debug { "Count of selection segments: ${selections.size}" }
    logger.debug { selections.values.joinToString("\n") { vimSelection -> "Caret: $vimSelection" } }

    val commandWrapper = VisualStartFinishWrapper(editor, cmd)
    commandWrapper.start()

    val res = Ref.create(true)
    when (this) {
      is SingleExecution -> {
        res.set(executeForAllCarets(editor, context, cmd, selections))
      }
      is ForEachCaret -> {
        logger.debug("Calling 'before execution'")
        if (!beforeExecution(editor, context, cmd, selections)) {
          logger.debug("Before execution block returned false. Stop further processing")
          return false
        }

        when {
          selections.keys.isEmpty() -> return false
          selections.keys.size == 1 -> res.set(executeAction(editor, selections.keys.first(), context, cmd, selections.values.first()))
          else -> editor.caretModel.runForEachCaret({ caret ->
            val range = selections.getValue(caret)
            val loopRes = executeAction(editor, caret, context, cmd, range)
            res.set(loopRes and res.get())
          }, true)
        }

        logger.debug("Calling 'after execution'")
        afterExecution(editor, context, cmd, res.get())
      }
    }

    commandWrapper.finish(res.get())

    editor.vimChangeActionSwitchMode?.let {
      VimPlugin.getChange().processPostChangeModeSwitch(editor, context, it)
    }

    return res.get()
  }

  private fun Editor.collectSelections(): Map<Caret, VimSelection>? {

    return when {
      this.inRepeatMode -> {
        if (this.vimLastSelectionType == SelectionType.BLOCK_WISE) {
          val primaryCaret = caretModel.primaryCaret
          val range = primaryCaret.vimLastVisualOperatorRange ?: return null
          val end = VisualOperation.calculateRange(this, range, 1, primaryCaret)
          mapOf(primaryCaret to VimBlockSelection(
            primaryCaret.offset,
            end,
            this, range.columns >= MotionGroup.LAST_COLUMN))
        } else {
          val carets = mutableMapOf<Caret, VimSelection>()
          this.caretModel.allCarets.forEach { caret ->
            val range = caret.vimLastVisualOperatorRange ?: return@forEach
            val end = VisualOperation.calculateRange(this, range, 1, caret)
            carets += caret to VimSelection.create(caret.offset, end, range.type, this)
          }
          carets.toMap()
        }
      }
      this.inBlockSubMode -> {
        val primaryCaret = caretModel.primaryCaret
        mapOf(primaryCaret to VimBlockSelection(
          primaryCaret.vimSelectionStart,
          primaryCaret.offset,
          this, primaryCaret.vimLastColumn >= MotionGroup.LAST_COLUMN))
      }
      else -> this.caretModel.allCarets.associateWith { caret ->

        val subMode = this.commandState.subMode
        VimSimpleSelection.createWithNative(
          caret.vimSelectionStart,
          caret.offset,
          caret.selectionStart,
          caret.selectionEnd,
          SelectionType.fromSubMode(subMode),
          this
        )
      }
    }
  }

  private class VisualStartFinishWrapper(private val editor: Editor, private val cmd: Command) {
    private val visualChanges = mutableMapOf<Caret, VisualChange?>()

    fun start() {
      logger.debug("Preparing visual command")
      editor.vimKeepingVisualOperatorAction = CommandFlags.FLAG_EXIT_VISUAL !in cmd.flags

      editor.vimForEachCaret {
        val change = if (this@VisualStartFinishWrapper.editor.inVisualMode && !this@VisualStartFinishWrapper.editor.inRepeatMode) {
          VisualOperation.getRange(this@VisualStartFinishWrapper.editor, it, this@VisualStartFinishWrapper.cmd.flags)
        } else null
        this@VisualStartFinishWrapper.visualChanges[it] = change
      }
      logger.debug { visualChanges.values.joinToString("\n") { "Caret: $visualChanges" } }

      // If this is a mutli key change then exit visual now
      if (CommandFlags.FLAG_MULTIKEY_UNDO in cmd.flags || CommandFlags.FLAG_EXIT_VISUAL in cmd.flags) {
        logger.debug("Exit visual before command executing")
        editor.exitVisualMode()
      }
    }

    fun finish(res: Boolean) {
      logger.debug("Finish visual command. Result: $res")

      if (CommandFlags.FLAG_MULTIKEY_UNDO !in cmd.flags && CommandFlags.FLAG_EXPECT_MORE !in cmd.flags) {
        logger.debug("Not multikey undo - exit visual")
        editor.exitVisualMode()
      }

      if (res) {
        VimRepeater.saveLastChange(cmd)
        VimRepeater.repeatHandler = false
        editor.vimForEachCaret { caret -> visualChanges[caret]?.let { caret.vimLastVisualOperatorRange = it } }
        editor.caretModel.allCarets.forEach { it.vimLastColumn = it.visualPosition.column }
      }

      editor.vimKeepingVisualOperatorAction = false
    }
  }

  private companion object {
    val logger = Logger.getInstance(VisualOperatorActionHandler::class.java.name)
  }
}
