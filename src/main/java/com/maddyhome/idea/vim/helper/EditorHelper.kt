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
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
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
      IdeaVimDisablerExtensionPoint.isDisabledForEditor(this) ||
      isNotFileEditorExceptCommit()
  }

/**
 * Almost every non-file-based editor should not use Vim mode. These editors are debug watch, Python console, AI chats,
 *   and other fields that are smart.
 *
 * We may support IdeaVim in these editors, but this will require a focused work and a lot of testing.
 *
 * Here are issues when non-file editors were supported:
 * AI Chat – VIM-3786
 * Debug evaluate console – VIM-3929
 *
 * However, we still support IdeaVim in a commit window because it works fine there, and removing vim from this place will
 *   be quite a visible change for users.
 * We detect the commit window by the name of the editor (Dummy.txt). If this causes issues, let's disable IdeaVim
 *   in the commit window as well.
 */
private fun Editor.isNotFileEditorExceptCommit(): Boolean {
  if (EditorHelper.getVirtualFile(this)?.name?.contains("Dummy.txt") == true) return false
  return !EditorHelper.isFileEditor(this)
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

/**
 * Checks if the editor should be treated like a terminal. I.e. switch to Insert mode automatically
 *
 * A "terminal" editor is an editor used for purposes other than mainstream editing, such as a terminal, console, log
 * viewer, etc. In this instance, the editor is writable, the document is writable, but it's not backed by a real file
 * and it's not the diff viewer. We also check that if it's an injected language fragment backed by a real file.
 */
internal fun Editor.isTerminalEditor(): Boolean {
  return !isViewer
    && document.isWritable
    && !EditorHelper.isFileEditor(this)
    && !EditorHelper.isDiffEditor(this)
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
