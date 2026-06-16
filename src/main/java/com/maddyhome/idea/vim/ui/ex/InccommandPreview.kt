/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.SubstitutePreviewChange
import com.maddyhome.idea.vim.api.VimSearchGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.helper.highlightPreviewMatch
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.commands.SubstituteCommand

/**
 * Renders Vim's 'inccommand' preview: a live, throwaway preview of a `:substitute` applied directly to the document as
 * the user types the command line. The preview is purely visual - it is always fully reverted before the real command
 * runs (on `<CR>`) or the command line is cancelled (on `<Esc>`), so it never commits a change itself.
 *
 * The preview is applied incrementally: only the matched ranges are touched, and reverting/re-applying happen inside a
 * single undo-transparent write action so the editor repaints once (only the net difference is visible - no flicker)
 * and the throwaway edits never pollute the user's undo stack.
 *
 * One instance is held per command-line panel. It is not thread-safe; all calls must happen on the EDT.
 */
internal class InccommandPreview {
  /** The replacements currently applied to the document as a preview, in the order they were made. */
  private var changes: List<SubstitutePreviewChange> = emptyList()

  /** A copy-free snapshot of the document taken before the preview was applied, used only as a revert fallback. */
  private var snapshot: CharSequence? = null

  /** Highlighters drawn over the replacement text so the user can see what changed. Removed alongside [changes]. */
  private var highlighters: List<RangeHighlighter> = emptyList()

  /**
   * Render a preview of [command] over [range], replacing whatever the previous preview showed.
   */
  fun apply(editor: Editor, command: SubstituteCommand, range: LineRange) {
    val vimEditor = IjVimEditor(editor)
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    CommandProcessor.getInstance().runUndoTransparentAction {
      ApplicationManager.getApplication().runWriteAction {
        revert(editor)
        snapshot = editor.document.immutableCharSequence
        // A command parsed purely for preview has no vimContext; give it a root so `\=expr` replacements can evaluate.
        command.vimContext = Script()
        changes = (VimPlugin.getSearch() as VimSearchGroupBase)
          .substitutePreview(vimEditor, context, range, command.command, command.argument, command)
        // Highlight the replacement text in place so the change is visible, like Vim/Neovim's inccommand preview.
        highlighters = changes.map { change ->
          val end = change.startOffset + change.replacementLength
          highlightPreviewMatch(editor, change.startOffset, end, change.originalText)
        }
      }
    }
  }

  /** Revert any active preview, restoring the document to its original state. */
  fun clear(editor: Editor) {
    if (changes.isEmpty() && snapshot == null && highlighters.isEmpty()) return
    CommandProcessor.getInstance().runUndoTransparentAction {
      ApplicationManager.getApplication().runWriteAction {
        revert(editor)
      }
    }
  }

  /**
   * Drop any tracked preview state *without* reverting it.
   *
   * Used when a command-line panel is (re)activated: the panel is a reused singleton, so it may still hold preview
   * state from a previous activation that was abandoned without a clean [clear] (e.g. the editor was disposed). Those
   * offsets refer to a now-stale document and must not be replayed, so we simply forget them.
   */
  fun reset() {
    changes = emptyList()
    snapshot = null
    highlighters = emptyList()
  }

  /**
   * Undo the tracked preview replacements, restoring the original text. Must be called inside a write action.
   *
   * Replacements are reverted in descending offset order so that reverting a later change (which changes the document
   * length) never invalidates the stored offset of an earlier one.
   */
  private fun revert(editor: Editor) {
    val changes = this.changes
    val snapshot = this.snapshot
    val highlighters = this.highlighters
    this.changes = emptyList()
    this.snapshot = null
    this.highlighters = emptyList()

    highlighters.forEach { editor.markupModel.removeHighlighter(it) }
    if (changes.isEmpty()) return

    val document = editor.document
    try {
      for (change in changes.sortedByDescending { it.startOffset }) {
        document.replaceString(change.startOffset, change.startOffset + change.replacementLength, change.originalText)
      }
    } catch (e: Throwable) {
      // Defensive fallback: the tracked offsets somehow diverged from the document. Restore the snapshot wholesale.
      if (snapshot != null) document.setText(snapshot)
      logger.warn("inccommand preview: incremental revert failed; restored snapshot", e)
    }
  }

  private companion object {
    private val logger = Logger.getInstance(InccommandPreview::class.java)
  }
}
