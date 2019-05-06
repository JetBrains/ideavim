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

package com.maddyhome.idea.vim.group

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.TemplateManagerListener
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.group.visual.moveCaretOneCharLeftFromSelectionEnd
import com.maddyhome.idea.vim.helper.EditorDataContext

/**
 * @author Alex Plate
 */
object IdeaSpecifics {
    fun addIdeaSpecificsListener(project: Project) {
        val connection = project.messageBus.connect()
        connection.subscribe(AnActionListener.TOPIC, VimActionListener)
        connection.subscribe(TemplateManager.TEMPLATE_STARTED_TOPIC, VimTemplateManagerListener)
    }

    object VimActionListener : AnActionListener {
        private val surrounderItems = listOf("if", "if / else")
        private val surrounderAction = "com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler\$InvokeSurrounderAction"

        override fun beforeActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
            if (!VimPlugin.isEnabled()) return
        }

        override fun afterActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
            if (!VimPlugin.isEnabled()) return

            val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
            when (ActionManager.getInstance().getId(action)) {
                IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET, IdeActions.ACTION_EDITOR_UNSELECT_WORD_AT_CARET -> {
                    // Rider moves caret to the end of selection
                    editor.caretModel.addCaretListener(object : CaretListener {
                        override fun caretPositionChanged(event: CaretEvent) {
                            moveCaretOneCharLeftFromSelectionEnd(event.editor)
                            event.editor.caretModel.removeCaretListener(this)
                        }
                    })
                }
            }

            if (surrounderAction == action.javaClass.name && surrounderItems.any { action.templatePresentation.text.endsWith(it) }) {
                // Enter insert mode after surround with if
                VimPlugin.getChange().insertBeforeCursor(editor, dataContext)
                KeyHandler.getInstance().reset(editor)
            }
        }
    }

    object VimTemplateManagerListener : TemplateManagerListener {
        override fun templateStarted(state: TemplateState) {
            val editor = state.editor
            if (!editor.selectionModel.hasSelection()) {
                VimPlugin.getChange().insertBeforeCursor(editor, EditorDataContext(editor))
                KeyHandler.getInstance().reset(editor)
            }
        }
    }
}