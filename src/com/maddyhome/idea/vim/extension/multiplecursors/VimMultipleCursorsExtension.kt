package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.CaretData
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.SearchHelper.findWordUnderCursor
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import java.lang.Integer.min

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
    putExtensionHandlerMapping(MappingMode.NVO, parseKeys(NEXT_WHOLE_OCCURRENCE), NextOccurrenceHandler(), false)
    putExtensionHandlerMapping(MappingMode.NVO, parseKeys(NEXT_OCCURRENCE), NextOccurrenceHandler(whole = false), false)
    putExtensionHandlerMapping(MappingMode.NO, parseKeys(ALL_WHOLE_OCCURRENCES), AllOccurrencesHandler(), false)
    putExtensionHandlerMapping(MappingMode.NO, parseKeys(ALL_OCCURRENCES), AllOccurrencesHandler(whole = false), false)
    putExtensionHandlerMapping(MappingMode.V, parseKeys(SKIP_OCCURRENCE), SkipOccurrenceHandler(), false)
    putExtensionHandlerMapping(MappingMode.V, parseKeys(REMOVE_OCCURRENCE), RemoveOccurrenceHandler(), false)

    putKeyMapping(MappingMode.NVO, parseKeys("<A-n>"), parseKeys(NEXT_WHOLE_OCCURRENCE), true)
    putKeyMapping(MappingMode.NVO, parseKeys("g<A-n>"), parseKeys(NEXT_OCCURRENCE), true)
    putKeyMapping(MappingMode.V, parseKeys("<A-x>"), parseKeys(SKIP_OCCURRENCE), true)
    putKeyMapping(MappingMode.V, parseKeys("<A-p>"), parseKeys(REMOVE_OCCURRENCE), true)
  }

  inner class NextOccurrenceHandler(val whole: Boolean = true) : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      val caretModel = editor.caretModel
      val commandState = CommandState.getInstance(editor)

      if (commandState.mode != CommandState.Mode.VISUAL) {
        if (caretModel.caretCount > 1) return

        val caret = caretModel.primaryCaret
        val range = findWordUnderCursor(editor, caret) ?: return
        if (range.startOffset > caret.offset) return

        caret.selectWordAtCaret(false)
        VimPlugin.getMotion().setVisualMode(editor, commandState.subMode)

        VimPlugin.getSearch().searchWord(editor, caret, 1, whole, 1)
        MotionGroup.moveCaret(editor, caret, range.endOffset - 1, true)
      }
      else {
        val newPositions = arrayListOf<VisualPosition>()
        val patterns = sortedSetOf<String>()
        for (caret in caretModel.allCarets) {
          val selectedText = caret.selectedText ?: return
          patterns += selectedText

          val lines = selectedText.count { it == '\n'}
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
          VimPlugin.getMotion().exitVisual(editor)
          newPositions.forEach { editor.caretModel.addCaret(it) ?: return }
          return
        }
        if (patterns.size > 1) return

        val primaryCaret = editor.caretModel.primaryCaret
        val nextOffset = VimPlugin.getSearch().searchNextFromOffset(editor, primaryCaret.offset + 1, 1)
        val pattern = patterns.first()
        if (nextOffset == -1 ||
            EditorHelper.getText(editor, nextOffset, nextOffset + pattern.length).indexOf(pattern) == -1) {

          if (caretModel.caretCount > 1) return

          val newNextOffset = VimPlugin.getSearch().search(editor, pattern, 1, Command.FLAG_SEARCH_FWD, false)

          val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(newNextOffset)) ?: return
          selectWord(caret, newNextOffset, editor, pattern)

          return
        }

        caretModel.allCarets.forEach {
          if (it.selectionStart == nextOffset) {
            VimPlugin.showMessage("No more matches")
            return
          }
        }

        val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(nextOffset)) ?: return
        selectWord(caret, nextOffset, editor, pattern)
      }
    }
  }

  inner class AllOccurrencesHandler(val whole: Boolean = true) : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
    }
  }

  inner class SkipOccurrenceHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
    }
  }

  inner class RemoveOccurrenceHandler : VimExtensionHandler {

    override fun execute(editor: Editor, context: DataContext) {}
  }

  private fun selectWord(caret: Caret, newNextOffset: Int, editor: Editor, pattern: String) {
    CaretData.setVisualStart(caret, newNextOffset)
    VimPlugin.getMotion().updateSelection(editor, caret, newNextOffset + pattern.length - 1)
    MotionGroup.moveCaret(editor, caret, newNextOffset + pattern.length - 1)
  }
}
