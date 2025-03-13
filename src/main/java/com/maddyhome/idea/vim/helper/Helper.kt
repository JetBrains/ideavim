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
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.util.Processor
import com.intellij.util.application
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.inBlockSelection

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

@Suppress("IncorrectParentDisposable")
internal fun Editor.isTemplateActive(): Boolean {
  val project = this.project ?: return false
  // XXX: I've disabled this check to find the stack trace where the project is disposed
//  if (project.isDisposed) return false
  if (TemplateManager.getInstance(project).getActiveTemplate(this) != null) {
    return true
  }

  return checkTemplateByHighlighter(project)
}

// Note: This approach is important for IDEs which use remote connection (Rider, CLion, RemDev, etc)
//  The highlighting happens on the backend, and IdeaVim works on the frontend. Thus, TemplateManager
//  is always empty. However, the highlighting itself also contains information about the LIVE TEMPLATE,
//  which we verify here.
private fun Editor.checkTemplateByHighlighter(project: Project): Boolean {
  return application.runReadAction(Computable {
    val offset = caretModel.primaryCaret.offset
    val editorMarkup = markupModel
    if (editorMarkup is MarkupModelEx && hasLiveTemplateHighlighter(editorMarkup, offset)) {
      return@Computable true
    }

    val documentMarkup = DocumentMarkupModel.forDocument(document, project, true)
    documentMarkup is MarkupModelEx && hasLiveTemplateHighlighter(documentMarkup, offset)
  })
}

private fun hasLiveTemplateHighlighter(
  markup: MarkupModelEx,
  offset: Int,
): Boolean {
  var found = false;
  markup.processRangeHighlightersOverlappingWith(
    offset, offset,
    Processor {
      found = it.textAttributesKey == EditorColors.LIVE_TEMPLATE_ATTRIBUTES
      !found
    })

  return found
}

private fun vimEnabled(editor: Editor?): Boolean {
  if (VimPlugin.isNotEnabled()) return false
  if (editor != null && editor.isIdeaVimDisabledHere) return false
  return true
}

internal fun vimDisabled(editor: Editor?): Boolean = !vimEnabled(editor)
