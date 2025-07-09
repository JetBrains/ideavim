/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.util.NlsSafe
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.helper.userData
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import kotlin.math.max
import kotlin.math.min

@NlsSafe
private const val NEXT_WHOLE_OCCURRENCE = "<Plug>NextWholeOccurrence"

@NlsSafe
private const val NEXT_OCCURRENCE = "<Plug>NextOccurrence"

@NlsSafe
private const val SKIP_OCCURRENCE = "<Plug>SkipOccurrence"

@NlsSafe
private const val REMOVE_OCCURRENCE = "<Plug>RemoveOccurrence"

@NlsSafe
private const val ALL_WHOLE_OCCURRENCES = "<Plug>AllWholeOccurrences"

@NlsSafe
private const val ALL_OCCURRENCES = "<Plug>AllOccurrences"

private var Editor.vimMultipleCursorsWholeWord: Boolean? by userData()
private var Editor.vimMultipleCursorsLastSelection: TextRange? by userData()

/**
 * Port of vim-multiple-cursors.
 *
 * See https://github.com/terryma/vim-multiple-cursors
 * */
internal class VimMultipleCursorsExtension : VimExtension {

  override val name = "multiple-cursors"

  override suspend fun init() {
    putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys(NEXT_WHOLE_OCCURRENCE), mappingOwner, NextOccurrenceHandler(), false)
    putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys(NEXT_OCCURRENCE),
      mappingOwner,
      NextOccurrenceHandler(whole = false),
      false,
    )
    putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys(ALL_WHOLE_OCCURRENCES), mappingOwner, AllOccurrencesHandler(), false)
    putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys(ALL_OCCURRENCES),
      mappingOwner,
      AllOccurrencesHandler(whole = false),
      false,
    )
    putExtensionHandlerMapping(MappingMode.X, injector.parser.parseKeys(SKIP_OCCURRENCE), mappingOwner, SkipOccurrenceHandler(), false)
    putExtensionHandlerMapping(MappingMode.X, injector.parser.parseKeys(REMOVE_OCCURRENCE), mappingOwner, RemoveOccurrenceHandler(), false)

    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys("<A-n>"), mappingOwner, injector.parser.parseKeys(NEXT_WHOLE_OCCURRENCE), true)
    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys("g<A-n>"), mappingOwner, injector.parser.parseKeys(NEXT_OCCURRENCE), true)
    putKeyMappingIfMissing(MappingMode.X, injector.parser.parseKeys("<A-x>"), mappingOwner, injector.parser.parseKeys(SKIP_OCCURRENCE), true)
    putKeyMappingIfMissing(MappingMode.X, injector.parser.parseKeys("<A-p>"), mappingOwner, injector.parser.parseKeys(REMOVE_OCCURRENCE), true)
  }

  abstract class WriteActionHandler : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      ApplicationManager.getApplication().runWriteAction {
        executeInWriteAction(editor.ij, context.ij)
      }
    }

    abstract fun executeInWriteAction(editor: Editor, context: DataContext)
  }

  inner class NextOccurrenceHandler(val whole: Boolean = true) : WriteActionHandler() {
    override fun executeInWriteAction(editor: Editor, context: DataContext) {
      val caretModel = editor.caretModel

      // vim-multiple-cursors provides a completely custom implementation of multiple cursors. We can rely on IntelliJ's
      // implementation.
      // vim-multiple-cursors will call "new" to add a new cursor. In normal mode, it sets "whole" to true, in visual,
      // "whole" is false. The "whole" flag is saved to a script wide variable, the cursor is added and then the plugin
      // enters a custom loop, applying appropriate commands. In this loop, there is only a key shortcut for "next"
      // (<C-N>) and no support for "next non-word". The loop will check the script wide word boundary flag and call
      // "new" again.
      // We might want to consider updating the mappings to handle the difference between normal mode and visual mode

      if (!editor.inVisualMode) {
        // TODO: Handle multiple cursors in normal mode
        // E.g. start a multiple cursor session, clear selection and add a new cursor
        // TODO: New cursor should be based on text at the last visual selection marks
        // (Marks are not set until we come out of visual mode, so might need to use a work around)
        // TODO: Make sure we can handle manually added cursors
        if (caretModel.caretCount > 1) return

        val selection = selectWordUnderCaret(editor, caretModel.primaryCaret)

        // The handler is specific to whole/not-whole word, but the next occurrence is based on the initial call
        editor.vimMultipleCursorsWholeWord = whole
        editor.vimMultipleCursorsLastSelection = selection
      } else {
        // vim-multiple-cursors is case sensitive, so it's ok to use a case sensitive set here
        val patterns = sortedSetOf<String>()
        val newPositions = arrayListOf<VisualPosition>()

        // If multiple lines are selected, we want to convert the selection to multiple carets, positioned at the start
        // of each line
        for (caret in caretModel.allCarets) {
          val selectedText = caret.selectedText ?: return

          // Keep a track of the selected text, we'll check it later
          patterns.add(selectedText)

          val minOffset = min(caret.selectionEnd, caret.selectionStart)
          var maxOffset = max(caret.selectionEnd, caret.selectionStart)

          // As the last offset appears after the new line character, technically it's placed on the next line.
          if (selectedText.lastOrNull() == '\n') {
            maxOffset -= 1
          }
          val start = editor.document.getLineNumber(minOffset)
          val end = editor.document.getLineNumber(maxOffset)
          val lines = end - start
          if (lines > 0) {
            val selectionStart = min(caret.selectionStart, caret.selectionEnd)
            val startPosition = editor.offsetToVisualPosition(selectionStart)
            for (line in startPosition.line + 1..startPosition.line + lines) {
              newPositions.add(VisualPosition(line, startPosition.column))
            }
            caret.vim.moveToOffset(selectionStart)
          }
        }

        if (newPositions.size > 0) {
          editor.vim.exitVisualMode()
          newPositions.forEach { editor.caretModel.addCaret(it, true) ?: return@forEach }
          editor.updateCaretsVisualAttributes()
          return
        }

        // All the carets should be selecting the same text. If they're not, then it's likely they have been added
        // by some other means, so we shouldn't continue with the VIM behaviour
        if (patterns.size > 1) return

        // If we are adding the first new cursor, based on the current selection, we do a non-whole word match (ignoring
        // the value passed to the handler during mapping. We should fix the mappings for visual mode). If we're adding
        // a second or subsequent cursor, we should use the boundary matching parameter used to start the session.
        // But all we know right now is that we're in visual mode, and we have a selection. We cannot tell if the
        // selection has been added by the user (we're trying to add the first cursor) or it was added when we added the
        // first/previous cursor (we're about to add a second/subsequent cursor).
        // So, we keep track of the selection used to add the previous cursor. If it matches the current select, we know
        // we're about to add a second cursor (so use the saved word boundary flag). If it does not match, something's
        // changed, so we're adding a first cursor based on the current selection (set a new non-whole word flag)
        val currentSelection = TextRange(caretModel.primaryCaret.selectionStart, caretModel.primaryCaret.selectionEnd)
        var lastSelection = editor.vimMultipleCursorsLastSelection
        val wholeWord = if (lastSelection != null && lastSelection.startOffset == currentSelection.startOffset &&
          lastSelection.endOffset == currentSelection.endOffset
        ) {
          editor.vimMultipleCursorsWholeWord ?: false
        } else {
          false
        }
        editor.vimMultipleCursorsWholeWord = wholeWord
        lastSelection = currentSelection

        // Always work on the text in the last visual selection range, so we work with any changed text, even if it's no
        // longer selected
        val pattern = editor.vim.getText(lastSelection)

        val primaryCaret = editor.caretModel.primaryCaret
        val nextOffset = findNextOccurrence(editor, primaryCaret.offset, pattern, wholeWord)
        if (nextOffset != -1) {
          caretModel.allCarets.forEach {
            if (it.selectionStart == nextOffset) {
              VimPlugin.showMessage(MessageHelper.message("message.no.more.matches"))
              return
            }
          }

          val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(nextOffset), true) ?: return
          editor.updateCaretsVisualAttributes()
          editor.vimMultipleCursorsLastSelection = selectText(caret, pattern, nextOffset)
        } else {
          VimPlugin.showMessage(MessageHelper.message("message.no.more.matches"))
        }
      }
    }
  }

  inner class AllOccurrencesHandler(val whole: Boolean = true) : WriteActionHandler() {
    override fun executeInWriteAction(editor: Editor, context: DataContext) {
      val caretModel = editor.caretModel
      if (caretModel.caretCount > 1) return

      val primaryCaret = caretModel.primaryCaret
      val text = if (editor.inVisualMode) {
        primaryCaret.selectedText ?: return
      } else {
        val range = injector.searchHelper.findWordNearestCursor(editor.vim, primaryCaret.vim) ?: return
        if (range.startOffset > primaryCaret.offset) return
        IjVimEditor(editor).getText(range)
      }

      if (!editor.inVisualMode) {
        enterVisualMode(editor.vim)
      }

      // Note that ignoreCase is not overridden by the `\C` in the pattern
      val pattern = makePattern(text, whole)
      val matches = injector.searchHelper.findAll(IjVimEditor(editor), pattern, 0, -1, false)
      for (match in matches) {
        if (match.contains(primaryCaret.offset)) {
          primaryCaret.vim.moveToOffset(match.startOffset)
          selectText(primaryCaret, text, match.startOffset)
        } else {
          val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(match.startOffset), true) ?: return
          selectText(caret, text, match.startOffset)
        }
      }
      editor.updateCaretsVisualAttributes()
    }
  }

  inner class SkipOccurrenceHandler : WriteActionHandler() {
    override fun executeInWriteAction(editor: Editor, context: DataContext) {
      val primaryCaret = editor.caretModel.primaryCaret
      val selectedText = primaryCaret.selectedText ?: return

      val nextOffset =
        findNextOccurrence(editor, primaryCaret.offset, selectedText, editor.vimMultipleCursorsWholeWord ?: false)
      if (nextOffset != -1) {
        editor.caretModel.allCarets.forEach {
          if (it.selectionStart == nextOffset) {
            VimPlugin.showMessage(MessageHelper.message("message.no.more.matches"))
            return
          }
        }

        primaryCaret.moveToVisualPosition(editor.offsetToVisualPosition(nextOffset))
        selectText(primaryCaret, selectedText, nextOffset)
      }
    }
  }

  inner class RemoveOccurrenceHandler : WriteActionHandler() {
    override fun executeInWriteAction(editor: Editor, context: DataContext) {
      val caret = editor.caretModel.primaryCaret
      if (caret.selectedText == null) return
      if (!editor.caretModel.removeCaret(caret)) {
        editor.vim.exitVisualMode()
      }
      injector.scroll.scrollCaretIntoView(editor.vim)
    }
  }

  private fun selectText(caret: Caret, text: String, offset: Int): TextRange? {
    if (text.isEmpty()) return null
    caret.vim.vimSetSelection(offset, offset + text.length - 1, true)
    injector.scroll.scrollCaretIntoView(caret.editor.vim)
    return TextRange(caret.selectionStart, caret.selectionEnd)
  }

  private fun selectWordUnderCaret(editor: Editor, caret: Caret): TextRange? {
    // TODO: I think vim-multiple-cursors uses a text object rather than the star operator
    val range = injector.searchHelper.findWordNearestCursor(editor.vim, caret.vim) ?: return null
    if (range.startOffset > caret.offset) return null

    enterVisualMode(editor.vim)

    // Select the word under the caret, moving the caret to the end of the selection
    caret.vim.vimSetSelection(range.startOffset, range.endOffsetInclusive, true)
    return TextRange(caret.selectionStart, caret.selectionEnd)
  }

  private fun enterVisualMode(editor: VimEditor) {
    // We need to reset the key handler to make sure we pick up the fact that we're in visual mode
    VimPlugin.getVisualMotion().enterVisualMode(editor, SelectionType.CHARACTER_WISE)
    KeyHandler.getInstance().reset(editor)
  }

  private fun findNextOccurrence(editor: Editor, startOffset: Int, text: String, whole: Boolean): Int {
    val searchOptions = enumSetOf(SearchOptions.WHOLE_FILE)
    if (injector.options(editor.vim).wrapscan) {
      searchOptions.add(SearchOptions.WRAP)
    }

    return injector.searchHelper.findPattern(IjVimEditor(editor), makePattern(text, whole), startOffset, 1, searchOptions)?.startOffset ?: -1
  }

  private fun makePattern(text: String, whole: Boolean): String {
    // Pattern is "very nomagic" (ignore regex chars) and "force case sensitive". This is vim-multiple-cursors behaviour
    return "\\V\\C" + if (whole) "\\<$text\\>" else text
  }
}
