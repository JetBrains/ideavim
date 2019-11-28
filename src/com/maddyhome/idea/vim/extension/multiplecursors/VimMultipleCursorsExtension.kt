/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.SearchHelper.findWordUnderCursor
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.option.OptionsManager
import java.lang.Integer.min
import java.util.*
import kotlin.Comparator

private const val NEXT_WHOLE_OCCURRENCE = "<Plug>NextWholeOccurrence"
private const val NEXT_OCCURRENCE = "<Plug>NextOccurrence"
private const val SKIP_OCCURRENCE = "<Plug>SkipOccurrence"
private const val REMOVE_OCCURRENCE = "<Plug>RemoveOccurrence"
private const val ALL_WHOLE_OCCURRENCES = "<Plug>AllWholeOccurrences"
private const val ALL_OCCURRENCES = "<Plug>AllOccurrences"

/**
 * Port of vim-multiple-cursors.
 *
 * See https://github.com/terryma/vim-multiple-cursors
 * */
class VimMultipleCursorsExtension : VimNonDisposableExtension() {
  override fun getName() = "multiple-cursors"

  override fun initOnce() {
    putExtensionHandlerMapping(MappingMode.NXO, parseKeys(NEXT_WHOLE_OCCURRENCE), NextOccurrenceHandler(), false)
    putExtensionHandlerMapping(MappingMode.NXO, parseKeys(NEXT_OCCURRENCE), NextOccurrenceHandler(whole = false), false)
    putExtensionHandlerMapping(MappingMode.NXO, parseKeys(ALL_WHOLE_OCCURRENCES), AllOccurrencesHandler(), false)
    putExtensionHandlerMapping(MappingMode.NXO, parseKeys(ALL_OCCURRENCES), AllOccurrencesHandler(whole = false), false)
    putExtensionHandlerMapping(MappingMode.X, parseKeys(SKIP_OCCURRENCE), SkipOccurrenceHandler(), false)
    putExtensionHandlerMapping(MappingMode.X, parseKeys(REMOVE_OCCURRENCE), RemoveOccurrenceHandler(), false)

    putKeyMapping(MappingMode.NXO, parseKeys("<A-n>"), parseKeys(NEXT_WHOLE_OCCURRENCE), true)
    putKeyMapping(MappingMode.NXO, parseKeys("g<A-n>"), parseKeys(NEXT_OCCURRENCE), true)
    putKeyMapping(MappingMode.X, parseKeys("<A-x>"), parseKeys(SKIP_OCCURRENCE), true)
    putKeyMapping(MappingMode.X, parseKeys("<A-p>"), parseKeys(REMOVE_OCCURRENCE), true)
  }

  abstract class WriteActionHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      ApplicationManager.getApplication().runWriteAction {
        executeInWriteAction(editor, context)
      }
    }

    abstract fun executeInWriteAction(editor: Editor, context: DataContext)
  }

  inner class NextOccurrenceHandler(val whole: Boolean = true) : WriteActionHandler() {
    override fun executeInWriteAction(editor: Editor, context: DataContext) {
      val caretModel = editor.caretModel
      val patternComparator = if (OptionsManager.ignorecase.isSet) String.CASE_INSENSITIVE_ORDER else Comparator(String::compareTo);

      if (!editor.inVisualMode) {
        if (caretModel.caretCount > 1) return

        val caret = caretModel.primaryCaret
        val range = findWordUnderCursor(editor, caret) ?: return
        if (range.startOffset > caret.offset) return

        val nextOffset = findNextOccurrence(editor, caret, range, whole)
        if (nextOffset == caret.selectionStart) VimPlugin.showMessage("No more matches")
      } else {
        val newPositions = arrayListOf<VisualPosition>()
        val patterns = sortedSetOf<String>(patternComparator)
        for (caret in caretModel.allCarets) {
          val selectedText = caret.selectedText ?: return
          patterns += selectedText

          val lines = selectedText.count { it == '\n' }
          if (lines > 0) {
            val selectionStart = min(caret.selectionStart, caret.selectionEnd)
            val startPosition = editor.offsetToVisualPosition(selectionStart)
            for (line in startPosition.line + 1..startPosition.line + lines) {
              newPositions += VisualPosition(line, startPosition.column)
            }
            MotionGroup.moveCaret(editor, caret, selectionStart)
          }
        }
        if (newPositions.size > 0) {
          editor.exitVisualMode()
          newPositions.forEach { editor.caretModel.addCaret(it) ?: return }
          return
        }
        if (patterns.size > 1) return

        val primaryCaret = editor.caretModel.primaryCaret
        val nextOffset = VimPlugin.getSearch().searchNextFromOffset(editor, primaryCaret.offset + 1, 1)
        val pattern = patterns.first()
        if (nextOffset == -1 || patternComparator.compare(EditorHelper.getText(editor, nextOffset, nextOffset + pattern.length), pattern) != 0) {
          if (caretModel.caretCount > 1) return

          val newNextOffset = VimPlugin.getSearch().search(editor, pattern, 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)

          val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(newNextOffset)) ?: return
          selectWord(caret, pattern, newNextOffset)

          return
        }

        caretModel.allCarets.forEach {
          if (it.selectionStart == nextOffset) {
            VimPlugin.showMessage("No more matches")
            return
          }
        }

        val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(nextOffset)) ?: return
        selectWord(caret, pattern, nextOffset)
      }
    }
  }

  inner class AllOccurrencesHandler(val whole: Boolean = true) : WriteActionHandler() {
    override fun executeInWriteAction(editor: Editor, context: DataContext) {
      val caretModel = editor.caretModel
      if (caretModel.caretCount > 1) return

      val primaryCaret = caretModel.primaryCaret
      var nextOffset = if (editor.inVisualMode) {
        val selectedText = primaryCaret.selectedText ?: return
        val nextOffset = VimPlugin.getSearch().search(editor, selectedText, 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
        nextOffset
      } else {
        val range = findWordUnderCursor(editor, primaryCaret) ?: return
        if (range.startOffset > primaryCaret.offset) {
          return
        }
        findNextOccurrence(editor, primaryCaret, range, whole)
      }

      val firstOffset = primaryCaret.selectionStart
      val newPositions = arrayListOf(nextOffset, firstOffset)
      while (nextOffset != firstOffset) {
        nextOffset = VimPlugin.getSearch().searchNextFromOffset(editor, nextOffset + 1, 1)
        newPositions += nextOffset
      }

      val pattern = primaryCaret.selectedText ?: return
      newPositions.sorted().forEach {
        val caret = caretModel.addCaret(editor.offsetToVisualPosition(it)) ?: return
        selectWord(caret, pattern, it)
      }
    }
  }

  inner class SkipOccurrenceHandler : WriteActionHandler() {
    override fun executeInWriteAction(editor: Editor, context: DataContext) {
      val caret = editor.caretModel.primaryCaret
      val selectedText = caret.selectedText ?: return

      val nextOffset = tryFindNextOccurrence(editor, caret, selectedText)
      if (nextOffset == -1) return

      editor.caretModel.allCarets.forEach {
        if (it.selectionStart == nextOffset) {
          VimPlugin.showMessage("No more matches")
          return
        }
      }

      val newCaret = editor.caretModel.addCaret(editor.offsetToVisualPosition(nextOffset)) ?: return
      selectWord(newCaret, selectedText, nextOffset)
      editor.caretModel.removeCaret(caret)
    }
  }

  inner class RemoveOccurrenceHandler : WriteActionHandler() {
    override fun executeInWriteAction(editor: Editor, context: DataContext) {
      val caret = editor.caretModel.primaryCaret
      val selectedText = caret.selectedText ?: return

      if (tryFindNextOccurrence(editor, caret, selectedText) == -1) return

      if (!editor.caretModel.removeCaret(caret)) {
        editor.exitVisualMode()
      }
    }
  }

  private fun selectWord(caret: Caret, pattern: String, offset: Int) {
    caret.vimSetSelection(offset, offset + pattern.length - 1, true)
    if (caret == caret.editor.caretModel.primaryCaret) MotionGroup.scrollCaretIntoView(caret.editor)
  }

  private fun findNextOccurrence(editor: Editor, caret: Caret, range: TextRange, whole: Boolean): Int {
    VimPlugin.getVisualMotion().setVisualMode(editor)
    val wordRange = VimPlugin.getMotion().getWordRange(editor, caret, 1, false, false)
    caret.vimSetSelection(wordRange.startOffset, wordRange.endOffsetInclusive, true)

    val offset = VimPlugin.getSearch().searchWord(editor, caret, 1, whole, 1)
    MotionGroup.moveCaret(editor, caret, range.endOffset - 1)

    return offset
  }

  private fun tryFindNextOccurrence(editor: Editor, caret: Caret, pattern: String): Int {
    val nextOffset = VimPlugin.getSearch().searchNextFromOffset(editor, caret.offset + 1, 1)
    if (nextOffset == -1 || EditorHelper.getText(editor, nextOffset, nextOffset + pattern.length) != pattern) {
      return -1
    }

    return nextOffset
  }
}
