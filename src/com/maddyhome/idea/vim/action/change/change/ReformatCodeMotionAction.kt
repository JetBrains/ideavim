/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.action.change.change

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.option.OptionsManager.textwidth
import java.util.*

class ReformatCodeMotionAction : ChangeEditorActionHandler.ForEachCaret(), DuplicableOperatorAction {
  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = 'q'

  override fun execute(editor: Editor,
                       caret: Caret,
                       context: DataContext,
                       count: Int,
                       rawCount: Int,
                       argument: Argument?): Boolean {
    return argument != null &&
      reformatCodeMotion(editor, caret, context, count, rawCount, argument)
  }

  private fun reformatCodeMotion(editor: Editor,
                                 caret: Caret,
                                 context: DataContext?,
                                 count: Int,
                                 rawCount: Int,
                                 argument: Argument): Boolean {
    val range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument)
    val action = argument.motion.action
    return if (range != null && action.id == "VimMotionOuterParagraphAction" && action.flags.contains(CommandFlags.FLAG_TEXT_BLOCK)) {
      reformatParagraphResetCursor(editor, caret, range)
    } else {
      range != null && VimPlugin.getChange().reformatCodeRange(editor, caret, range)
    }
  }

  private fun reformatParagraphResetCursor(editor: Editor,
                                           caret: Caret,
                                           range: TextRange): Boolean {
    val firstLine = editor.offsetToLogicalPosition(range.startOffset).line
    reformatParagraph(editor, caret, range)
    val newOffset = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, firstLine)
    MotionGroup.moveCaret(editor, caret, newOffset)
    return true
  }

  data class ReformatParagraphResult(val text: String, val newCursorOffset: Int)

  companion object {
    fun reformatParagraph(editor: Editor, caret: Caret, range: TextRange): Int {
      val startOffset = range.startOffset
      val endOffset = range.endOffset
      val textRange = com.intellij.openapi.util.TextRange.create(startOffset, endOffset)
      val text = editor.document.getText(textRange)
      val oldCaretEditorOffset = caret.offset
      val oldCaretParagraphOffset = oldCaretEditorOffset - startOffset

      val formatResult: ReformatParagraphResult = reformatTextAsParagraph(text, oldCaretParagraphOffset)
      editor.document.replaceString(startOffset, endOffset, formatResult.text)

      return startOffset + formatResult.newCursorOffset
    }

    fun reformatTextAsParagraph(inputText: String, oldCaretOffset: Int): ReformatParagraphResult {
      val textWidth = textwidth.value()
      val chars = inputText.toCharArray()
      val firstNonWhitespaceCharIndex: Int = getFirstNonWhitespaceCharIndex(chars)
      var newCaretOffset = 0
      if (oldCaretOffset < firstNonWhitespaceCharIndex) {
        newCaretOffset = oldCaretOffset
      }
      //Only whitespace, no need to do any reformatting
      if (firstNonWhitespaceCharIndex == inputText.length) {
        return ReformatParagraphResult(inputText, oldCaretOffset)
      }
      val builder = StringBuilder()
      val trailingWhitespaceStart: Int = getTrailingWhitespaceStart(chars)

      // track the number of characters added so we can calculate the new offset of the cursor
      var numberOfAddedCharacters = 0
      if (firstNonWhitespaceCharIndex > 0) {
        builder.append(inputText, 0, firstNonWhitespaceCharIndex)
        numberOfAddedCharacters += firstNonWhitespaceCharIndex - 1
      }
      var charactersInLine = 0
      var i: Int
      val currentToken = ArrayList<Char>()
      i = firstNonWhitespaceCharIndex
      while (i < trailingWhitespaceStart) {
        if (Character.isWhitespace(chars[i])) {
          if (!currentToken.isEmpty()) {
            if (charactersInLine + currentToken.size > textWidth) {
              builder.append("\n")
              addTokenToBuilder(builder, currentToken)
              numberOfAddedCharacters++
              charactersInLine = currentToken.size
            } else {
              if (charactersInLine != 0) {
                builder.append(" ")
                numberOfAddedCharacters++
              }
              addTokenToBuilder(builder, currentToken)
              charactersInLine += currentToken.size + 1
            }
            currentToken.clear()
          }
        } else {
          currentToken.add(chars[i])
          numberOfAddedCharacters++
        }
        if (i == oldCaretOffset) {
          newCaretOffset = oldCaretOffset - (i - numberOfAddedCharacters)
        }
        i++
      }
      if (currentToken.size != 0) {
        builder.append(" ")
        addTokenToBuilder(builder, currentToken)
        numberOfAddedCharacters++
      }
      builder.append(inputText.substring(trailingWhitespaceStart))
      return ReformatParagraphResult(builder.toString(), newCaretOffset)
    }

    private fun getFirstNonWhitespaceCharIndex(chars: CharArray): Int {
      var i = 0
      while (i < chars.size && Character.isWhitespace(chars[i])) {
        i++
      }
      return i
    }

    fun getTrailingWhitespaceStart(chars: CharArray): Int {
      var i = chars.size - 1
      while (i >= 0 && Character.isWhitespace(chars[i])) {
        i--
      }
      return i + 1
    }

    private fun addTokenToBuilder(builder: StringBuilder, currentToken: ArrayList<Char>) {
      for (j in currentToken.indices) {
        builder.append(currentToken[j])
      }
    }
  }
}