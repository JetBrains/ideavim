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
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VisualChange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.motion.VisualMotionGroup
import com.maddyhome.idea.vim.helper.CaretData
import com.maddyhome.idea.vim.helper.EditorData
import com.maddyhome.idea.vim.helper.VimSelection
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.helper.visualBlockRange

/**
 * @author Alex Plate
 */
abstract class VisualOperatorActionHandler : EditorActionHandlerBase(false) {

    protected abstract fun executeAction(editor: Editor, caret: Caret, context: DataContext, cmd: Command, range: VimSelection): Boolean

    protected open fun beforeExecution(editor: Editor, context: DataContext, cmd: Command) = true
    protected open fun afterExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}

    final override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        logger.debug("execute, cmd=$cmd")

        EditorData.setChangeSwitchMode(editor, null)
        EditorData.setWasVisualBlockMode(editor, CommandState.inVisualBlockMode(editor))

        if (CommandState.getInstance(editor).mode == CommandState.Mode.VISUAL) {
            val range = editor.visualBlockRange
            logger.debug("range=$range")
        }

        val selections = editor.collectSelections() ?: return false

        val commandWrapper = VisualStartFinishWrapper(editor, cmd)
        commandWrapper.start()

        if (!beforeExecution(editor, context, cmd)) return false

        val res = Ref.create(true)
        when {
            selections.keys.isEmpty() -> return false
            selections.keys.size == 1 -> res.set(executeAction(editor, editor.caretModel.primaryCaret, context, cmd, selections.values.first()))
            else -> editor.caretModel.runForEachCaret({ caret ->
                val range = selections.getValue(caret)
                val loopRes = executeAction(editor, caret, context, cmd, range)
                res.set(loopRes and res.get())
            }, true)
        }

        afterExecution(editor, context, cmd, res.get())

        commandWrapper.finish(res.get())

        EditorData.getChangeSwitchMode(editor)?.let {
            VimPlugin.getChange().processPostChangeModeSwitch(editor, context, it)
        }

        return res.get()
    }

    final override fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
        return super.execute(editor, caret, context, cmd)
    }

    private fun Editor.collectSelections(): Map<Caret, VimSelection>? =
            this.caretModel.allCarets.associateWith { caret ->
                val subMode = CommandState.getInstance(this).subMode
                val adj = if (VisualMotionGroup.exclusiveSelection) 0 else 1
                if (CommandState.getInstance(this).mode == CommandState.Mode.VISUAL)
                    if (CommandState.inVisualBlockMode(this)) {
                        VimSelection(caret.vimSelectionStart, caret.offset + adj, SelectionType.fromSubMode(subMode))
                    } else {
                        val primaryCaret = caretModel.primaryCaret
                        return mapOf(primaryCaret to VimSelection(primaryCaret.vimSelectionStart, primaryCaret.offset + adj, SelectionType.BLOCK_WISE))
                    }
                else {
                    val startAndEnd = VimPlugin.getMark().getVisualSelectionMarks(this) ?: return null
                    val lastSelectionType = EditorData.getLastSelectionType(this) ?: return null
                    VimSelection(startAndEnd.startOffset, startAndEnd.endOffset, lastSelectionType)
                }
            }

    protected class VisualStartFinishWrapper(private val editor: Editor, private val cmd: Command) {
        private lateinit var lastMode: CommandState.SubMode
        private var wasRepeat: Boolean = false

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
            if (CommandState.getInstance(editor).mode == CommandState.Mode.VISUAL) {
                if (!wasRepeat) {
                    change = VisualMotionGroup
                            .getVisualOperatorRange(editor, caret, cmd.flags)
                }
                logger.debug("change=$change")
            }
            CaretData.setVisualChange(caret, change)
        }

        fun start() {
            logger.debug("start")
            wasRepeat = CommandState.getInstance(editor).mode == CommandState.Mode.REPEAT
            EditorData.setKeepingVisualOperatorAction(editor, CommandFlags.FLAG_EXIT_VISUAL !in cmd.flags)

            for (caret in editor.caretModel.allCarets) {
                startForCaret(caret)
            }

            // If this is a mutli key change then exit visual now
            if (CommandFlags.FLAG_MULTIKEY_UNDO in cmd.flags) {
                logger.debug("multikey undo - exit visual")
                VisualMotionGroup.exitVisual(editor)
            } else if (CommandFlags.FLAG_FORCE_LINEWISE in cmd.flags) {
                lastMode = CommandState.getInstance(editor).subMode
                if (lastMode != CommandState.SubMode.VISUAL_LINE && CommandFlags.FLAG_FORCE_VISUAL in cmd.flags) {
                    VisualMotionGroup.toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_LINE)
                }
            }
        }

        private fun finishForCaret(caret: Caret, res: Boolean) {
            if (CommandFlags.FLAG_MULTIKEY_UNDO !in cmd.flags && CommandFlags.FLAG_EXPECT_MORE !in cmd.flags) {
                if (wasRepeat) {
                    CaretData.setLastColumn(editor, caret, CaretData.getPreviousLastColumn(caret))
                }
            }

            if (res) {
                CaretData.getVisualChange(caret)?.let {
                    CaretData.setLastVisualOperatorRange(caret, it)
                }
            }
        }

        fun finish(res: Boolean) {
            logger.debug("finish")

            if (CommandFlags.FLAG_FORCE_LINEWISE in cmd.flags) {
                if (this::lastMode.isInitialized && lastMode != CommandState.SubMode.VISUAL_LINE && CommandFlags.FLAG_FORCE_VISUAL in cmd.flags) {
                    VisualMotionGroup.toggleVisual(editor, 1, 0, lastMode)
                }
            }

            if (CommandFlags.FLAG_MULTIKEY_UNDO !in cmd.flags && CommandFlags.FLAG_EXPECT_MORE !in cmd.flags) {
                logger.debug("not multikey undo - exit visual")
                VisualMotionGroup.exitVisual(editor)
            }

            if (res) {
                CommandState.getInstance(editor).saveLastChangeCommand(cmd)
            }

            for (caret in editor.caretModel.allCarets) {
                finishForCaret(caret, res)
            }

            EditorData.setKeepingVisualOperatorAction(editor, false)
        }
    }

    companion object {
        val logger = Logger.getInstance(VisualOperatorActionHandler::class.java.name)
    }
}