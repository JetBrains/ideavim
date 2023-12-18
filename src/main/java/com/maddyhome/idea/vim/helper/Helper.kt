/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeWithMe.ClientId
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.ClientEditorManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.inBlockSelection
import java.util.stream.Collectors

/**
 * This annotation is created for test functions (methods).
 * It means that the original vim behavior has small differences from behavior of IdeaVim.
 * [shouldBeFixed] flag indicates whether the given functionality should be fixed
 *   or the given behavior is normal for IdeaVim and should be leaved as is.
 *
 * E.g. after execution of some commands original vim has the following text:
 *    Hello1
 *    Hello2
 *    Hello3
 *
 * But IdeaVim gives you:
 *    Hello1
 *
 *    Hello2
 *    Hello3
 *
 * In this case you should still create the test function and mark this function with [VimBehaviorDiffers] annotation.
 *
 * Why does this annotation exist?
 * After creating some functionality you can understand that IdeaVim has a bit different behavior, but you
 *   cannot fix it right now because of any reason (bugs in IDE,
 *   the impossibility of this functionality in IDEA (*[shouldBeFixed] == false*), leak of time for fixing).
 *   In that case, you should NOT remove the corresponding test or leave it without any marks that this test
 *   not fully convenient with vim, but leave the test with IdeaVim's behavior and put this annotation
 *   with description of how original vim works.
 *
 * Note that using this annotation should be avoided as much as possible and behavior of IdeaVim should be as close
 *   to vim as possible.
 */
@Target(AnnotationTarget.FUNCTION)
internal annotation class VimBehaviorDiffers(
  val originalVimAfter: String = "",
  val description: String = "",
  val shouldBeFixed: Boolean = true,
)

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
public fun localEditors(): List<Editor> {
  return ClientEditorManager.getCurrentInstance().editors().collect(Collectors.toList())
}

public fun localEditors(doc: Document): List<Editor> {
  return EditorFactory.getInstance().getEditors(doc)
    .filter { editor -> editor.editorClientId.let { it == null || it == ClientId.currentOrNull } }
}

public fun localEditors(doc: Document, project: Project): List<Editor> {
  return EditorFactory.getInstance().getEditors(doc, project)
    .filter { editor -> editor.editorClientId.let { it == null || it == ClientId.currentOrNull } }
}

private val Editor.editorClientId: ClientId?
  get() {
    if (editorClientKey == null) {
      @Suppress("DEPRECATION")
      editorClientKey = Key.findKeyByName("editorClientIdby userData()") ?: return null
    }
    return editorClientKey?.let { this.getUserData(it) as? ClientId }
  }

private var editorClientKey: Key<*>? = null

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
