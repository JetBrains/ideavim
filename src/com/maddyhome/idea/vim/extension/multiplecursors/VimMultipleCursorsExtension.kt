package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.motion.visual.addNewSelection
import com.maddyhome.idea.vim.action.motion.visual.selectNextOccurrence
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys

private const val NEXT_OCCURRENCE = "<Plug>NextOccurrence"
private const val SKIP_OCCURRENCE = "<Plug>SkipOccurrence"
private const val REMOVE_OCCURRENCE = "<Plug>RemoveOccurrence"

class VimMultipleCursorsExtension : VimNonDisposableExtension() {
  override fun getName() = "multiple-cursors"

  override fun initOnce() {
    putExtensionHandlerMapping(MappingMode.NVO, parseKeys(NEXT_OCCURRENCE), NextOccurrenceHandler(), false)
    putExtensionHandlerMapping(MappingMode.V, parseKeys(SKIP_OCCURRENCE), SkipOccurrenceHandler(), false)
    putExtensionHandlerMapping(MappingMode.V, parseKeys(REMOVE_OCCURRENCE), RemoveOccurrenceHandler(), false)

    putKeyMapping(MappingMode.NVO, parseKeys("<A-n>"), parseKeys(NEXT_OCCURRENCE), true)
    putKeyMapping(MappingMode.V, parseKeys("<A-x>"), parseKeys(SKIP_OCCURRENCE), true)
    putKeyMapping(MappingMode.V, parseKeys("<A-p>"), parseKeys(REMOVE_OCCURRENCE), true)
  }

  private class NextOccurrenceHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      val offset = VimPlugin.getMotion().selectNextOccurrence(editor)
      if (offset != -1) MotionGroup.moveCaret(editor, editor.caretModel.primaryCaret, offset, true)
    }
  }

  private class SkipOccurrenceHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      val offset = VimPlugin.getMotion().skipCurrentOccurrence(editor)
      if (offset != -1) MotionGroup.moveCaret(editor, editor.caretModel.primaryCaret, offset, true)
    }

  }

  private class RemoveOccurrenceHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      editor.selectionModel.removeSelection()
      editor.caretModel.removeCaret(editor.caretModel.primaryCaret)
    }
  }
}

fun MotionGroup.skipCurrentOccurrence(editor: Editor): Int {
  val primaryCaret = editor.caretModel.primaryCaret
  val nextOffset = VimPlugin.getSearch().searchNext(editor, primaryCaret, 1)
  if (nextOffset == -1) return nextOffset

  primaryCaret.moveToOffset(nextOffset)
  return addNewSelection(editor, primaryCaret)
}