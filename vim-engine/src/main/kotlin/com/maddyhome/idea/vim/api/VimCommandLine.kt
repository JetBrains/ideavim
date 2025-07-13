/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.history.HistoryEntry
import com.maddyhome.idea.vim.history.VimHistory
import org.jetbrains.annotations.TestOnly
import com.maddyhome.idea.vim.key.VimKeyStroke

/**
 * This interface is not supposed to have any implementation logic.
 * The reason why we have implementation details here is
 * that this class extended by [ExEntryPanel] that already extends [JPanel]
 * and can't extend a base implementation of [VimCommandLine].
 * TODO: Consider creating a derived instance that has-a instance of ExEntryPanel
 */
interface VimCommandLine {
  val inputProcessing: ((String) -> Unit)?
  val finishOn: Char?

  val editor: VimEditor
  val caret: VimCommandLineCaret

  val label: String
  val isReplaceMode: Boolean

  var histIndex: Int
  var lastEntry: String
  val historyType: VimHistory.Type
    get() = VimHistory.Type.getTypeByLabel(label)

  fun toggleReplaceMode()

  /**
   * The entered text. It does not include any rendered text such as `<80>` or prompts such as `^` or `?`
   */
  val text: String

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
   * `<C-U>`, etc.). Typed characters are usually not mapped, and passed back to the command line component, where they
   * are added to the text content.
   */
  fun handleKey(key: VimKeyStroke)

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

  // FIXME I don't want it to conflict with Swings `requestFocus` and can suggest a better name
  fun focus()

  fun selectHistory(isUp: Boolean, filter: Boolean) {
    val history = injector.historyGroup.getEntries(historyType, 0, 0)

    val dir = if (isUp) -1 else 1
    if (histIndex + dir < 0 || histIndex + dir > history.size) {
      injector.messages.indicateError()

      return
    }

    if (filter) {
      var i: Int = histIndex + dir
      while (i >= 0 && i <= history.size) {
        var txt: String
        if (i == history.size) {
          txt = lastEntry
        } else {
          val entry: HistoryEntry = history[i]
          txt = entry.entry
        }

        if (txt.startsWith(lastEntry)) {
          setText(txt, updateLastEntry = false)
          caret.offset = txt.length
          histIndex = i

          return
        }
        i += dir
      }

      injector.messages.indicateError()
    } else {
      histIndex += dir
      val txt: String
      if (histIndex == history.size) {
        txt = lastEntry
      } else {
        val entry: HistoryEntry = history[histIndex]
        txt = entry.entry
      }

      setText(txt, updateLastEntry = false)
      caret.offset = txt.length
    }
  }
}
