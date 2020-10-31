/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

@file:JvmName("EditorHelperRt")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.util.ui.table.JBTableRowEditor
import com.maddyhome.idea.vim.option.OptionsManager
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTable
import kotlin.system.measureTimeMillis

val Editor.fileSize: Int
  get() = document.textLength

/**
 * There is a problem with one-line editors. At the moment of the editor creation, this property is always set to false.
 *   So, we should enable IdeaVim for such editors and disable it on the first interaction
 */
val Editor.isIdeaVimDisabledHere: Boolean
  get() {
    var res = true
    val start = System.currentTimeMillis()
    val times = mutableListOf<Pair<Long, String>>()
    val timeForCalculation = measureTimeMillis {
      res = (disabledInDialog.apply { times += System.currentTimeMillis() to "Disabled in dialog" }

        || (!OptionsManager.ideaenabledbufs.contains("singleline")
        .apply { times += System.currentTimeMillis() to "first single line check" }
        && isDatabaseCell(times).apply { times += System.currentTimeMillis() to "is db cell" })

        || (!OptionsManager.ideaenabledbufs.contains("singleline")
        .apply { times += System.currentTimeMillis() to "second single line check" }
        && isOneLineMode.apply { times += System.currentTimeMillis() to "is one line" })
        )
    }
    if (timeForCalculation > 10) {
      val timeDiffs = times.map { it.second + ": " + (it.first - start) }
      val message = "Time for calculation of 'isIdeaVimDisabledHere' took $timeForCalculation ms. Time diff: $timeDiffs"
      logger<Editor>().error(message)
    }
    return res
  }

private fun Editor.isDatabaseCell(times: MutableList<Pair<Long, String>>): Boolean {
  return isTableCellEditor(this.component, times)
}

private val Editor.disabledInDialog: Boolean
  get() = (!OptionsManager.ideaenabledbufs.contains("dialog") && !OptionsManager.ideaenabledbufs.contains("dialoglegacy"))
    && (!this.isPrimaryEditor() && !EditorHelper.isFileEditor(this))

/**
 * Checks if the editor is a primary editor in the main editing area.
 */
fun Editor.isPrimaryEditor(): Boolean {
  val project = project ?: return false
  val fileEditorManager = FileEditorManagerEx.getInstanceEx(project) ?: return false
  return fileEditorManager.allEditors.any { fileEditor -> this == EditorUtil.getEditorEx(fileEditor) }
}

// Optimized clone of com.intellij.ide.ui.laf.darcula.DarculaUIUtil.isTableCellEditor
private fun isTableCellEditor(c: Component, times: MutableList<Pair<Long, String>>): Boolean {
  return (java.lang.Boolean.TRUE == (c as JComponent).getClientProperty("JComboBox.isTableCellEditor"))
    .apply { times += System.currentTimeMillis() to "is property tru" } ||

    (findParentByCondition(c) { it is JBTableRowEditor } == null)
      .apply { times += System.currentTimeMillis() to "is null" } &&

    (findParentByCondition(c) { it is JTable } != null).apply { times += System.currentTimeMillis() to "is not null" }
}

private const val PARENT_BY_CONDITION_DEPTH = 10

private fun findParentByCondition(c: Component?, condition: (Component?) -> Boolean): Component? {
  var eachParent = c
  var goDeep = PARENT_BY_CONDITION_DEPTH
  while (eachParent != null && --goDeep > 0) {
    if (condition(eachParent)) return eachParent
    eachParent = eachParent.parent
  }
  return null
}
