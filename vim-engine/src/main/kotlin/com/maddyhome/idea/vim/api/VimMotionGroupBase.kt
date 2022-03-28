package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.isEndAllowedIgnoringOnemore
import com.maddyhome.idea.vim.helper.mode

abstract class VimMotionGroupBase : VimMotionGroup {
  override fun getVerticalMotionOffset(editor: VimEditor, caret: VimCaret, count: Int): Int {
    val pos = caret.getVisualPosition()
    if ((pos.line == 0 && count < 0) || (pos.line >= injector.engineEditorHelper.getVisualLineCount(editor) - 1 && count > 0)) {
      return -1
    } else {
      var col = caret.vimLastColumn
      val line = injector.engineEditorHelper.normalizeVisualLine(editor, pos.line + count)

      if (col == LAST_COLUMN) {
        col = injector.engineEditorHelper.normalizeVisualColumn(
          editor, line, col,
          editor.mode.isEndAllowedIgnoringOnemore
        )
      } else {
        if (line < 0) {
          // https://web.ea.pages.jetbrains.team/#/issue/266279
          // There is a weird exception for line < 0, but I don't understand how this may happen
          throw RuntimeException("Line is " + line + " , pos.line=" + pos.line + ", count=" + count)
        }
        val newInlineElements = injector.engineEditorHelper
          .amountOfInlaysBeforeVisualPosition(editor, VimVisualPosition(line, col, false))

        col = injector.engineEditorHelper
          .normalizeVisualColumn(editor, line, col, (editor).isEndAllowed)
        col += newInlineElements
      }

      val newPos = VimVisualPosition(line, col, false)
      return editor.visualPositionToOffset(newPos).point
    }
  }

  companion object {
    const val LAST_COLUMN = 9999
  }
}