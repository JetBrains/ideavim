/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.GlobalCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SubstituteCommand
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser

/**
 * 'incsearch' and 'inccommand' for an Ex command line. Only `:s` and `:g` are previewed, their pattern is the argument
 * of the command
 */
internal class ExCommandPreview(
  editor: Editor,
  private val isIncsearchEnabled: Boolean,
  private val isInccommandEnabled: Boolean,
) : CommandLinePreview {
  private val incsearch = IncsearchSession(editor)

  // Renders the 'inccommand' preview (live :substitute) into the buffer as the command line is edited.
  private val inccommandPreview = InccommandPreview()

  override val matchOffset: Int
    get() = incsearch.matchOffset

  override fun update(editor: Editor, text: String) {
    incsearch.restartFromFirstMatch()
    when (val update = parse(editor, text)) {
      is Update.Renderable -> {
        if (isIncsearchEnabled) showIncsearchHighlights(editor, update)
        updateInccommandPreview(editor, update)
      }

      Update.ClearPreview -> inccommandPreview.clearBufferPreview(editor)

      Update.ResetIncsearch -> {
        inccommandPreview.clearBufferPreview(editor)
        if (isIncsearchEnabled) incsearch.restoreLastSearch(editor)
      }
    }
  }

  override fun advanceMatch(editor: Editor, text: String, next: Boolean) {
    if (!isIncsearchEnabled) return
    val update = parse(editor, text) as? Update.Renderable ?: return
    incsearch.advanceMatch(next)
    showIncsearchHighlights(editor, update)
  }

  override fun close(editor: Editor?, resetCaret: Boolean) {
    // Revert any inccommand preview before anything else. On <CR> the command line is closed (and thus deactivated)
    // *before* the real command executes (see CommandKeyConsumer), so reverting here guarantees the real substitution
    // runs against the original, un-previewed document - and on <Esc> it simply restores the buffer.
    if (editor != null) inccommandPreview.clear(editor)
    if (isIncsearchEnabled) incsearch.close(editor, resetCaret)
  }

  /** What the current command-line text means for the preview */
  private sealed interface Update {
    /** A `:s` or `:g` command with a usable pattern */
    class Renderable(
      val separator: Char,
      val searchText: String,
      val searchRange: LineRange,
      val substituteCommand: SubstituteCommand?,
    ) : Update

    /** Nothing to render - an empty or non-previewable command line. */
    object ClearPreview : Update

    /** An in-progress ex command that lost its pattern/range; restore the previous incsearch highlights. */
    object ResetIncsearch : Update
  }

  private fun parse(editor: Editor, text: String): Update {
    if (text.isEmpty()) return Update.ClearPreview
    val command = parseIncsearchCommand(text) ?: return Update.ClearPreview

    // The argument is e.g. `/foo/bar/g` for `:%s/foo/bar/g`: the first char is the separator, the rest is the pattern
    // (plus replacement and flags). `%` is the range, `s` the command, `/` the argument separator.
    val argument = command.commandArgument
    // E.g. highlight `whatever`, type `:%s/foo` (now highlighting `foo`), then delete back to `:%s/`: restore
    // `whatever`. A null range means the user typed an invalid range (e.g. an unset mark).
    if (argument.length < 2) return Update.ResetIncsearch
    val searchRange = command.getLineRangeSafe(IjVimEditor(editor)) ?: return Update.ResetIncsearch
    return Update.Renderable(argument[0], argument.substring(1), searchRange, command as? SubstituteCommand)
  }

  private fun showIncsearchHighlights(editor: Editor, update: Update.Renderable) {
    incsearch.showExCommand(editor, update.searchText, update.separator, update.searchRange)
  }

  /** Render the inccommand preview. Called after highlighting, which must see the original (un-previewed) text. */
  private fun updateInccommandPreview(editor: Editor, update: Update.Renderable) {
    val command = update.substituteCommand
    // Only preview once a replacement delimiter has been typed (e.g. `:%s/foo/` rather than just `:%s/foo`).
    if (isInccommandEnabled && command != null && update.searchText.contains("/")) {
      val splitContent = inccommandPreview.apply(editor, command, update.searchRange)
      updateSplitPreviewWindow(editor, splitContent)
    } else {
      inccommandPreview.clearBufferPreview(editor)
    }
  }

  private fun updateSplitPreviewWindow(editor: Editor, content: String?) {
    if (injector.globalOptions().inccommand != "split" || content == null) return
    val vimEditor = IjVimEditor(editor)
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    openSubstitutePreview(context, content, vimEditor)
  }

  private fun openSubstitutePreview(
    context: ExecutionContext,
    content: String,
    vimEditor: IjVimEditor,
  ) {
    ApplicationManager.getApplication().invokeLater {
      val virtualBufferGroup = injector.virtualBufferGroup
      if (virtualBufferGroup.isOpen(context, VirtualBufferKind.SubstitutePreview)) {
        virtualBufferGroup.refresh(context, VirtualBufferKind.SubstitutePreview, content)
        return@invokeLater
      }
      virtualBufferGroup.open(
        context,
        vimEditor,
        VirtualBufferKind.SubstitutePreview,
        content,
        focus = false,
      )
    }
  }

  private fun parseIncsearchCommand(commandText: String): Command? {
    try {
      val exCommand = VimscriptParser.parseCommand(commandText)
      // TODO: Add smagic and snomagic here if/when the commands are supported
      if (exCommand is SubstituteCommand || exCommand is GlobalCommand) {
        return exCommand
      }
    } catch (e: Exception) {
      logger.error("Cannot parse command for incsearch", e)
    }
    return null
  }

  companion object {
    private val logger = Logger.getInstance(ExCommandPreview::class.java)
  }
}
