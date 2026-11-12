/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.register

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.selectionType

/**
 * `autoselect` in 'clipboard': the visual selection goes to PRIMARY.
 *
 * The IDE puts every selection into PRIMARY on its own, as untyped text (and turns that off without `autoselect`).
 * IdeaVim publishes the visual selection again with its type, after the IDE, because the write of the IDE reaches
 * readers late on Wayland (VIM-4184). The text is the one the IDE publishes, so whichever comes last, PRIMARY holds the
 * text of the selection and its type is recognised from it.
 */
internal object PrimaryAutoselect {
  /** The selection to publish now, or null if there is none to publish. */
  fun selectionToPublish(editor: VimEditor): Pair<String, SelectionType>? {
    if (!editor.inVisualMode) return null
    if (OptionConstants.clipboard_autoselect !in injector.globalOptions().clipboard) return null
    // The visual selection is set transiently by operators (yy, dd, c…), text objects and put with a selection too,
    // which must not reach PRIMARY
    if (injector.vimState.executingCommand?.type in OPERATORS_SKIPPING_PRIMARY_PUSH) return null
    val selectionType = editor.mode.selectionType ?: SelectionType.CHARACTER_WISE
    val text = selectedText(editor).takeIf { it.isNotEmpty() } ?: return null
    return ensureLinewiseTrailingNewline(text, selectionType) to selectionType
  }

  /**
   * The selection of every caret joined by newlines, a caret without selection adding an empty line, which is how the
   * IDE puts a selection into PRIMARY (`SelectionModel.getSelectedText(true)`). A block selection is one caret per line.
   */
  private fun selectedText(editor: VimEditor): String {
    val text = editor.text()
    return editor.nativeCarets().joinToString("\n") { caret ->
      if (caret.hasSelection()) text.subSequence(caret.selectionStart, caret.selectionEnd) else ""
    }
  }

  // A linewise selection ends at the trailing `\n` rather than after it, so the selected text excludes the newline.
  // Re-add it so that readers of PRIMARY see linewise content.
  private fun ensureLinewiseTrailingNewline(text: String, selectionType: SelectionType): String =
    if (selectionType == SelectionType.LINE_WISE && !text.endsWith("\n")) "$text\n" else text

  private val OPERATORS_SKIPPING_PRIMARY_PUSH = setOf(
    Command.Type.COPY,
    Command.Type.DELETE,
    Command.Type.CHANGE,
    Command.Type.PASTE,
    Command.Type.INSERT,
  )
}
