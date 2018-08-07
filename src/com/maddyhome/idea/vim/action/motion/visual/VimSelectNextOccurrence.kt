package com.maddyhome.idea.vim.action.motion.visual

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.CommandState.Mode
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.MotionEditorActionHandler
import com.maddyhome.idea.vim.helper.CaretData
import com.maddyhome.idea.vim.helper.SearchHelper
import java.util.*
import javax.swing.KeyStroke

class VimSelectNextOccurrence : VimCommandAction(Handler()) {
  private class Handler : MotionEditorActionHandler() {
    override fun getOffset(editor: Editor, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
      return VimPlugin.getMotion().selectNextOccurrence(editor)
    }
  }

  override fun getMappingModes(): EnumSet<MappingMode> = MappingMode.NVO

  override fun getKeyStrokesSet(): Set<List<KeyStroke>> = parseKeysSet("gn")

  override fun getType() = Command.Type.MOTION
}

fun MotionGroup.selectNextOccurrence(editor: Editor): Int {
  val caretModel = editor.caretModel
  val primaryCaret = caretModel.primaryCaret
  val nextOffset = VimPlugin.getSearch().searchNext(editor, primaryCaret, 1)
  if (nextOffset == -1) return nextOffset

  val state = CommandState.getInstance(editor)
  if (caretModel.caretCount == 1 && state.mode != Mode.VISUAL) {
    primaryCaret.moveToOffset(nextOffset)
    state.pushState(Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER, MappingMode.VISUAL)
    return addNewSelection(editor)
  }

  caretModel.addCaret(editor.offsetToVisualPosition(nextOffset), true) ?: return -1
  return addNewSelection(editor)
}

internal fun MotionGroup.addNewSelection(editor: Editor, caret: Caret = editor.caretModel.primaryCaret): Int {
  val range = SearchHelper.findWordUnderCursor(editor, caret) ?: return -1
  val startOffset = range.startOffset
  val endOffset = range.endOffset - 1
  CaretData.setVisualStart(caret, startOffset)
  updateSelection(editor, caret, endOffset)
  editor.scrollingModel.scrollToCaret(ScrollType.CENTER)

  return endOffset
}
