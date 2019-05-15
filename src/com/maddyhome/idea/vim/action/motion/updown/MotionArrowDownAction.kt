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

package com.maddyhome.idea.vim.action.motion.updown

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.*
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.option.Options
import com.maddyhome.idea.vim.option.Options.KEYMODEL
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

@Suppress("DuplicatedCode")
private object MotionArrowDownActionHandler : MotionActionHandler.ForEachCaret() {

    private var col: Int = 0

    override fun getOffset(editor: Editor,
                           caret: Caret,
                           context: DataContext,
                           count: Int,
                           rawCount: Int,
                           argument: Argument?): Int {
        val keymodel = Options.getInstance().getListOption(KEYMODEL)
        if (CommandState.inSelectMode(editor) && (keymodel?.contains("stopsel") == true || keymodel?.contains("stopselect") == true)) {
            VimPlugin.getVisualMotion().exitSelectMode(editor, false)
        }
        if (CommandState.inVisualMode(editor) && (keymodel?.contains("stopsel") == true || keymodel?.contains("stopvisual") == true)) {
            VimPlugin.getVisualMotion().exitVisual(editor)
        }

        return VimPlugin.getMotion().moveCaretVertical(editor, caret, count)
    }

    override fun preOffsetComputation(editor: Editor,
                                      caret: Caret,
                                      context: DataContext,
                                      cmd: Command): Boolean {
        col = caret.vimLastColumn
        return true
    }

    override fun postMove(editor: Editor, caret: Caret, context: DataContext,
                          cmd: Command) {
        caret.vimLastColumn = col
    }
}

class MotionArrowDownAction : VimCommandAction(MotionArrowDownActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.NVOS

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = mutableSetOf(parseKeys("<Down>"), mutableListOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0)))

    override fun getType(): Command.Type = Command.Type.MOTION

    override fun getFlags(): EnumSet<CommandFlags> = EnumSet.of(CommandFlags.FLAG_MOT_LINEWISE)
}
