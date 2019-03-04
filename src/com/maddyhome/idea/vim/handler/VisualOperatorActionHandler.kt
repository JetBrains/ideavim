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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.VisualChange
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.motion.VisualMotionGroup
import com.maddyhome.idea.vim.helper.CaretData
import com.maddyhome.idea.vim.helper.EditorData
import com.maddyhome.idea.vim.helper.visualBlockRange
import com.maddyhome.idea.vim.helper.visualRangeMarker
import java.util.*

/**
 * @author Alex Plate
 */
abstract class VisualOperatorActionHandler : EditorActionHandlerBase(false) {

    protected abstract fun executeCharacterAndLinewise(editor: Editor, caret: Caret, context: DataContext, cmd: Command, range: RangeMarker): Boolean
    protected abstract fun executeBlockwise(editor: Editor, context: DataContext, cmd: Command, ranges: Map<Caret, RangeMarker>): Boolean

    protected open fun beforeExecution(editor: Editor, context: DataContext, cmd: Command) = true
    protected open fun afterExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}

    protected open fun beforeCaLExecution(editor: Editor, context: DataContext, cmd: Command) = true
    protected open fun afterCaLExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}

    protected open fun beforeBlockExecution(editor: Editor, context: DataContext, cmd: Command) = true
    protected open fun afterBlockExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}

    final override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        logger.debug("execute, cmd=$cmd")

        EditorData.setChangeSwitchMode(editor, null)
        EditorData.setWasVisualBlockMode(editor, CommandState.inVisualBlockMode(editor))

        if (CommandState.getInstance(editor).mode == CommandState.Mode.VISUAL) {
            val range = editor.visualBlockRange
            logger.debug("range=$range")
        }

        val ranges = editor.collectVisualRanges() ?: return false

        val executeInBatch = operateCaretsInBatch || CommandState.inVisualBlockMode(editor)
        val runnable = VisualStartFinishRunnable(editor, cmd, true)
        runnable.start()

        if (!beforeExecution(editor, context, cmd)) return false

        val res = Ref.create(true)
        if (executeInBatch) {
            if (!beforeBlockExecution(editor, context, cmd)) return false
            res.set(executeBlockwise(editor, context, cmd, ranges))
            afterBlockExecution(editor, context, cmd, res.get())
        } else {
            if (!beforeCaLExecution(editor, context, cmd)) return false
            editor.caretModel.runForEachCaret({ caret ->
                val range = ranges.getValue(caret)
                val loopRes = executeCharacterAndLinewise(editor, caret, context, cmd, range)
                res.set(loopRes and res.get())
            }, true)
            afterCaLExecution(editor, context, cmd, res.get())
        }

        afterExecution(editor, context, cmd, res.get())

        runnable.setRes(res.get())
        runnable.finish()
        ranges.values.forEach { it.dispose() }

        EditorData.getChangeSwitchMode(editor)?.let {
            VimPlugin.getChange().processPostChangeModeSwitch(editor, context, it)
        }

        return res.get()
    }

    protected open val operateCaretsInBatch: Boolean = false

    private fun Editor.collectVisualRanges(): Map<Caret, RangeMarker>? = this.caretModel.allCarets.associateWith {
        if (CommandState.getInstance(this).mode == CommandState.Mode.VISUAL)
            it.visualRangeMarker
        else {
            val startAndEnd = VimPlugin.getMark().getVisualSelectionMarks(this) ?: return null
            this.document.createRangeMarker(startAndEnd.startOffset, startAndEnd.endOffset, true)
        }
    }

    protected class VisualStartFinishRunnable(private val editor: Editor, private val cmd: Command?, private val myRunForEachCaret: Boolean) {
        private var res: Boolean = false
        private lateinit var lastMode: CommandState.SubMode
        private var wasRepeat: Boolean = false

        init {
            this.res = true
        }

        fun setRes(res: Boolean) {
            this.res = res
        }

        private fun startForCaret(caret: Caret) {
            if (CommandState.getInstance(editor).mode == CommandState.Mode.REPEAT) {
                CaretData.setPreviousLastColumn(caret, CaretData.getLastColumn(caret))
                val range = CaretData.getLastVisualOperatorRange(caret)
                VisualMotionGroup.toggleVisual(editor, 1, 1, CommandState.SubMode.NONE)
                if (range != null && range.columns == MotionGroup.LAST_COLUMN) {
                    CaretData.setLastColumn(editor, caret, MotionGroup.LAST_COLUMN)
                }
            }

            var change: VisualChange? = null
            var res: TextRange? = null
            if (CommandState.getInstance(editor).mode == CommandState.Mode.VISUAL) {
                res = if (!myRunForEachCaret) {
                    VisualMotionGroup.getVisualRange(editor)
                } else {
                    VisualMotionGroup.getVisualRange(caret)
                }
                if (!wasRepeat) {
                    change = VisualMotionGroup
                            .getVisualOperatorRange(editor, caret, cmd?.flags
                                    ?: EnumSet.of(CommandFlags.FLAG_MOT_LINEWISE))
                }
                if (logger.isDebugEnabled) logger.debug("change=$change")
            }
            CaretData.setVisualChange(caret, change)
        }

        fun start() {
            logger.debug("start")
            if (cmd == null) return
            wasRepeat = CommandState.getInstance(editor).mode == CommandState.Mode.REPEAT
            EditorData.setKeepingVisualOperatorAction(editor, !cmd.flags.contains(CommandFlags.FLAG_EXIT_VISUAL))

            if (myRunForEachCaret) {
                for (caret in editor.caretModel.allCarets) {
                    startForCaret(caret)
                }
            } else {
                startForCaret(editor.caretModel.primaryCaret)
            }

            // If this is a mutli key change then exit visual now
            if (cmd.flags.contains(CommandFlags.FLAG_MULTIKEY_UNDO)) {
                logger.debug("multikey undo - exit visual")
                VisualMotionGroup.exitVisual(editor)
            } else if (cmd.flags.contains(CommandFlags.FLAG_FORCE_LINEWISE)) {
                lastMode = CommandState.getInstance(editor).subMode
                if (lastMode != CommandState.SubMode.VISUAL_LINE && cmd.flags.contains(CommandFlags.FLAG_FORCE_VISUAL)) {
                    VisualMotionGroup.toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_LINE)
                }
            }
        }

        private fun finishForCaret(caret: Caret) {
            if (cmd == null || !cmd.flags.contains(CommandFlags.FLAG_MULTIKEY_UNDO) && !cmd.flags.contains(CommandFlags.FLAG_EXPECT_MORE)) {
                if (wasRepeat) {
                    CaretData.setLastColumn(editor, caret, CaretData.getPreviousLastColumn(caret))
                }
            }

            if (res) {
                val change = CaretData.getVisualChange(caret)
                if (change != null) {
                    CaretData.setLastVisualOperatorRange(caret, change)
                }
            }
        }

        fun finish() {
            logger.debug("finish")

            if (cmd != null && cmd.flags.contains(CommandFlags.FLAG_FORCE_LINEWISE)) {
                if (this::lastMode.isInitialized && lastMode != CommandState.SubMode.VISUAL_LINE && cmd.flags.contains(CommandFlags.FLAG_FORCE_VISUAL)) {
                    VisualMotionGroup.toggleVisual(editor, 1, 0, lastMode)
                }
            }

            if (cmd == null || !cmd.flags.contains(CommandFlags.FLAG_MULTIKEY_UNDO) && !cmd.flags.contains(CommandFlags.FLAG_EXPECT_MORE)) {
                logger.debug("not multikey undo - exit visual")
                VisualMotionGroup.exitVisual(editor)
            }

            if (res) {
                if (cmd != null) {
                    CommandState.getInstance(editor).saveLastChangeCommand(cmd)
                }
            }

            if (myRunForEachCaret) {
                for (caret in editor.caretModel.allCarets) {
                    finishForCaret(caret)
                }
            } else {
                finishForCaret(editor.caretModel.primaryCaret)
            }

            EditorData.setKeepingVisualOperatorAction(editor, false)
        }
    }

    companion object {
        val logger = Logger.getInstance(VisualOperatorActionHandler::class.java.name)
    }
}