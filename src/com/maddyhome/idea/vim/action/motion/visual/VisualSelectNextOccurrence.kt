package com.maddyhome.idea.vim.action.motion.visual

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.CommandState.Mode
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.CaretOrder
import com.maddyhome.idea.vim.handler.MotionEditorActionHandler
import com.maddyhome.idea.vim.helper.CaretData
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.SearchHelper
import java.util.*
import javax.swing.KeyStroke

class VisualSelectNextOccurrence : VimCommandAction(Handler()) {
  class Handler : MotionEditorActionHandler() {
    override fun getOffset(editor: Editor, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
      return VimPlugin.getMotion().selectNextSearch(editor)
    }
  }

  override fun getMappingModes(): EnumSet<MappingMode> = MappingMode.NVO

  override fun getKeyStrokesSet(): Set<MutableList<KeyStroke>> = parseKeysSet("gn")

  override fun getType() = Command.Type.MOTION
}

fun MotionGroup.selectNextSearch(editor: Editor): Int {
  val lastCaret = EditorHelper.getOrderedCaretsList(editor,
                                                    CaretOrder.DECREASING_OFFSET)[0]

  val nextOffset = VimPlugin.getSearch().searchNext(editor, lastCaret, 1)
  if (nextOffset == -1) return nextOffset

  val state = CommandState.getInstance(editor)
  if (editor.caretModel.caretCount == 1 && state.mode != Mode.VISUAL) {
    lastCaret.moveToOffset(nextOffset)

    state.pushState(Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER, MappingMode.VISUAL)

    val range = SearchHelper.findWordUnderCursor(editor, lastCaret) ?: return -1
    val startOffset = range.startOffset
    val endOffset = range.endOffset - 1
    CaretData.setVisualStart(lastCaret, startOffset)
    updateSelection(editor, lastCaret, endOffset)

    return endOffset
  }

  val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(nextOffset), true) ?: return -1
  val range = SearchHelper.findWordUnderCursor(editor, caret) ?: return -1
  val startOffset = range.startOffset
  val endOffset = range.endOffset - 1
  CaretData.setVisualStart(caret, startOffset)
  updateSelection(editor, caret, endOffset)

  return endOffset
}

