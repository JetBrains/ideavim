/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.key.findAbbreviationLhsRange
import com.maddyhome.idea.vim.key.isAbbreviationKeywordChar
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

interface VimCommandLine {
  val inputProcessing: ((String) -> Unit)?
  val finishOn: Char?

  val editor: VimEditor
  val caret: VimCommandLineCaret

  fun getLabel(): String
  val isReplaceMode: Boolean

  var lastEntry: String?
  val historyType: VimHistory.Type
    get() = VimHistory.Type.getTypeByLabel(getLabel())

  fun toggleReplaceMode()

  /** Active completion session, or null if no completion is in progress */
  var activeCompletion: CommandLineCompletion?

  /** Called when a new completion session starts and candidates should be displayed */
  fun showCompletionBar(completion: CommandLineCompletion) {}

  /** Called when the selected completion item changes */
  fun selectCompletionItem(selectedIndex: Int?) {}

  /** Called when the completion bar should be hidden */
  fun hideCompletionBar() {}

  fun isExCommand(): Boolean

  /**
   * The entered text. It does not include any rendered text such as `<80>` or prompts such as `^` or `?`
   */
  val text: String

  /**
   * @deprecated Use `text` instead
   *
   * It's now used in the com.github.dankinsoid.multicursor plugin
   */
  @Suppress("unused")
  @Deprecated("Use `text` instead", ReplaceWith("text"))
  val actualText: String
    get() = text

  /**
   * Get the text as it is rendered in the command line.
   *
   * This includes control characters rendered in Vim style, e.g. `<80>` or `^[` and prompts such as `^` or `?`
   */
  @TestOnly
  fun getRenderedText(): String

  /**
   * Replaces the current text with the new string
   *
   * Note that this will reset the scroll position of the text field. If the text is being edited, it is better to use
   * [insertText] or [deleteText].
   */
  fun setText(string: String, updateLastEntry: Boolean = true)

  /**
   * Insert the new string into the text at the given offset, maintaining the text field's current scroll position
   *
   * This will always save the updated text as the last entry in the command line's history.
   */
  fun insertText(offset: Int, string: String)

  /**
   * Delete the text at the given offset, maintaining the text field's current scroll position
   *
   * This will always save the updated text as the last entry in the command line's history.
   */
  fun deleteText(offset: Int, length: Int)

  /**
   * Called by the [KeyHandler] to finish handling the keystroke
   *
   * All keystrokes received by the command line are first routed through the key handler to allow for mapping and
   * commands. If a keystroke is not consumed as part of a mapping or command, it is returned to the command line for
   * further processing. If it is mapped to a new keystroke, the new keystroke is passed instead. Typically, commands
   * exist for cursor movements (`<Left>` and `<Right>`) as well as for shortcuts for Vim actions (`<Up>`, `<Down>`,
   * `<C-U>`, etc.). Typed characters are usually not mapped and passed back to the command line component, where they
   * are added to the text content.
   */
  fun handleKey(key: KeyStroke)

  /**
   * Text to show while composing a digraph or inserting a literal or register
   *
   * The prompt character is inserted directly into the text of the text field, rather than drawn over the top of the
   * current character. When the action has been completed, the new character(s) are either inserted or overwritten,
   * depending on the insert/overwrite status of the text field. This mimics Vim's behaviour.
   *
   * @param promptCharacter The character to show as prompt
   */
  fun setPromptCharacter(promptCharacter: Char)
  fun clearPromptCharacter()

  fun clearCurrentAction()

  /**
   * TODO remove me, close is safer
   */
  fun deactivate(refocusOwningEditor: Boolean, resetCaret: Boolean)

  fun close(refocusOwningEditor: Boolean, resetCaret: Boolean) {
    // If 'cpoptions' contains 'x', then Escape should execute the command line. This is the default for Vi but not Vim.
    // IdeaVim does not (currently?) support 'cpoptions', so sticks with Vim's default behaviour. Escape cancels.
    editor.mode = editor.mode.returnTo
    KeyHandler.getInstance().keyHandlerState.leaveCommandLine()
    deactivate(refocusOwningEditor, resetCaret)
  }

  /**
   * Set by the cmdline impl when a pure cursor move (caret moved without the text length changing)
   * has happened during the current cmdline session. Suppresses [tryExpandAbbreviation] until the
   * cmdline is re-activated. Mirrors Vim's `arrow_used` for the cmdline.
   */
  val isAbbreviationInvalidated: Boolean
    get() = false

  /**
   * If [trigger] is a non-keyword char and the text before the caret matches a `:cabbrev`-style
   * abbreviation, replace the matched lhs in the cmdline buffer with the abbreviation's rhs.
   */
  fun tryExpandAbbreviation(trigger: Char) {
    if (isAbbreviationKeywordChar(trigger)) return
    if (isAbbreviationInvalidated) return
    val lhsRange = findAbbreviationLhsRange(text, caret.offset, lineStart = 0) ?: return
    val lhs = text.substring(lhsRange.startOffset, lhsRange.endOffset)
    val entry = injector.abbreviationGroup.getAbbreviation(lhs, MappingMode.CMD_LINE, editor) ?: return
    deleteText(lhsRange.startOffset, lhs.length)
    insertText(lhsRange.startOffset, entry.rhs)
  }

  // FIXME I don't want it to conflict with Swings `requestFocus` and can suggest a better name
  fun focus()

  fun selectNewerHistory(filter: Boolean) {
    val previousCurrent = injector.historyGroup.getCurrentEntry(historyType)
    val newCurrent = injector.historyGroup.selectNewerEntry(historyType, if (filter) lastEntry else null)
    if (newCurrent == null) {
      selectHistory(lastEntry)
      if (previousCurrent == null) {
        injector.messages.indicateError()
      }
      return
    }
    selectHistory(newCurrent.entry)
  }

  fun selectOlderHistory(filter: Boolean) {
    val newCurrent = injector.historyGroup.selectOlderEntry(historyType, if (filter) lastEntry else null)
    if (newCurrent == null) {
      injector.messages.indicateError()
      return
    }
    selectHistory(newCurrent.entry)
  }

  private fun selectHistory(entry: String?) {
    if (entry != null) {
      setText(entry, updateLastEntry = false)
      caret.offset = entry.length
    }
  }
}
