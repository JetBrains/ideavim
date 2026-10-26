/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.CommandLineType
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector

/**
 * The live preview in the editor while the command line is edited, e.g. 'incsearch' highlights or the 'inccommand'
 * substitution
 *
 * [ExEntryPanel] creates one for each activation from the type of the command line and forwards the edits to it.
 */
internal interface CommandLinePreview {
  /** How many matches `c_CTRL-G`/`c_CTRL-T` moved the current match past the first one */
  val matchOffset: Int
    get() = 0

  /** Called after every edit of the command line [text] */
  fun update(editor: Editor, text: String) {}

  /** `c_CTRL-G` ([next] == true) / `c_CTRL-T` ([next] == false) - move the current match for the command line [text] */
  fun advanceMatch(editor: Editor, text: String, next: Boolean) {}

  /**
   * Called when the command line is closed, before the entered command runs
   *
   * @param editor      The editor of the command line, or null if it was disposed
   * @param resetCaret  Whether the caret should go back to where it was when the command line was opened
   */
  fun close(editor: Editor?, resetCaret: Boolean) {}

  companion object {
    /**
     * Creates the preview for a command line of the given [type] in [editor]
     *
     * 'incsearch' and 'inccommand' are read once here, they don't change while the command line is open.
     */
    fun create(type: CommandLineType, editor: Editor): CommandLinePreview {
      val isIncsearchEnabled = injector.globalOptions().incsearch
      // 'inccommand' is a string option: "" (off), "nosplit" or "split". We currently render both non-empty values as
      // an in-buffer preview (the "split" preview window is not yet implemented).
      val isInccommandEnabled = injector.globalOptions().inccommand.isNotEmpty()
      return when (type) {
        is CommandLineType.Search -> if (isIncsearchEnabled) SearchPreview(editor, type) else NoPreview
        CommandLineType.Command -> if (isIncsearchEnabled || isInccommandEnabled) {
          ExCommandPreview(editor, isIncsearchEnabled, isInccommandEnabled)
        } else {
          NoPreview
        }
        // The text of an input prompt (e.g. `input()`, the surround tag prompt or `<C-R>=`) is not a pattern
        CommandLineType.Expression, CommandLineType.Input -> NoPreview
      }
    }
  }
}

/** A command line without a preview */
internal object NoPreview : CommandLinePreview
