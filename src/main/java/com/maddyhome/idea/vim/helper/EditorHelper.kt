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

@file:JvmName("EditorHelperRt")

package com.maddyhome.idea.vim.helper

import com.intellij.codeWithMe.ClientId
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.util.ui.table.JBTableRowEditor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTable

val Editor.fileSize: Int
  get() = document.textLength

/**
 * There is a problem with one-line editors. At the moment of the editor creation, this property is always set to false.
 *   So, we should enable IdeaVim for such editors and disable it on the first interaction
 */
val Editor.isIdeaVimDisabledHere: Boolean
  get() {
    val ideaVimSupportValue = (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, IjVimOptionService.ideavimsupportName) as VimString).value
    return disabledInDialog ||
      (!ClientId.isCurrentlyUnderLocalId) || // CWM-927
      (!ideaVimSupportValue.contains(IjVimOptionService.ideavimsupport_singleline) && isDatabaseCell()) ||
      (!ideaVimSupportValue.contains(IjVimOptionService.ideavimsupport_singleline) && isOneLineMode)
  }

private fun Editor.isDatabaseCell(): Boolean {
  return isTableCellEditor(this.component)
}

private val Editor.disabledInDialog: Boolean
  get() {
    val ideaVimSupportValue = (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, IjVimOptionService.ideavimsupportName) as VimString).value
    return (!ideaVimSupportValue.contains(IjVimOptionService.ideavimsupport_dialog) && !ideaVimSupportValue.contains(IjVimOptionService.ideavimsupport_dialoglegacy)) &&
      (!this.isPrimaryEditor() && !EditorHelper.isFileEditor(this))
  }

/**
 * Checks if the editor is a primary editor in the main editing area.
 */
fun Editor.isPrimaryEditor(): Boolean {
  val project = project ?: return false
  val fileEditorManager = FileEditorManagerEx.getInstanceEx(project) ?: return false
  return fileEditorManager.allEditors.any { fileEditor -> this == EditorUtil.getEditorEx(fileEditor) }
}

// Optimized clone of com.intellij.ide.ui.laf.darcula.DarculaUIUtil.isTableCellEditor
private fun isTableCellEditor(c: Component): Boolean {
  return (java.lang.Boolean.TRUE == (c as JComponent).getClientProperty("JComboBox.isTableCellEditor")) ||
    (findParentByCondition(c) { it is JTable } != null) &&
    (findParentByCondition(c) { it is JBTableRowEditor } == null)
}

private const val PARENT_BY_CONDITION_DEPTH = 10

private inline fun findParentByCondition(c: Component?, condition: (Component?) -> Boolean): Component? {
  var eachParent = c
  var goDeep = PARENT_BY_CONDITION_DEPTH
  while (eachParent != null && --goDeep > 0) {
    if (condition(eachParent)) return eachParent
    eachParent = eachParent.parent
  }
  return null
}

fun Editor.endsWithNewLine(): Boolean {
  val textLength = this.document.textLength
  if (textLength == 0) return false
  return this.document.charsSequence[textLength - 1] == '\n'
}

/**
 * Get caret line in vim notation (1-based)
 */
val Caret.vimLine: Int
  get() = this.logicalPosition.line + 1

/**
 * Get current caret line in vim notation (1-based)
 */
val Editor.vimLine: Int
  get() = this.caretModel.currentCaret.vimLine

inline fun Editor.runWithEveryCaretAndRestore(action: () -> Unit) {
  val caretModel = this.caretModel
  val carets = if (this.inBlockSubMode) null else caretModel.allCarets
  if (carets == null || carets.size == 1) {
    action()
  }
  else {
    var initialDocumentSize = this.document.textLength
    var documentSizeDifference = 0

    val caretOffsets = carets.map { it.selectionStart to it.selectionEnd }
    val restoredCarets = mutableListOf<CaretState>()

    caretModel.removeSecondaryCarets()
    
    for ((selectionStart, selectionEnd) in caretOffsets) {
      if (selectionStart == selectionEnd) {
        caretModel.primaryCaret.moveToOffset(selectionStart + documentSizeDifference)
      }
      else {
        caretModel.primaryCaret.setSelection(
          selectionStart + documentSizeDifference,
          selectionEnd + documentSizeDifference
        )
      }
      
      action()
      restoredCarets.add(caretModel.caretsAndSelections.single())

      val documentLength = this.document.textLength
      documentSizeDifference += documentLength - initialDocumentSize
      initialDocumentSize = documentLength
    }

    caretModel.caretsAndSelections = restoredCarets
  } 
}
