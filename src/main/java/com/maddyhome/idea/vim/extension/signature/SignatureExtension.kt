/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.signature

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.vim.api.VimInitApi
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.VimMarkListener
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.newapi.IjVimEditor
import javax.swing.Icon

internal const val PLUGIN_NAME: String = "signature"

/**
 * Gutter signs for the local marks of a file - a port of kshenoy/vim-signature (VIM-1347).
 *
 * Global marks `A`-`Z` are left to the IDE: with the default `ideamarks` they are IDE bookmarks, which the platform
 * already draws in the gutter itself. Local marks `a`-`z` have no visual at all, and that is what this adds.
 */
internal class SignatureExtension : VimExtension, VimMarkListener {

  override fun getName(): String = PLUGIN_NAME

  override fun init(initApi: VimInitApi) {
    injector.listenersNotifier.markListeners.add(this)
    marksChanged(null)
  }

  override fun dispose() {
    injector.listenersNotifier.markListeners.remove(this)
    injector.editorGroup.getEditors().forEach { removeAllSigns((it as IjVimEditor).editor) }
  }

  /**
   * Redraws the signs of every open editor.
   *
   * Only `a`-`z` are drawn, so a change to any other mark - and IdeaVim sets `.`, `^`, `'`, `[` and `]` on almost every
   * edit and jump - can be dropped without doing the work. [markChar] is null when the caller does not know which marks
   * changed, and then there is no choice but to redraw.
   */
  override fun marksChanged(markChar: Char?) {
    if (markChar != null && markChar !in VimMarkService.LOWERCASE_MARKS) return

    // The markup model is not ours to touch from a background thread, and setMark is reachable from one.
    runOnEdt {
      injector.editorGroup.getEditors().forEach { e ->
        val wanted =
          injector.markService.getAllLocalMarks(e.primaryCaret()).filter { it.key in VimMarkService.LOWERCASE_MARKS }
        val ijEditor = (e as IjVimEditor).editor
        val existingMarks = ijEditor.signHighlighters()

        removeMarks(existingMarks, ijEditor, wanted)
        addMarks(wanted, existingMarks, ijEditor)
      }
    }
  }
}

private fun runOnEdt(action: () -> Unit) {
  val application = ApplicationManager.getApplication()
  if (application.isDispatchThread) action() else application.invokeLater(action)
}

private fun Editor.signHighlighters(): List<RangeHighlighter> =
  markupModel.allHighlighters.filter { it.gutterIconRenderer is MarkupGutterIconRenderer }

private fun removeAllSigns(ijEditor: Editor) {
  ijEditor.signHighlighters().forEach { ijEditor.markupModel.removeHighlighter(it) }
}

private fun addMarks(
  wanted: List<Mark>,
  existingMarks: List<RangeHighlighter>,
  ijEditor: Editor,
) {
  wanted.forEach { mark ->
    // Marks are stored per file path with a plain line number and outlive the editor, so a file that has shrunk since
    // the mark was set - or since the last session, as local marks are persisted - can carry a mark past its end.
    // addLineHighlighter throws IndexOutOfBoundsException for such a line.
    if (mark.line >= ijEditor.document.lineCount || mark.line < 0) return@forEach
    if (!isMarkAlreadyPresent(mark, existingMarks, ijEditor)) {
      val highlighter = ijEditor.markupModel.addLineHighlighter(null, mark.line, HighlighterLayer.ADDITIONAL_SYNTAX)
      highlighter.gutterIconRenderer = MarkupGutterIconRenderer(mark.key)
    }
  }
}

private fun removeMarks(
  existingMarks: List<RangeHighlighter>,
  ijEditor: Editor,
  wanted: List<Mark>,
) {
  existingMarks.forEach { highlighter ->
    if (!isHighlighterStillNeeded(highlighter, ijEditor, wanted)) {
      ijEditor.markupModel.removeHighlighter(highlighter)
    }
  }
}

private fun isMarkAlreadyPresent(mark: Mark, existingMarks: List<RangeHighlighter>, ijEditor: Editor): Boolean {
  return existingMarks.any { it.isRepresentingMark(mark, ijEditor) }
}

private fun isHighlighterStillNeeded(highlighter: RangeHighlighter, ijEditor: Editor, wanted: List<Mark>): Boolean {
  return wanted.any { highlighter.isRepresentingMark(it, ijEditor) }
}

private fun RangeHighlighter.isRepresentingMark(mark: Mark, ijEditor: Editor): Boolean {
  val markChar = (gutterIconRenderer as MarkupGutterIconRenderer).mark
  if (mark.key != markChar) return false
  if (!isValid) return false
  return mark.line == ijEditor.document.getLineNumber(startOffset)
}

private class MarkupGutterIconRenderer(val mark: Char) : GutterIconRenderer() {
  override fun getIcon(): Icon = AllIcons.Gutter.Bookmark
  override fun getTooltipText(): String = mark.toString()
  override fun equals(obj: Any?): Boolean {
    return obj is MarkupGutterIconRenderer && mark == obj.mark
  }

  override fun hashCode(): Int {
    return mark.hashCode()
  }
}
