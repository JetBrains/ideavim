/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.client.ClientKind
import com.intellij.openapi.client.ClientSessionsManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.ClientEditorManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.inBlockSelection
import java.util.stream.Collectors
import java.util.stream.Stream

internal fun <T : Comparable<T>> sort(a: T, b: T) = if (a > b) b to a else a to b

// TODO Should be replaced with VimEditor.carets()
internal inline fun Editor.vimForEachCaret(action: (caret: Caret) -> Unit) {
  if (this.vim.inBlockSelection) {
    action(this.caretModel.primaryCaret)
  } else {
    this.caretModel.allCarets.forEach(action)
  }
}

internal fun Editor.getTopLevelEditor() = if (this is EditorWindow) this.delegate else this

/**
 * Return list of editors for local host (for code with me plugin)
 */
public fun localEditors(): List<Editor> = getLocalEditors()
  .collect(Collectors.toList())

public fun localEditors(doc: Document): List<Editor> = getLocalEditors()
  .filter { editor -> editor.document == doc }
  .collect(Collectors.toList())

public fun localEditors(doc: Document, project: Project): List<Editor> {
  return getLocalEditors()
    .filter { editor -> editor.document == doc && editor.project == project }
    .collect(Collectors.toList())
}

private fun getLocalEditors(): Stream<Editor> {
  // Always fetch local editors. If we're hosting a Code With Me session, any connected guests will create hidden
  // editors to handle syntax highlighting, completion requests, etc. We need to make sure that IdeaVim only makes
  // changes (e.g. adding search highlights) to local editors, so things don't incorrectly flow through to any Clients.
  // In non-CWM scenarios, or if IdeaVim is installed on the Client, there are only ever local editors, so this will
  // also work there. In Gateway remote development scenarios, IdeaVim should not be installed on the host, only the
  // Client, so all should work there too.
  // Note that most IdeaVim operations are in response to interactive keystrokes, which would mean that
  // ClientEditorManager.getCurrentInstance would return local editors. However, some operations are in response to
  // events such as document change (to update search highlights) and these can come from CWM guests, and we'd get the
  // remote editors.
  // This invocation will always get local editors, regardless of current context.
  val localSession = ClientSessionsManager.getAppSessions(ClientKind.LOCAL).single()
  return localSession.service<ClientEditorManager>().editors()
}

@Suppress("IncorrectParentDisposable")
internal fun Editor.isTemplateActive(): Boolean {
  val project = this.project ?: return false
  // XXX: I've disabled this check to find the stack trace where the project is disposed
//  if (project.isDisposed) return false
  return TemplateManager.getInstance(project).getActiveTemplate(this) != null
}

private fun vimEnabled(editor: Editor?): Boolean {
  if (VimPlugin.isNotEnabled()) return false
  if (editor != null && editor.isIdeaVimDisabledHere) return false
  return true
}

internal fun vimDisabled(editor: Editor?): Boolean = !vimEnabled(editor)
