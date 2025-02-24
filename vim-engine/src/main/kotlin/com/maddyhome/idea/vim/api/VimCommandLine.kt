/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.history.HistoryEntry
import com.maddyhome.idea.vim.history.VimHistory
import javax.swing.KeyStroke
import kotlin.math.min

/**
 * This interface is not supposed to have any implementation logic.
 * The reason why we have implementation details here is
 * that this class extended by [ExEntryPanel] that already extends [JPanel]
 * and can't extend a base implementation of [VimCommandLine].
 * It will also be hard to wrap [ExEntryPanel] into some other class that extends [VimCommandLine],
 * because [ExEntryPanel] has a listener that should use the [actualText] field, so it must implement [VimCommandLine]
 */
interface VimCommandLine {
  companion object {
    private val logger = vimLogger<VimCommandLine>()
  }

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
   * The actual text present in the command line, excluding special characters like the `?` displayed during digraph input.
   * This text represents the real content that is being processed or executed.
   */
  val actualText: String
    get() {
      val promptCharacterOffset1 = promptCharacterOffset
      return if (promptCharacterOffset1 == null) visibleText else {
        if (promptCharacterOffset1 > visibleText.length) {
          logger.error("promptCharacterOffset1 >= visibleText.length: $promptCharacterOffset1 >= ${visibleText.length}")
          visibleText
        } else {
          visibleText.removeRange(promptCharacterOffset1, promptCharacterOffset1 + 1)
        }
      }
    }

  /**
   * The text content displayed in the command line, including any additional characters or symbols
   * that might be shown to the user, such as the `?` during digraph input.
   * This is the text that the user sees on the screen.
   */
  val visibleText: String
  var promptCharacterOffset: Int?

  fun setText(string: String, updateLastEntry: Boolean = true)
  fun insertText(offset: Int, string: String) {
    val newText = if (isReplaceMode) {
      val endOffset = min(offset + string.length, actualText.length)
      StringBuilder(actualText).replace(offset, endOffset, string)
    } else {
      StringBuilder(actualText).insert(offset, string)
    }.toString()
    setText(newText)
  }

  fun handleKey(key: KeyStroke)

  /**
   * Text to show while composing a digraph or inserting a literal or register
   * <p>
   * The prompt character is inserted directly into the text of the text field, rather than drawn over the top of the
   * current character. When the action has been completed, the new character(s) are either inserted or overwritten,
   * depending on the insert/overwrite status of the text field. This mimics Vim's behaviour.
   *
   * @param promptCharacter The character to show as prompt
   */
  fun setPromptCharacter(char: Char) {
    val stringBuilder = StringBuilder(actualText)

    val offset =
      promptCharacterOffset ?: caret.offset // TODO is there a case where caret is not at the [promptCharacterOffset]?
    promptCharacterOffset = offset
    stringBuilder.insert(offset, char)
    setText(stringBuilder.toString())

    caret.offset = offset
  }

  fun clearPromptCharacter() {
    if (promptCharacterOffset == null) return

    // Note: We have to set promptCharacterOffset to null first, because when we set the new text,
    //   the listener will be called, which will try to get the actual text again. And, if this field isn't null,
    //   it will get an incorrect result.
    promptCharacterOffset = null
    setText(actualText)
    caret.offset = min(caret.offset, visibleText.length)
  }

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
