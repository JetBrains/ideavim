package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.CommandState.Mode
import com.maddyhome.idea.vim.command.CommandState.SubMode
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.CaretData
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys

private const val NEXT_WHOLE_OCCURRENCE = "<Plug>NextWholeOccurrence"
private const val NEXT_OCCURRENCE = "<Plug>NextOccurrence"
private const val SKIP_OCCURRENCE = "<Plug>SkipOccurrence"
private const val REMOVE_OCCURRENCE = "<Plug>RemoveOccurrence"
private const val ALL_WHOLE_OCCURRENCES = "<Plug>AllWholeOccurrences"
private const val ALL_OCCURRENCES = "<Plug>AllOccurrences"

class VimMultipleCursorsExtension : VimNonDisposableExtension() {
  private var nextOffset = -1
  private lateinit var firstRange: TextRange

  private val hasNext: Boolean get() = nextOffset != firstRange.startOffset

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
    override fun execute(editor: Editor, context: DataContext) =
      if (editor.caretModel.caretCount == 1 && CommandState.getInstance(editor).mode != Mode.VISUAL) {
        handleFirstSelection(editor, whole)
      }
      else {
        handleNextSelection(editor)
      }
  }

  inner class AllOccurrencesHandler(val whole: Boolean = true) : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      handleFirstSelection(editor, whole)
      while (hasNext) handleNextSelection(editor)
    }
  }

  inner class SkipOccurrenceHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      val caret = editor.caretModel.primaryCaret

      editor.selectionModel.removeSelection()
      MotionGroup.moveCaret(editor, caret, nextOffset)
      if (editor.caretModel.caretCount == 1) {
        val firstLength = firstRange.length
        firstRange = VimPlugin.getMotion().getWordRange(editor, caret, 1, false, false)
        if (firstRange.length != firstLength) {
          firstRange = TextRange(firstRange.startOffset, firstRange.startOffset + firstLength)
        }
      }

      val endOffset = nextOffset + firstRange.length
      selectRange(editor, caret, nextOffset, endOffset)
      MotionGroup.moveCaret(editor, caret, endOffset, true)

      nextOffset = VimPlugin.getSearch().searchNext(editor, caret, 1)
    }
  }

  inner class RemoveOccurrenceHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      nextOffset = CaretData.getVisualStart(editor.caretModel.primaryCaret)
      editor.selectionModel.removeSelection()
      if (!editor.caretModel.removeCaret(editor.caretModel.primaryCaret)) {
        if (CommandState.getInstance(editor).mode == Mode.VISUAL) {
          CommandState.getInstance(editor).popState()
        }
      }
    }
  }

  private fun selectRange(editor: Editor, caret: Caret, startOffset: Int, endOffset: Int) {
    CaretData.setVisualStart(caret, startOffset)
    VimPlugin.getMotion().updateSelection(editor, caret, endOffset)
    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
  }

  private fun handleFirstSelection(editor: Editor, whole: Boolean) {
    val caret = editor.caretModel.primaryCaret
    firstRange = VimPlugin.getMotion().getWordRange(editor, caret, 1, false, false)
    nextOffset = VimPlugin.getSearch().searchWord(editor, caret, 1, whole, 1)

    CommandState.getInstance(editor).pushState(Mode.VISUAL, SubMode.VISUAL_CHARACTER, MappingMode.VISUAL)
    selectRange(editor, caret, firstRange.startOffset, firstRange.endOffset)
    MotionGroup.moveCaret(editor, caret, firstRange.endOffset, true)
  }

  private fun handleNextSelection(editor: Editor) {
    if (!hasNext) {
      VimPlugin.showMessage("No more matches")
      return
    }

    val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(nextOffset), true)
        ?: throw IllegalStateException("Multiple carets are not supported")

    val endOffset = nextOffset + firstRange.length
    selectRange(editor, caret, nextOffset, endOffset)
    MotionGroup.moveCaret(editor, caret, endOffset, true)

    nextOffset = VimPlugin.getSearch().searchNext(editor, caret, 1)
  }
}

private val TextRange.length: Int
  get() {
    if (isMultiple) throw IllegalStateException("Multiple range found")
    return maxLength
  }