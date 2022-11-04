/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("EditorHelperRt")

package com.maddyhome.idea.vim.helper

import com.intellij.codeWithMe.ClientId
import com.intellij.openapi.editor.Caret
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
