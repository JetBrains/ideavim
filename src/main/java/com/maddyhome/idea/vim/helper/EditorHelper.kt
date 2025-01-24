/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("EditorHelperRt")

package com.maddyhome.idea.vim.helper

import com.intellij.codeWithMe.ClientId
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.util.ui.table.JBTableRowEditor
import com.maddyhome.idea.vim.api.StringListOptionValue
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.key.IdeaVimDisablerExtensionPoint
import com.maddyhome.idea.vim.newapi.globalIjOptions
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTable

@Deprecated("Use fileSize from VimEditor")
val Editor.fileSize: Int
  get() = document.textLength

/**
 * There is a problem with one-line editors. At the moment of the editor creation, this property is always set to false.
 *   So, we should enable IdeaVim for such editors and disable it on the first interaction
 */
internal val Editor.isIdeaVimDisabledHere: Boolean
  get() {
    val ideaVimSupportValue = injector.globalIjOptions().ideavimsupport
    return (ideaVimDisabledInDialog(ideaVimSupportValue) && isInDialog()) ||
      !ClientId.isCurrentlyUnderLocalId || // CWM-927
      (ideaVimDisabledForSingleLine(ideaVimSupportValue) && isSingleLine()) ||
      IdeaVimDisablerExtensionPoint.isDisabledForEditor(this)
  }

private fun ideaVimDisabledInDialog(ideaVimSupportValue: StringListOptionValue): Boolean {
  return !ideaVimSupportValue.contains(IjOptionConstants.ideavimsupport_dialog)
    && !ideaVimSupportValue.contains(IjOptionConstants.ideavimsupport_dialoglegacy)
}

private fun ideaVimDisabledForSingleLine(ideaVimSupportValue: StringListOptionValue): Boolean {
  return !ideaVimSupportValue.contains(IjOptionConstants.ideavimsupport_singleline)
}

private fun Editor.isInDialog(): Boolean {
  return !this.isPrimaryEditor() && !EditorHelper.isFileEditor(this)
}

private fun Editor.isSingleLine(): Boolean {
  return isTableCellEditor(this.component) || isOneLineMode
}

/**
 * Checks if the editor is a primary editor in the main editing area.
 */
internal fun Editor.isPrimaryEditor(): Boolean {
  val project = project ?: return false
  val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
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

/**
 * Get caret line in vim notation (1-based)
 */
internal val Caret.vimLine: Int
  get() = this.logicalPosition.line + 1

/**
 * Get current caret line in vim notation (1-based)
 */
internal val Editor.vimLine: Int
  get() = this.caretModel.currentCaret.vimLine

internal val DataContext.isNotEditorContextComponent: Boolean
  get() {
    val contextComponent = this.getData(PlatformDataKeys.CONTEXT_COMPONENT) ?: return true
    return contextComponent !is EditorComponentImpl
  }